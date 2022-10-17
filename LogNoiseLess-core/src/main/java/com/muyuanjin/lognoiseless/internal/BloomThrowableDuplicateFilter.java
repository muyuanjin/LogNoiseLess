package com.muyuanjin.lognoiseless.internal;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.muyuanjin.lognoiseless.StackLineSkipPredicate;
import com.muyuanjin.lognoiseless.util.LazyReference;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class BloomThrowableDuplicateFilter implements StackLineSkipPredicate {
    //因为日志文件会输出到控制台和文件内，会多次调用 isShouldEnableSkip 方法 ，缓存同一对象的判断结果
    private static final LazyReference<Cache<IThrowableProxy, Boolean>> RESULT_CACHE = new LazyReference<>(() -> Caffeine.newBuilder().weakKeys().build());
    private final AutoRebuildBloomFilter<StackTraceElementProxy[]> autoRebuildBloomFilter;
    private final StackLineSkipPredicate stackLineSkipPredicate;

    /**
     * @param cycleDuration          周期长度，即重置计数器的间隔
     * @param stackLineSkipPredicate 周期内超过最大数量后的判断谓词
     */
    public BloomThrowableDuplicateFilter(Duration cycleDuration, StackLineSkipPredicate stackLineSkipPredicate) {
        Assert.notNull(cycleDuration, "cycleDuration can not be null");
        Assert.notNull(stackLineSkipPredicate, "stackLineSkipPredicate can not be null");
        this.autoRebuildBloomFilter = new AutoRebuildBloomFilter<>(cycleDuration, () ->
                BloomFilter.create(Funnels.DEFAULT, 3000 * Math.min(Math.max(1, Math.abs(cycleDuration.toHours())), 24)));
        this.stackLineSkipPredicate = stackLineSkipPredicate;
    }

    @Override
    public boolean isShouldEnableSkip(IThrowableProxy throwableProxy) {
        //如果 mightContain false 则直接判断为打印完整堆栈（return false）
        return Boolean.TRUE.equals(RESULT_CACHE.get().get(throwableProxy, k -> autoRebuildBloomFilter.isFull(k.getStackTraceElementProxyArray())));
    }

    @Override
    public boolean isShouldSkipLine(String line) {
        return stackLineSkipPredicate.isShouldSkipLine(line);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static class AutoRebuildBloomFilter<T> {
        /**
         * 重建间隔
         */
        private final long rebuildingInterval;
        /**
         * 上次重建的时间
         */
        private final AtomicLong lastRebuildingTime = new AtomicLong(System.currentTimeMillis());

        private final Supplier<BloomFilter<T>> supplier;
        private final AtomicReference<BloomFilter<T>> reference = new AtomicReference<>();

        public AutoRebuildBloomFilter(Duration duration, Supplier<BloomFilter<T>> supplier) {
            this.supplier = supplier;
            this.reference.set(supplier.get());
            this.rebuildingInterval = duration.toMillis();
        }

        public boolean isFull(T item) {
            BloomFilter<T> bloomFilter = rebuildIfNecessary();
            return !bloomFilter.put(item);
        }

        private BloomFilter<T> rebuildIfNecessary() {
            long last = lastRebuildingTime.get();
            long now = System.currentTimeMillis();
            if (now - last < rebuildingInterval) {
                return reference.get();
            }
            if (lastRebuildingTime.compareAndSet(last, now)) {
                this.reference.set(supplier.get());
            }
            return reference.get();
        }
    }
}