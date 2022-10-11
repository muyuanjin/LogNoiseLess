package com.muyuanjin.lognoiseless;

import ch.qos.logback.classic.spi.IThrowableProxy;
import com.muyuanjin.lognoiseless.internal.InternalStackLinePredicate;

/**
 * 堆栈行跳过谓词，用于判断是否应该跳过改行的堆栈打印
 */
@FunctionalInterface
public interface StackLineSkipPredicate extends InternalStackLinePredicate {
    /**
     * 本异常是否应该启用堆栈打印跳过
     */
    default boolean isShouldEnableSkip(IThrowableProxy throwableProxy) {
        return true;
    }

    /**
     * 该行是否应该跳过
     */
    @Override
    boolean isShouldSkipLine(String line);
}