package com.muyuanjin.lognoiseless.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class SpringEnvUtil implements EnvironmentPostProcessor {
    private static final AtomicReference<ConfigurableEnvironment> ENVIRONMENT = new AtomicReference<>(new AbstractEnvironment() {
    });

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        SpringEnvUtil.ENVIRONMENT.set(environment);
    }

    public static String[] getActiveProfiles() {
        return ENVIRONMENT.get().getActiveProfiles();
    }

    public static boolean isEnvReady() {
        return !ENVIRONMENT.get().getClass().equals(NoNpeEnv.class);
    }

    public static boolean isProActive() {
        if (log.isDebugEnabled()) {
            return false;
        }
        for (String activeProfile : ENVIRONMENT.get().getActiveProfiles()) {
            if (activeProfile.equals("prod")) {
                return true;
            }
        }
        return false;
    }

    public static String getProperty(String key) {
        return ENVIRONMENT.get().getProperty(key);
    }


    private static class NoNpeEnv extends AbstractEnvironment {

    }
}
