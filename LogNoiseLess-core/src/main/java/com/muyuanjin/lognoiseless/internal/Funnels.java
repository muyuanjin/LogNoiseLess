package com.muyuanjin.lognoiseless.internal;

import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

@SuppressWarnings({"unused", "UnstableApiUsage"})
enum Funnels implements Funnel<StackTraceElementProxy[]> {
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