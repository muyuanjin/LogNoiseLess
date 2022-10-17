package com.muyuanjin.lognoiseless.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.lang.NonNull;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SpringUtil implements ApplicationContextAware, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor {
    private static final AtomicReference<ConfigurableListableBeanFactory> BEAN_FACTORY = new AtomicReference<>();
    private static final AtomicReference<ApplicationContext> APPLICATION_CONTEXT = new AtomicReference<>();
    private static final ScopeMetadataResolver SCOPE_METADATA_RESOLVER = new AnnotationScopeMetadataResolver();
    private static final AtomicReference<BeanDefinitionRegistry> REGISTRY = new AtomicReference<>();

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        REGISTRY.set(beanDefinitionRegistry);
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BEAN_FACTORY.set(beanFactory);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        APPLICATION_CONTEXT.set(applicationContext);
    }

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


    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) getNotNullBeanFactory().getBean(name);
    }

    public static <T> T getBean(Class<? extends T> clazz) {
        return getNotNullBeanFactory().getBean(clazz);
    }

    public static String registerBean(Class<?> beanClazz) throws BeanDefinitionStoreException {
        return BeanDefinitionReaderUtils.registerWithGeneratedName(getAutowireBeanDefinition(beanClazz), REGISTRY.get());
    }

    /**
     * 注销bean
     * <p>
     * 将Spring中的bean注销，请谨慎使用
     *
     * @param beanName bean名称
     */
    public static void unregisterBean(String beanName) {
        if (BEAN_FACTORY.get() instanceof DefaultSingletonBeanRegistry) {
            DefaultSingletonBeanRegistry registry = (DefaultSingletonBeanRegistry) BEAN_FACTORY.get();
            registry.destroySingleton(beanName);
        } else {
            throw new UnsupportedOperationException("Can not unregister bean, the factory is not a DefaultSingletonBeanRegistry!");
        }
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

    /**
     * 获取bean定义，已经设置好按照类型自动注入依赖
     */
    private static AnnotatedGenericBeanDefinition getAutowireBeanDefinition(Class<?> beanClazz) {
        AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(beanClazz);
        ScopeMetadata scopeMetadata = SCOPE_METADATA_RESOLVER.resolveScopeMetadata(beanDefinition);
        beanDefinition.setScope(scopeMetadata.getScopeName());
        AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDefinition);
        boolean autowireConstructor = false;
        for (Constructor<?> constructor : beanClazz.getConstructors()) {
            if (constructor.getParameterCount() > 0) {
                autowireConstructor = true;
                break;
            }
        }
        beanDefinition.setAutowireMode(autowireConstructor ? AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR : AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        //org.springframework.beans.factory.support.DefaultListableBeanFactory.registerBeanDefinition
        beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
        return beanDefinition;
    }
}
