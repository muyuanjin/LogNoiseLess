package com.muyuanjin.lognoiseless.internal;

import com.muyuanjin.lognoiseless.StackLineSkipPredicate;
import com.muyuanjin.lognoiseless.util.ConfigUtil;
import com.muyuanjin.lognoiseless.util.InstanceUtil;
import com.muyuanjin.lognoiseless.util.LazyReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;

/**
 * 堆栈跳过配置
 */
@Slf4j
@Data
public class NoiseLessConfig {
    private static final String PROPERTY_SKIP_STACK_TRACE_LINES = "logback.stackTrace.skipLine";
    private static final String PROPERTY_SKIP_MODE = "logback.stackTrace.skipLineMode";
    private static final String PROPERTY_MAX_NUM_PER_CYCLE = "logback.stackTrace.maxNumPerCycle";
    private static final String PROPERTY_CYCLE_DURATION = "logback.stackTrace.cycleDuration";
    public static final LazyReference<NoiseLessConfig> INSTANCE = new LazyReference<>(() -> {
        String property = ConfigUtil.getProperty(PROPERTY_SKIP_STACK_TRACE_LINES);
        if (!StringUtils.hasText(property)) {
            return null;
        }
        SkipLineMode mode;
        try {
            mode = SkipLineMode.valueOf(ConfigUtil.getProperty(PROPERTY_SKIP_MODE, SkipLineMode.BLACKLIST.name()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
            System.err.println("SkipLineMode:" + ConfigUtil.getProperty(PROPERTY_SKIP_MODE) + " 设置失败 ,请检查该属性配置");
            return null;
        }
        StackLineSkipPredicate skipPredicate;
        try {
            skipPredicate = mode.getStackLinePredicate(property);
            //这里必须catch Throwable,有可能抛出 IllegalAccessError
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
            System.err.println("[" + PROPERTY_SKIP_STACK_TRACE_LINES + "] 属性初始化失败 property:" + property + ", skipLineMode:" + mode + ", error:" + e.getMessage());
            return null;
        }
        int maxNumPerCycle = ConfigUtil.getInt(PROPERTY_MAX_NUM_PER_CYCLE, 0);
        Duration duration = null;
        if (maxNumPerCycle > 0) {
            String cycleDurationStr = ConfigUtil.getProperty(PROPERTY_CYCLE_DURATION);
            ConversionService conversionService = InstanceUtil.getInstance(ApplicationConversionService.class);
            if (cycleDurationStr != null && conversionService != null) {
                duration = conversionService.convert(cycleDurationStr, Duration.class);
            }
        }
        if (maxNumPerCycle > 0 && duration != null) {
            skipPredicate = maxNumPerCycle == 1 ? new BloomThrowableDuplicateFilter(duration, skipPredicate) : new CuckooThrowableDuplicateFilter(maxNumPerCycle, duration, skipPredicate);
        }
        return new NoiseLessConfig(mode, skipPredicate, maxNumPerCycle, duration);
    });
    /**
     * 日志堆栈打印跳过行的模式
     */
    private final SkipLineMode skipLineMode;
    /**
     * 堆栈行跳过谓词
     */
    private final StackLineSkipPredicate predicate;
    /**
     * 每个周期允许打印全栈的最大数量
     */
    private final long maxNumOfAllowedToPrintFullStackPerCycle;
    /**
     * 周期时长
     */
    private final Duration cycleDuration;
}
