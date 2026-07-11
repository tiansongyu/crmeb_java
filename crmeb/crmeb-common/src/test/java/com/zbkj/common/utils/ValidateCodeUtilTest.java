package com.zbkj.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ValidateCodeUtilTest {

    @Test
    public void eachCaptchaUsesAnIndependentResultObject() {
        ValidateCodeUtil.Validate first = ValidateCodeUtil.getRandomCode();
        ValidateCodeUtil.Validate second = ValidateCodeUtil.getRandomCode();

        assertNotSame(first, second);
        assertEquals(4, first.getValue().length());
        assertEquals(4, second.getValue().length());
        assertNotNull(first.getBase64Str());
        assertTrue(first.getBase64Str().length() > 100);
    }
}
