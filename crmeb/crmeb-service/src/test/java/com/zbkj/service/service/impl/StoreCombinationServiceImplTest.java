package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.combination.StoreCombination;
import com.zbkj.common.model.combination.StorePink;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttr;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.CombinationDetailResponse;
import com.zbkj.common.response.GoPinkResponse;
import com.zbkj.service.dao.StoreCombinationDao;
import com.zbkj.service.service.StorePinkService;
import com.zbkj.service.service.StoreProductAttrService;
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
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StoreCombinationServiceImplTest {

    private StoreCombinationServiceImpl service;

    @Mock
    private StoreCombinationDao storeCombinationDao;

    @Mock
    private StorePinkService storePinkService;

    @Mock
    private StoreProductAttrService storeProductAttrService;

    @Mock
    private StoreProductAttrValueService storeProductAttrValueService;

    @Mock
    private UserService userService;

    @Mock
    private StoreProductService storeProductService;

    @Mock
    private StoreProductDescriptionService storeProductDescriptionService;

    @Mock
    private UserVisitRecordService userVisitRecordService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new StoreCombinationServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", storeCombinationDao);
        ReflectionTestUtils.setField(service, "dao", storeCombinationDao);
        ReflectionTestUtils.setField(service, "storePinkService", storePinkService);
        ReflectionTestUtils.setField(service, "storeProductAttrService", storeProductAttrService);
        ReflectionTestUtils.setField(service, "storeProductAttrValueService", storeProductAttrValueService);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "storeProductService", storeProductService);
        ReflectionTestUtils.setField(service, "storeProductDescriptionService", storeProductDescriptionService);
        ReflectionTestUtils.setField(service, "userVisitRecordService", userVisitRecordService);
    }

    @Test
    public void goPinkFallsBackToMasterProductSkuWhenCombinationSkuAttrIsMissing() {
        StorePink teamPink = new StorePink();
        teamPink.setId(1);
        teamPink.setUid(41);
        teamPink.setCid(33);
        teamPink.setPid(90);
        teamPink.setPeople(2);
        teamPink.setStatus(1);
        teamPink.setKId(0);
        teamPink.setIsRefund(false);
        teamPink.setOrderId("order-test");
        teamPink.setTotalNum(1);
        teamPink.setTotalPrice(new BigDecimal("19.90"));
        teamPink.setPrice(new BigDecimal("19.90"));

        StoreCombination combination = new StoreCombination();
        combination.setId(33);
        combination.setProductId(90);
        combination.setPeople(2);
        combination.setPrice(new BigDecimal("19.90"));
        combination.setStock(10);
        combination.setQuota(10);
        combination.setQuotaShow(10);
        combination.setIsShow(true);
        combination.setIsDel(false);
        combination.setStopTime(System.currentTimeMillis() + 3600000L);

        User user = new User();
        user.setUid(41);
        user.setNickname("tester");
        user.setAvatar("avatar.png");

        StoreProductAttr masterAttr = new StoreProductAttr();
        masterAttr.setId(501);
        masterAttr.setProductId(90);
        masterAttr.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterAttr.setAttrName("颜色");
        masterAttr.setAttrValues("[红色]");

        StoreProductAttrValue masterValue = new StoreProductAttrValue();
        masterValue.setId(601);
        masterValue.setProductId(90);
        masterValue.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterValue.setSuk("红色");
        masterValue.setPrice(new BigDecimal("29.90"));
        masterValue.setStock(20);

        when(storePinkService.getById(1)).thenReturn(teamPink);
        when(storePinkService.getListByCidAndKid(33, 1)).thenReturn(new ArrayList<>());
        when(storeCombinationDao.selectById(33)).thenReturn(combination);
        when(userService.getInfo()).thenReturn(user);
        when(storeProductAttrService.getByEntity(any(StoreProductAttr.class))).thenReturn(Collections.emptyList());
        when(storeProductAttrService.getListByProductIdAndType(90, Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(Collections.singletonList(masterAttr));
        when(storeProductAttrValueService.getByEntity(any(StoreProductAttrValue.class))).thenReturn(Collections.emptyList());
        when(storeProductAttrValueService.getListByProductIdAndType(90, Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(Collections.singletonList(masterValue));

        GoPinkResponse response = service.goPink(1);

        assertNotNull(response.getStoreCombination());
        assertFalse(response.getStoreCombination().getProductAttr().isEmpty());
        assertEquals(masterAttr.getId(), response.getStoreCombination().getAloneAttrValueId());
        assertNotNull(response.getStoreCombination().getProductValue().get(masterValue.getSuk()));
    }

    @Test
    public void getH5DetailFallsBackToMasterProductSkuAttrWhenCombinationSkuAttrIsMissing() {
        StoreCombination combination = new StoreCombination();
        combination.setId(33);
        combination.setProductId(90);
        combination.setPeople(2);
        combination.setTitle("combination-test");
        combination.setImages("image-a,image-b");
        combination.setIsShow(true);
        combination.setIsDel(false);
        combination.setStock(10);
        combination.setQuota(10);
        combination.setQuotaShow(10);

        StoreProduct product = new StoreProduct();
        product.setId(90);
        product.setIsDel(false);
        product.setIsShow(true);
        product.setStock(20);
        product.setSales(3);
        product.setFicti(1);

        StoreProductAttr masterAttr = new StoreProductAttr();
        masterAttr.setId(501);
        masterAttr.setProductId(90);
        masterAttr.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterAttr.setAttrName("颜色");
        masterAttr.setAttrValues("[红色]");

        StoreProductAttrValue masterValue = new StoreProductAttrValue();
        masterValue.setId(601);
        masterValue.setProductId(90);
        masterValue.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterValue.setSuk("红色");
        masterValue.setPrice(new BigDecimal("29.90"));
        masterValue.setStock(20);

        when(storeCombinationDao.selectById(33)).thenReturn(combination);
        when(storeProductService.getById(90)).thenReturn(product);
        when(storeProductAttrService.getListByProductIdAndType(33, Constants.PRODUCT_TYPE_PINGTUAN))
                .thenReturn(Collections.emptyList());
        when(storeProductAttrService.getByEntity(any(StoreProductAttr.class))).thenReturn(Collections.emptyList());
        when(storeProductAttrService.getListByProductIdAndType(90, Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(Collections.singletonList(masterAttr));
        when(storeProductAttrValueService.getListByProductIdAndType(90, Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(Collections.singletonList(masterValue));
        when(storeProductAttrValueService.getListByProductIdAndType(33, Constants.PRODUCT_TYPE_PINGTUAN))
                .thenReturn(Collections.emptyList());
        when(storePinkService.getListByCidAndKid(33, 0)).thenReturn(Collections.emptyList());
        when(userService.getUserId()).thenReturn(41);

        CombinationDetailResponse response = service.getH5Detail(33);

        assertFalse(response.getProductAttr().isEmpty());
        assertEquals(masterAttr.getId(), response.getProductAttr().get(0).getId());
        assertNotNull(response.getProductValue().get(masterValue.getSuk()));
    }
}
