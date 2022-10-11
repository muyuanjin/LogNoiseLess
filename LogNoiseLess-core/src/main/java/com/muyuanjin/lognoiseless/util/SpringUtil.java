package com.muyuanjin.lognoiseless.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SpringUtil implements ApplicationContextAware, BeanFactoryPostProcessor {
    private static final AtomicReference<ConfigurableListableBeanFactory> BEAN_FACTORY = new AtomicReference<>();
    private static final AtomicReference<ApplicationContext> APPLICATION_CONTEXT = new AtomicReference<>();

    /**
     * 如果早于Spring上下文或者不在Spring中时，返回null
     */
    @Nullable
    public static ListableBeanFactory getBeanFactory() {
        ConfigurableListableBeanFactory beanFactory = BEAN_FACTORY.get();
        return beanFactory == null ? APPLICATION_CONTEXT.get() : beanFactory;
    }

    @NotNull
    public static ListableBeanFactory getNotNullBeanFactory() {
        ConfigurableListableBeanFactory beanFactory = BEAN_FACTORY.get();
        return Objects.requireNonNull(beanFactory == null ? APPLICATION_CONTEXT.get() : beanFactory);
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BEAN_FACTORY.set(beanFactory);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        APPLICATION_CONTEXT.set(applicationContext);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) getNotNullBeanFactory().getBean(name);
    }

    public static <T> T getBean(Class<? extends T> clazz) {
        return getNotNullBeanFactory().getBean(clazz);
    }

    @Nullable
    public static <T> T getBeanOrNull(Class<? extends T> type) {
        try {
            return getBeanFactory() == null ? null : getBean(type);
        } catch (NoSuchBeanDefinitionException notFound) {
            return null;
        }
    }

    @NotNull
    public static <T> T getBean(String name, Class<? extends T> clazz) {
        return getNotNullBeanFactory().getBean(name, clazz);
    }
}
