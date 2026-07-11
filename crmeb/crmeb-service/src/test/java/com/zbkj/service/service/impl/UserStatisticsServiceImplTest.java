package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.response.UserOverviewResponse;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.UserRechargeService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.UserVisitRecordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserStatisticsServiceImplTest {

    private UserStatisticsServiceImpl service;

    @Mock
    private StoreOrderService storeOrderService;

    @Mock
    private UserService userService;

    @Mock
    private UserRechargeService userRechargeService;

    @Mock
    private UserVisitRecordService userVisitRecordService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new UserStatisticsServiceImpl();
        ReflectionTestUtils.setField(service, "storeOrderService", storeOrderService);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "userRechargeService", userRechargeService);
        ReflectionTestUtils.setField(service, "userVisitRecordService", userVisitRecordService);
    }

    @Test
    public void latelySevenMergesEveryMetricExactlyOnce() {
        stubDateValues();
        stubPeriodValues();

        UserOverviewResponse result = service.getOverview(Constants.SEARCH_DATE_LATELY_7);

        assertEquals(Integer.valueOf(12), result.getRegisterNum());
        assertEquals(Integer.valueOf(23), result.getPageviews());
        assertEquals(Integer.valueOf(34), result.getActiveUserNum());
        assertEquals(Integer.valueOf(45), result.getOrderUserNum());
        assertEquals(Integer.valueOf(56), result.getRechargeUserNum());
        assertEquals(Integer.valueOf(67), result.getOrderPayUserNum());
        assertEquals(new BigDecimal("670"), result.getPayOrderAmount());
        assertEquals(new BigDecimal("10.00"), result.getCustomerPrice());
        assertEquals("20%", result.getRegisterNumRatio());
        assertEquals("13%", result.getActiveUserNumRatio());
        assertEquals("12%", result.getRechargeUserNumRatio());
    }

    @Test
    public void customRangeEndingTodayMergesLiveDayData() {
        stubDateValues();
        stubPeriodValues();
        String start = DateUtil.yesterday().toString("yyyy-MM-dd");
        String end = DateUtil.today();

        UserOverviewResponse result = service.getOverview(start + "," + end);

        assertEquals(Integer.valueOf(12), result.getRegisterNum());
        assertEquals(Integer.valueOf(56), result.getRechargeUserNum());
        verify(userService).getRegisterNumByPeriod(start, start);
    }

    @Test
    public void emptyMetricsDoNotCauseNullOrDivideByZeroFailures() {
        UserOverviewResponse result = service.getOverview(Constants.SEARCH_DATE_DAY);

        assertEquals(BigDecimal.ZERO, result.getCustomerPrice());
        assertEquals("0%", result.getRegisterNumRatio());
        assertEquals("0%", result.getActiveUserNumRatio());
        assertEquals("0%", result.getRechargeUserNumRatio());
    }

    private void stubDateValues() {
        when(userService.getRegisterNumByDate(anyString())).thenReturn(2);
        when(userVisitRecordService.getPageviewsByDate(anyString())).thenReturn(3);
        when(userVisitRecordService.getActiveUserNumByDate(anyString())).thenReturn(4);
        when(storeOrderService.getOrderUserNumByDate(anyString())).thenReturn(5);
        when(userRechargeService.getRechargeUserNumByDate(anyString())).thenReturn(6);
        when(storeOrderService.getOrderPayUserNumByDate(anyString())).thenReturn(7);
        when(storeOrderService.getPayOrderAmountByDate(anyString())).thenReturn(new BigDecimal("70"));
    }

    private void stubPeriodValues() {
        when(userService.getRegisterNumByPeriod(anyString(), anyString())).thenReturn(10);
        when(userVisitRecordService.getPageviewsByPeriod(anyString(), anyString())).thenReturn(20);
        when(userVisitRecordService.getActiveUserNumByPeriod(anyString(), anyString())).thenReturn(30);
        when(storeOrderService.getOrderUserNumByPeriod(anyString(), anyString())).thenReturn(40);
        when(userRechargeService.getRechargeUserNumByPeriod(anyString(), anyString())).thenReturn(50);
        when(storeOrderService.getOrderPayUserNumByPeriod(anyString(), anyString())).thenReturn(60);
        when(storeOrderService.getPayOrderAmountByPeriod(anyString(), anyString())).thenReturn(new BigDecimal("600"));
    }
}
