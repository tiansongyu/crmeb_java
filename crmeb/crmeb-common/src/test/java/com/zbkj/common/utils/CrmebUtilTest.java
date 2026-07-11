package com.zbkj.common.utils;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class CrmebUtilTest {

    @Test
    public void percentageHandlesMissingAndZeroTotals() {
        assertEquals(0, CrmebUtil.percentInstanceIntVal(10, 0));
        assertEquals(0, CrmebUtil.percentInstanceIntVal((Integer) null, 10));
        assertEquals(0, CrmebUtil.percentInstanceIntVal(BigDecimal.TEN, new BigDecimal("0.00")));
    }

    @Test
    public void percentageIsClampedToDisplayRange() {
        assertEquals(100, CrmebUtil.percentInstanceIntVal(11, 10));
        assertEquals(0, CrmebUtil.percentInstanceIntVal(-1, 10));
        assertEquals(50, CrmebUtil.percentInstanceIntVal(1, 2));
    }

    @Test
    public void growthRateRecognizesScaledZero() {
        assertEquals(100, CrmebUtil.getRate(BigDecimal.TEN, new BigDecimal("0.00")));
    }
}
