package com.zbkj.common.utils;

import com.zbkj.common.constants.OfflinePayConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflinePayUtilTest {

    @Test
    public void labelsKnownStatuses() {
        assertEquals("未提交", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_NOT_SUBMITTED));
        assertEquals("待审核", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_PENDING));
        assertEquals("已通过", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_APPROVED));
        assertEquals("已驳回", OfflinePayUtil.getStatusText(OfflinePayConstants.STATUS_REJECTED));
    }

    @Test
    public void detectsPendingReview() {
        assertTrue(OfflinePayUtil.isPending(OfflinePayConstants.STATUS_PENDING));
        assertFalse(OfflinePayUtil.isPending(OfflinePayConstants.STATUS_NOT_SUBMITTED));
        assertFalse(OfflinePayUtil.isPending(null));
    }

    @Test
    public void detectsRejectedReview() {
        assertTrue(OfflinePayUtil.isRejected(OfflinePayConstants.STATUS_REJECTED));
        assertFalse(OfflinePayUtil.isRejected(OfflinePayConstants.STATUS_PENDING));
        assertFalse(OfflinePayUtil.isRejected(null));
    }
}
