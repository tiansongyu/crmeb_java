package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.user.UserBill;
import com.zbkj.service.dao.UserBillDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserBillServiceImplTest {

    private UserBillServiceImpl service;

    @Mock
    private UserBillDao dao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new UserBillServiceImpl();
        ReflectionTestUtils.setField(service, "dao", dao);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sumsInSqlAndAppliesOnlyTheRequestedBillType() {
        UserBill aggregate = new UserBill();
        aggregate.setNumber(new BigDecimal("12.345"));
        when(dao.selectOne(any(QueryWrapper.class))).thenReturn(aggregate);

        BigDecimal result = service.getSumBigDecimal(
                null, 7, Constants.USER_BILL_CATEGORY_MONEY, null, Constants.USER_BILL_TYPE_BROKERAGE);

        assertEquals(new BigDecimal("12.34"), result);
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(dao).selectOne(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        Collection<Object> parameters = captor.getValue().getParamNameValuePairs().values();
        assertTrue(parameters.contains(Constants.USER_BILL_TYPE_BROKERAGE));
        assertFalse(parameters.contains(Constants.USER_BILL_TYPE_PAY_PRODUCT_REFUND));
        assertTrue(sqlSegment.contains("type"));
        assertTrue(captor.getValue().getSqlSelect().contains("SUM(number)"));
    }
}
