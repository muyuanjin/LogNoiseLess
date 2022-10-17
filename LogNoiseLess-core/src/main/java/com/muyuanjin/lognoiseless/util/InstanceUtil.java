package com.muyuanjin.lognoiseless.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class InstanceUtil {
    /**
     * 使用ClassValue 防止强引用Class 导致类无法卸载（实际上是把用户对象存入Class）
     */
    private static final ClassValue<Object> INSTANCES = new ClassValue<Object>() {
        /**
         * 先从Bean中寻找，找不到尝试反射直接new，new不出来手动注册Bean再获取bean
         */
        @Override
        protected Object computeValue(Class<?> key) {
            Object beanOrNull = SpringUtil.getBeanOrNull(key);
            if (beanOrNull != null) {
                return beanOrNull;
            }
            try {
                return ReflectionUtils.accessibleConstructor(key).newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                SpringUtil.registerBean(key);
                return SpringUtil.getBean(key);
            }
        }
    };

    public static <T> List<T> getInstance(Class<? extends T>[] clazz) {
        List<T> result = new ArrayList<>();
        for (Class<? extends T> aClass : clazz) {
            result.add(getInstance(aClass));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<? extends T> clazz) {
        return (T) INSTANCES.get(clazz);
    }
}