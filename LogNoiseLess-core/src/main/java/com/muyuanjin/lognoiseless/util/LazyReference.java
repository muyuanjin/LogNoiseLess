package com.muyuanjin.lognoiseless.util;

import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.function.Supplier;


/**
 * 懒加载引用容器
 */
public class LazyReference<T> {
    private volatile Supplier<? extends T> supplier;
    private T value;

    public LazyReference(Supplier<? extends T> supplier) {
        Assert.notNull(supplier, "supplier can not be null");
        this.supplier = supplier;
    }

    public T get() {
        if (this.supplier != null) synchronized (this) {
            if (this.supplier != null) {
                this.value = this.supplier.get();
                this.supplier = null;
            }
        }
        return value;
    }

    /**
     * 是否已经调用过 supplier 执行过初始化
     */
    public boolean isInitialized() {
        return supplier == null;
    }

    /**
     * 延迟初始化，延迟到 spring 属性已刷新后再初始化，在此之前会返回null
     */
    @Nullable
    public T getIfSpringEnvReady() {
        if (isInitialized()) {
            return get();
        }
        if (SpringEnvUtil.isEnvReady()) {
            return get();
        }
        return null;
    }
}