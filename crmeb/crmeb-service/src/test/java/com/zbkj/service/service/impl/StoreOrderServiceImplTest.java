package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SystemWriteOffOrderSearchRequest;
import com.zbkj.common.response.SystemWriteOffOrderResponse;
import com.zbkj.service.dao.StoreOrderDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StoreOrderServiceImplTest {

    private StoreOrderServiceImpl service;

    @Mock
    private StoreOrderDao dao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (TableInfoHelper.getTableInfo(StoreOrder.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "test"), StoreOrder.class);
        }
        service = new StoreOrderServiceImpl();
        ReflectionTestUtils.setField(service, "dao", dao);
    }

    @Test
    public void writeOffSearchPassesKeywordsAsBoundParameters() {
        String maliciousKeyword = "' OR 1=1 --";
        SystemWriteOffOrderResponse summary = new SystemWriteOffOrderResponse()
                .setOrderTotalPrice(BigDecimal.ZERO)
                .setRefundTotalPrice(BigDecimal.ZERO)
                .setRefundTotal(0);
        when(dao.getWriteOffSummary(null, null, maliciousKeyword, null, null)).thenReturn(summary);
        when(dao.selectList(any(Wrapper.class))).thenReturn(Collections.<StoreOrder>emptyList());

        SystemWriteOffOrderSearchRequest request = new SystemWriteOffOrderSearchRequest();
        request.setKeywords(maliciousKeyword);
        PageParamRequest page = new PageParamRequest();
        page.setPage(1);
        page.setLimit(20);

        SystemWriteOffOrderResponse result = service.getWriteOffList(request, page);

        verify(dao).getWriteOffSummary(null, null, maliciousKeyword, null, null);
        assertEquals(BigDecimal.ZERO, result.getOrderTotalPrice());
        assertEquals(Long.valueOf(0L), result.getTotal());
    }

    @Test
    public void numericKeywordIsBoundAsAnOrderIdToo() {
        SystemWriteOffOrderResponse summary = new SystemWriteOffOrderResponse();
        when(dao.getWriteOffSummary(null, null, "42", 42, null)).thenReturn(summary);
        when(dao.selectList(any(Wrapper.class))).thenReturn(Collections.<StoreOrder>emptyList());

        SystemWriteOffOrderSearchRequest request = new SystemWriteOffOrderSearchRequest();
        request.setKeywords("42");
        PageParamRequest page = new PageParamRequest();
        page.setPage(1);
        page.setLimit(20);

        service.getWriteOffList(request, page);

        verify(dao).getWriteOffSummary(null, null, "42", 42, null);
    }

    @Test
    public void userSpendIsAggregatedByTheDatabase() {
        StoreOrder aggregate = new StoreOrder();
        aggregate.setPayPrice(new BigDecimal("123.45"));
        when(dao.selectOne(any(QueryWrapper.class))).thenReturn(aggregate);

        BigDecimal result = service.getSumPayPriceByUid(7);

        assertEquals(new BigDecimal("123.45"), result);
        verify(dao).selectOne(any(QueryWrapper.class));
    }
}
