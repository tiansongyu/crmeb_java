package com.zbkj.service.service.impl;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.model.page.PageDiy;
import com.zbkj.service.dao.page.PageDiyDao;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PageDiyServiceImplTest {

    private PageDiyServiceImpl service;

    @Mock
    private PageDiyDao pageDiyDao;

    @Mock
    private SystemConfigService systemConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new PageDiyServiceImpl();
        ReflectionTestUtils.setField(service, "dao", pageDiyDao);
        ReflectionTestUtils.setField(service, "baseMapper", pageDiyDao);
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
    }

    @Test
    public void savePageDiyAllowsBlankApiUrlForTemplateStore() {
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_API_URL)).thenReturn("");
        when(pageDiyDao.selectList(any())).thenReturn(Collections.emptyList());
        when(pageDiyDao.insert(any(PageDiy.class))).thenReturn(1);

        PageDiy pageDiy = new PageDiy();
        pageDiy.setName("模板首页");
        pageDiy.setValue("{\"1001\":{\"name\":\"titles\",\"timestamp\":1001}}");

        PageDiy result = service.savePageDiy(pageDiy);

        assertSame(pageDiy, result);
        assertEquals("{\"1001\":{\"name\":\"titles\",\"timestamp\":1001}}", pageDiy.getValue());
        verify(pageDiyDao).insert(pageDiy);
    }

    @Test
    public void editPageDiyAllowsBlankApiUrlForTemplateStore() {
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_API_URL)).thenReturn("");
        when(pageDiyDao.selectList(any())).thenReturn(Collections.emptyList());
        when(pageDiyDao.updateById(any(PageDiy.class))).thenReturn(1);

        PageDiy pageDiy = new PageDiy();
        pageDiy.setId(1001);
        pageDiy.setName("模板首页");
        pageDiy.setValue("{\"1001\":{\"name\":\"titles\",\"timestamp\":1001}}");

        assertTrue(service.editPageDiy(pageDiy));
        verify(pageDiyDao).updateById(pageDiy);
    }
}
