package com.zbkj.service.exception;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.exception.ExceptionLog;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.ExceptionLogService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private ExceptionLogService exceptionLogService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "exceptionLogService", exceptionLogService);
    }

    @Test
    public void hidesUnexpectedExceptionDetailsFromClients() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");

        CommonResult<?> result = handler.defaultExceptionHandler(
                request, new RuntimeException("database password was exposed"));

        assertEquals("服务器内部错误，请稍后重试", result.getMessage());
    }

    @Test
    public void loggingFailureDoesNotReplaceTheOriginalResponse() {
        when(exceptionLogService.save(any(ExceptionLog.class)))
                .thenThrow(new RuntimeException("database unavailable"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");

        CommonResult<?> result = handler.defaultExceptionHandler(
                request, new CrmebException("订单状态错误"));

        assertEquals("订单状态错误", result.getMessage());
    }
}
