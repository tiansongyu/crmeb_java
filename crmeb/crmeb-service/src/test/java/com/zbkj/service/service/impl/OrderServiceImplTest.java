package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.model.combination.StoreCombination;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.PreOrderDetailRequest;
import com.zbkj.common.vo.OrderInfoDetailVo;
import com.zbkj.service.service.StoreCombinationService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class OrderServiceImplTest {

    private OrderServiceImpl service;

    @Mock
    private StoreCombinationService storeCombinationService;

    @Mock
    private StoreProductAttrValueService storeProductAttrValueService;

    @Mock
    private StoreProductService storeProductService;

    @Mock
    private StoreOrderService storeOrderService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "storeCombinationService", storeCombinationService);
        ReflectionTestUtils.setField(service, "storeProductAttrValueService", storeProductAttrValueService);
        ReflectionTestUtils.setField(service, "storeProductService", storeProductService);
        ReflectionTestUtils.setField(service, "storeOrderService", storeOrderService);
    }

    @Test
    public void validatePreOrderCombinationMapsMasterSkuIdToCombinationSku() {
        StoreCombination combination = new StoreCombination();
        combination.setId(34);
        combination.setProductId(91);
        combination.setTitle("模板拼团商品");
        combination.setStock(20);
        combination.setOnceNum(1);
        combination.setNum(20);
        combination.setStartTime(System.currentTimeMillis() - 1000L);
        combination.setStopTime(System.currentTimeMillis() + 3600000L);

        StoreProductAttrValue masterSku = new StoreProductAttrValue();
        masterSku.setId(1140);
        masterSku.setProductId(91);
        masterSku.setType(Constants.PRODUCT_TYPE_NORMAL);
        masterSku.setSuk("默认规格");
        masterSku.setStock(9);

        StoreProductAttrValue combinationSku = new StoreProductAttrValue();
        combinationSku.setId(1358);
        combinationSku.setProductId(34);
        combinationSku.setType(Constants.PRODUCT_TYPE_PINGTUAN);
        combinationSku.setSuk(masterSku.getSuk());
        combinationSku.setPrice(new BigDecimal("170.60"));
        combinationSku.setStock(9);
        combinationSku.setQuota(20);
        combinationSku.setImage("template-product.jpg");
        combinationSku.setVolume(BigDecimal.ZERO);
        combinationSku.setWeight(BigDecimal.ZERO);

        StoreProduct product = new StoreProduct();
        product.setId(91);
        product.setStock(9);
        product.setIsDel(false);

        User user = new User();
        user.setUid(41);

        PreOrderDetailRequest request = new PreOrderDetailRequest();
        request.setCombinationId(34);
        request.setProductId(91);
        request.setAttrValueId(1140);
        request.setProductNum(1);

        when(storeCombinationService.getByIdException(34)).thenReturn(combination);
        when(storeProductAttrValueService.getByIdAndProductIdAndType(1140, 34, Constants.PRODUCT_TYPE_PINGTUAN))
                .thenReturn(null);
        when(storeProductAttrValueService.getById(1140)).thenReturn(masterSku);
        when(storeProductAttrValueService.getByProductIdAndSkuAndType(34, masterSku.getSuk(), Constants.PRODUCT_TYPE_PINGTUAN))
                .thenReturn(combinationSku);
        when(storeProductService.getById(91)).thenReturn(product);
        when(storeProductAttrValueService.getByProductIdAndSkuAndType(91, masterSku.getSuk(), Constants.PRODUCT_TYPE_NORMAL))
                .thenReturn(masterSku);
        when(storeOrderService.getUserCurrentCombinationOrders(41, 34)).thenReturn(Collections.emptyList());

        OrderInfoDetailVo detailVo = ReflectionTestUtils.invokeMethod(service, "validatePreOrderCombination", request, user);

        assertEquals(combinationSku.getId(), detailVo.getAttrValueId());
        assertEquals(combinationSku.getSuk(), detailVo.getSku());
        assertEquals(Constants.PRODUCT_TYPE_PINGTUAN, detailVo.getProductType());
    }
}
