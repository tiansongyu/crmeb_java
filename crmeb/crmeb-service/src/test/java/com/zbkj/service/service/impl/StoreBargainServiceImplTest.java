package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.ProductConstants;
import com.zbkj.common.model.bargain.StoreBargain;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.product.StoreProductDescription;
import com.zbkj.common.response.BargainDetailH5Response;
import com.zbkj.service.dao.StoreBargainDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductDescriptionService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.UserVisitRecordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StoreBargainServiceImplTest {

    private StoreBargainServiceImpl service;

    @Mock
    private StoreBargainDao storeBargainDao;

    @Mock
    private StoreProductAttrValueService attrValueService;

    @Mock
    private StoreProductDescriptionService storeProductDescriptionService;

    @Mock
    private StoreProductService storeProductService;

    @Mock
    private UserService userService;

    @Mock
    private UserVisitRecordService userVisitRecordService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new StoreBargainServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", storeBargainDao);
        ReflectionTestUtils.setField(service, "dao", storeBargainDao);
        ReflectionTestUtils.setField(service, "attrValueService", attrValueService);
        ReflectionTestUtils.setField(service, "storeProductDescriptionService", storeProductDescriptionService);
        ReflectionTestUtils.setField(service, "storeProductService", storeProductService);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "userVisitRecordService", userVisitRecordService);
    }

    @Test
    public void getH5DetailFallsBackToMasterProductSkuWhenBargainSkuValueIsMissing() {
        StoreBargain bargain = new StoreBargain();
        bargain.setId(22);
        bargain.setProductId(90);
        bargain.setTitle("bargain-test");
        bargain.setStatus(true);
        bargain.setIsDel(false);
        bargain.setStock(10);
        bargain.setQuota(10);
        bargain.setQuotaShow(10);
        bargain.setPrice(new BigDecimal("29.90"));
        bargain.setMinPrice(new BigDecimal("19.90"));

        StoreProduct product = new StoreProduct();
        product.setId(90);
        product.setIsDel(false);
        product.setIsShow(true);
        product.setStock(20);

        StoreProductAttrValue masterValue = new StoreProductAttrValue();
        masterValue.setId(601);
        masterValue.setProductId(90);
        masterValue.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterValue.setSuk("红色");
        masterValue.setPrice(new BigDecimal("29.90"));
        masterValue.setStock(20);

        when(storeBargainDao.selectById(22)).thenReturn(bargain);
        when(storeProductService.getById(90)).thenReturn(product);
        when(attrValueService.getListByProductIdAndType(22, ProductConstants.PRODUCT_TYPE_BARGAIN))
                .thenReturn(Collections.emptyList());
        when(attrValueService.getListByProductIdAndType(90, Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(Collections.singletonList(masterValue));
        when(storeProductDescriptionService.getOne(any(Wrapper.class))).thenReturn((StoreProductDescription) null);
        when(userService.getUserId()).thenReturn(41);

        BargainDetailH5Response response = service.getH5Detail(22);

        assertNotNull(response);
        assertEquals(masterValue.getId(), response.getAttrValueId());
        assertEquals(masterValue.getSuk(), response.getSku());
    }
}
