package com.muyuanjin.lognoiseless.internal;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mgunlogson.cuckoofilter4j.CuckooFilter;
import com.github.mgunlogson.cuckoofilter4j.Utils;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.muyuanjin.lognoiseless.StackLineSkipPredicate;
import com.muyuanjin.lognoiseless.util.LazyReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 并不严格的周期内重复堆栈压缩过滤器，由于受到布谷过滤器的最大计数上限的限制，如果周期内最大数量超过计数上限，则会分隔至多个周期内
 *
 * @author muyuanjin
 */
public class CuckooThrowableDuplicateFilter implements StackLineSkipPredicate {
    //因为日志文件会输出到控制台和文件内，会多次调用 isShouldEnableSkip 方法 ，缓存同一对象的判断结果
    private static final LazyReference<Cache<IThrowableProxy, Boolean>> RESULT_CACHE = new LazyReference<>(() -> Caffeine.newBuilder().weakKeys().build());
    private final AutoRebuildCuckooFilter<StackTraceElementProxy[]> autoRebuildCuckooFilter;
    private final StackLineSkipPredicate stackLineSkipPredicate;

    /**
     * @param maxNumOfAllowedToPrintFullStackPerCycle 允许一个周期内打印全栈的最大数量
     * @param cycleDuration                           周期长度，即重置计数器的间隔
     * @param stackLineSkipPredicate                  周期内超过最大数量后的判断谓词
     */
    public CuckooThrowableDuplicateFilter(int maxNumOfAllowedToPrintFullStackPerCycle, Duration cycleDuration, StackLineSkipPredicate stackLineSkipPredicate) {
        Assert.isTrue(maxNumOfAllowedToPrintFullStackPerCycle > 0 && maxNumOfAllowedToPrintFullStackPerCycle < cycleDuration.toMillis(),
                "maxNumOfAllowedToPrintFullStackPerCycle can not less than 0 or bigger than cycleDuration millis");
        Assert.notNull(cycleDuration, "cycleDuration can not be null");
        Assert.notNull(stackLineSkipPredicate, "stackLineSkipPredicate can not be null");
        this.autoRebuildCuckooFilter = new AutoRebuildCuckooFilter<>(maxNumOfAllowedToPrintFullStackPerCycle, cycleDuration, () ->
                new CuckooFilter.Builder<>(Funnels.DEFAULT, 3000 * Math.min(Math.max(1, Math.abs(cycleDuration.toHours())), 24))
                        .withFalsePositiveRate(0.01).withHashAlgorithm(Utils.Algorithm.Murmur3_32).build());
        this.stackLineSkipPredicate = stackLineSkipPredicate;
    }

    @Override
    public boolean isShouldEnableSkip(IThrowableProxy throwableProxy) {
        //通过堆栈前3行（抛出异常的位置）进行判断是否未记录过，如果 mightContain false 则直接判断为打印完整堆栈（return false）
        //如果 mightContain true ，则由于 抛出位置≠完整堆栈 & 布谷鸟过滤器的误判，再进行一次完整堆栈的布谷鸟过滤
        return Boolean.TRUE.equals(RESULT_CACHE.get().get(throwableProxy, k -> {
            //通过堆栈前3行（抛出异常的位置＋调用）进行判断是否未记录过，如果 mightContain false 则直接判断为打印完整堆栈（return false）
            StackTraceElementProxy[] traceElementProxies = k.getStackTraceElementProxyArray();
            StackTraceElementProxy[] stackTraces = Arrays.copyOf(traceElementProxies, 3);
            if (!autoRebuildCuckooFilter.mightContain(stackTraces)) {
                autoRebuildCuckooFilter.put(stackTraces);
                return false;
            }
            //如果 mightContain true ，则由于 抛出位置≠完整堆栈 & 布谷鸟过滤器的误判，再进行一次完整堆栈的布谷鸟过滤
            return autoRebuildCuckooFilter.isFull(traceElementProxies);
        }));
    }

    @Override
    public boolean isShouldSkipLine(String line) {
        return stackLineSkipPredicate.isShouldSkipLine(line);
    }

    @SuppressWarnings({"unused", "UnstableApiUsage"})
    private enum Funnels implements Funnel<StackTraceElementProxy[]> {
        DEFAULT {
            @Override
            public void funnel(StackTraceElementProxy @NotNull [] from, @NotNull PrimitiveSink into) {
                if (from.length == 0) {
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (StackTraceElementProxy stackTraceElementProxy : from) {
                    builder.append(stackTraceElementProxy.getSTEAsString());
                }
                into.putString(builder.toString(), StandardCharsets.UTF_8);
            }
        }
    }

    private static class AutoRebuildCuckooFilter<T> {
        private static final int CUCKOO_MAX_COUNT = 7;
        /**
         * 一个周期内最大重建数
         */
        private final int maxRebuildingNum;
        /**
         * 周期内最后一次重建的最大计数
         */
        private final int lastBuildMaxCount;
        /**
         * 重建间隔
         */
        private final long rebuildingInterval;
        /**
         * 当前是周期内的第几次重建
         */
        private final AtomicInteger currentRebuildingNum = new AtomicInteger(1);
        /**
         * 上次重建的时间
         */
        private final AtomicLong lastRebuildingTime = new AtomicLong(System.currentTimeMillis());

        private final Supplier<CuckooFilter<T>> supplier;
        private final AtomicReference<CuckooFilter<T>> reference = new AtomicReference<>();

        public AutoRebuildCuckooFilter(int tokens, Duration duration, Supplier<CuckooFilter<T>> supplier) {
            this.supplier = supplier;
            this.reference.set(supplier.get());
            this.lastBuildMaxCount = tokens % CUCKOO_MAX_COUNT;
            if (this.lastBuildMaxCount != 0) {
                this.maxRebuildingNum = tokens / CUCKOO_MAX_COUNT + 1;
            } else {
                this.maxRebuildingNum = tokens / CUCKOO_MAX_COUNT;
            }
            this.rebuildingInterval = duration.toMillis() / this.maxRebuildingNum;
        }

        public boolean put(T item) {
            CuckooFilter<T> cuckooFilter = rebuildIfNecessary();
            return cuckooFilter.put(item);
        }

        public boolean mightContain(T item) {
            CuckooFilter<T> cuckooFilter = rebuildIfNecessary();
            return cuckooFilter.mightContain(item);
        }

        public boolean isFull(T item) {
            CuckooFilter<T> cuckooFilter = rebuildIfNecessary();
            int times = CUCKOO_MAX_COUNT;
            if (currentRebuildingNum.get() == maxRebuildingNum) {
                times = lastBuildMaxCount;
            }
            return !cuckooFilter.put(item) || cuckooFilter.approximateCount(item) >= times;
        }

        private CuckooFilter<T> rebuildIfNecessary() {
            long last = lastRebuildingTime.get();
            long now = System.currentTimeMillis();
            if (now - last < rebuildingInterval) {
                return reference.get();
            }
            if (lastRebuildingTime.compareAndSet(last, now)) {
                currentRebuildingNum.incrementAndGet();
                this.reference.set(supplier.get());
                currentRebuildingNum.compareAndSet((maxRebuildingNum + 1), 1);
            }
            return reference.get();
        }
    }

}
