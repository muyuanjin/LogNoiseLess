package com.muyuanjin.lognoiseless.internal;

import com.muyuanjin.lognoiseless.StackLineSkipPredicate;
import com.muyuanjin.lognoiseless.util.InstanceUtil;
import lombok.SneakyThrows;
import org.codehaus.janino.ExpressionEvaluator;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志堆栈打印跳过行的模式
 */
public enum SkipLineMode {
    /**
     * 白名单默认
     */
    WHITELIST {
        @Override
        public StackLineSkipPredicate getStackLinePredicate(String property) {
            return new StackLineSkipPredicate() {
                private final String[] strings = StringUtils.commaDelimitedListToStringArray(property);
                private final List<String> collect = Arrays.stream(strings).distinct().filter(StringUtils::hasText).collect(Collectors.toList());

                @Override
                public boolean isShouldSkipLine(String line) {
                    for (String s : collect) {
                        if (line.startsWith("at " + s)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
    },
    /**
     * 黑名单模式
     */
    BLACKLIST {
        @Override
        public StackLineSkipPredicate getStackLinePredicate(String property) {
            return new StackLineSkipPredicate() {
                private final String[] strings = StringUtils.commaDelimitedListToStringArray(property);
                private final List<String> collect = Arrays.stream(strings).distinct().filter(StringUtils::hasText).collect(Collectors.toList());

                @Override
                public boolean isShouldSkipLine(String line) {
                    for (String s : collect) {
                        if (line.startsWith("at " + s)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    },

    /**
     * 使用{@link StackLineSkipPredicate}实现类，优先从SpringBean中获取
     */
    PREDICATE_CLASS {
        @Override
        @SneakyThrows
        @SuppressWarnings("unchecked")
        public StackLineSkipPredicate getStackLinePredicate(String property) {
            return InstanceUtil.getInstance((Class<StackLineSkipPredicate>) ClassUtils.forName(property, null));
        }
    },
    /**
     * 使用 janino 表达式， line 作为当前堆栈行的局部变量，如 "!line.contains(\"abc\");"
     */
    JANINO_EXPRESSION {
        @Override
        @SneakyThrows
        public StackLineSkipPredicate getStackLinePredicate(String property) throws IllegalAccessError {
            return new ExpressionEvaluator().createFastEvaluator(
                    property, // 要评估的表达式
                    InternalStackLinePredicate.class, // 描述表达式签名的接口
                    "line")::isShouldSkipLine;
        }
    };

    public abstract StackLineSkipPredicate getStackLinePredicate(String property);
}
