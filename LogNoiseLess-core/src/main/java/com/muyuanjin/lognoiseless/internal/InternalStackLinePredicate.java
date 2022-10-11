package com.muyuanjin.lognoiseless.internal;

/**
 * @author muyuanjin
 */
@FunctionalInterface
public interface InternalStackLinePredicate {
    boolean isShouldSkipLine(String line);
}