package com.muyuanjin.lognoiseless.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import javax.validation.constraints.NotEmpty;
import java.security.AccessController;
import java.security.PrivilegedAction;

@Slf4j
@UtilityClass
public class ConfigUtil {
    @Nullable
    public static String getProperty(@NotNull @NotEmpty String propertyKey) {
        return getProperty(propertyKey, null);
    }

    /**
     * 获取配置属性，优先从spring中获取，然后从系统中获取，然后是缺省值，然后是读取缺省值所在的文件
     *
     * @param propertyKey                 属性键
     * @param defaultValue                缺省值
     * @return 值
     */
    public static String getProperty(@NotNull @NotEmpty String propertyKey, @Nullable String defaultValue) {
        Assert.hasText(propertyKey, "propertyKey can not be null or empty");
        String value = SpringEnvUtil.getProperty(propertyKey);
        if (value != null) {
            return value;
        }
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(propertyKey);
            } else {
                value = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(propertyKey));
            }
        } catch (SecurityException e) {
            log.warn("Unable to retrieve a system property '{}'; default values will be used.", propertyKey, e);
        }
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。 {@code def} 如果没有这样的属性或者不允许访问指定的属性。
     */
    public static boolean getBoolean(String key, boolean def) {
        String value = getProperty(key);
        if (value == null) {
            return def;
        }
        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return def;
        }
        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }
        log.warn(
                "Unable to parse the boolean system property '{}':{} - using the default value: {}",
                key, value, def
        );
        return def;
    }

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。 {@code def} 如果没有这样的属性或者不允许访问指定的属性。
     */
    public static int getInt(String key, int def) {
        String value = getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim();
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            // Ignore
        }
        log.warn("Unable to parse the integer system property '{}':{} - using the default value: {}", key, value, def);
        return def;
    }

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。 {@code def} 如果没有这样的属性或者不允许访问指定的属性。
     */
    public static long getLong(String key, long def) {
        String value = getProperty(key);
        if (value == null) {
            return def;
        }
        value = value.trim();
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            // Ignore
        }
        log.warn("Unable to parse the long integer system property '{}':{} - using the default value: {}", key, value, def);
        return def;
    }
}