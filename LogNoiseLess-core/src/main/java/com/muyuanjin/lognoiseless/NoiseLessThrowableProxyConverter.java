package com.muyuanjin.lognoiseless;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.muyuanjin.lognoiseless.internal.ExtensibleExtendedWhitespaceThrowableProxyConverter;
import com.muyuanjin.lognoiseless.internal.NoiseLessConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 噪音抑制异常转换器，可根据需要抑制某些堆栈行的打印
 *
 * @see ThrowableProxyConverter
 */
@Slf4j
public class NoiseLessThrowableProxyConverter extends ExtensibleExtendedWhitespaceThrowableProxyConverter {
    private static final AtomicBoolean FAILED_PREDICATE = new AtomicBoolean(false);

    @Override
    public void start() {
        NoiseLessConfig.INSTANCE.getIfSpringEnvReady();
        super.start();
    }

    @Override
    protected boolean isShouldEnableIgnore(IThrowableProxy tp) {
        if (FAILED_PREDICATE.get()) {
            return super.isShouldEnableIgnore(tp);
        }
        NoiseLessConfig config = NoiseLessConfig.INSTANCE.getIfSpringEnvReady();
        StackLineSkipPredicate predicate;
        if (config != null && (predicate = config.getPredicate()) != null) {
            try {
                return predicate.isShouldEnableSkip(tp);
            } catch (Exception e) {
                if (FAILED_PREDICATE.compareAndSet(false, true)) {
                    if (log.isDebugEnabled()) {
                        System.err.println("堆栈行跳过谓词调用失败,已禁用,将使用原始配置,error: \n" + e);
                        e.printStackTrace();
                    } else {
                        addWarn("堆栈行跳过谓词调用失败,已禁用,将使用原始配置");
                        throw e;
                    }
                }
                return super.isShouldEnableIgnore(tp);
            }
        }
        return super.isShouldEnableIgnore(tp);
    }

    @Override
    protected boolean isIgnoredStackTraceLine(String line) {
        if (FAILED_PREDICATE.get()) {
            return super.isIgnoredStackTraceLine(line);
        }
        NoiseLessConfig config = NoiseLessConfig.INSTANCE.getIfSpringEnvReady();
        StackLineSkipPredicate predicate;
        if (config != null && (predicate = config.getPredicate()) != null) {
            try {
                return predicate.isShouldSkipLine(line);
            } catch (Exception e) {
                if (FAILED_PREDICATE.compareAndSet(false, true)) {
                    if (log.isDebugEnabled()) {
                        System.err.println("堆栈行跳过谓词调用失败,已禁用,将使用原始配置,error: \n" + e);
                        e.printStackTrace();
                    } else {
                        addWarn("堆栈行跳过谓词调用失败,已禁用,将使用原始配置");
                        throw e;
                    }
                }
                return super.isIgnoredStackTraceLine(line);
            }
        }
        return super.isIgnoredStackTraceLine(line);
    }
}