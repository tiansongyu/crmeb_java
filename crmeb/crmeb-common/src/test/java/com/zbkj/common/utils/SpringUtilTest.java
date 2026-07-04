package com.zbkj.common.utils;

import org.junit.Test;
import org.springframework.context.annotation.Lazy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SpringUtilTest {

    @Test
    public void springUtilDisablesLazyInitializationForContextHolder() {
        Lazy lazy = SpringUtil.class.getAnnotation(Lazy.class);

        assertNotNull("SpringUtil must initialize eagerly when global lazy initialization is enabled", lazy);
        assertFalse(lazy.value());
    }
}
