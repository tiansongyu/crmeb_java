package com.zbkj.service.service.impl;

import com.zbkj.common.model.category.Category;
import com.zbkj.common.model.express.ShippingTemplates;
import com.zbkj.common.request.CategorySearchRequest;
import com.zbkj.common.request.StoreProductAddRequest;
import com.zbkj.common.response.ProductImportResponse;
import com.zbkj.service.service.CategoryService;
import com.zbkj.service.service.ShippingTemplatesService;
import com.zbkj.service.service.StoreProductService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductImportServiceImplTest {

    private ProductImportServiceImpl service;

    @Mock
    private StoreProductService storeProductService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ShippingTemplatesService shippingTemplatesService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new ProductImportServiceImpl();
        ReflectionTestUtils.setField(service, "storeProductService", storeProductService);
        ReflectionTestUtils.setField(service, "categoryService", categoryService);
        ReflectionTestUtils.setField(service, "shippingTemplatesService", shippingTemplatesService);
    }

    @Test
    public void dryRunValidatesWithoutSavingOrCreatingRecords() {
        when(categoryService.getList(any(CategorySearchRequest.class)))
                .thenReturn(Collections.singletonList(category(10001, "模板分类一")));
        when(shippingTemplatesService.list())
                .thenReturn(Collections.singletonList(template(10001)));

        ProductImportResponse response = service.importProducts(file(singleProductJson("模板分类一")), true);

        assertTrue(response.getDryRun());
        assertEquals(Integer.valueOf(1), response.getTotal());
        assertEquals(Integer.valueOf(1), response.getSuccess());
        assertEquals(Integer.valueOf(0), response.getFailed());
        verify(storeProductService, never()).save(any(StoreProductAddRequest.class));
        verify(categoryService, never()).save(any(Category.class));
        verify(shippingTemplatesService, never()).save(any(ShippingTemplates.class));
    }

    @Test
    public void importCreatesMissingCategoryAndDefaultShippingTemplateThenSavesProduct() {
        when(categoryService.getList(any(CategorySearchRequest.class))).thenReturn(Collections.emptyList());
        when(categoryService.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(20001);
            return true;
        });
        when(shippingTemplatesService.list()).thenReturn(Collections.emptyList());
        when(shippingTemplatesService.save(any(ShippingTemplates.class))).thenAnswer(invocation -> {
            ShippingTemplates template = invocation.getArgument(0);
            template.setId(30001);
            return true;
        });
        when(storeProductService.save(any(StoreProductAddRequest.class))).thenReturn(true);

        ProductImportResponse response = service.importProducts(file(singleProductJson("新模板分类")), false);

        ArgumentCaptor<StoreProductAddRequest> captor = ArgumentCaptor.forClass(StoreProductAddRequest.class);
        verify(storeProductService).save(captor.capture());
        StoreProductAddRequest request = captor.getValue();

        assertFalse(response.getDryRun());
        assertEquals(Integer.valueOf(1), response.getTotal());
        assertEquals(Integer.valueOf(1), response.getSuccess());
        assertEquals("20001", request.getCateId());
        assertEquals(Integer.valueOf(30001), request.getTempId());
        assertEquals(Boolean.FALSE, request.getSpecType());
        assertEquals("[\"crmebimage/public/product/demo-a.jpg\"]", request.getSliderImage());
        assertEquals("模板商品A", request.getStoreName());
        assertEquals("规格", request.getAttr().get(0).getAttrName());
        assertEquals("默认", request.getAttr().get(0).getAttrValues());
    }

    @Test
    public void importContinuesWhenOneRowFailsValidation() {
        when(categoryService.getList(any(CategorySearchRequest.class)))
                .thenReturn(Collections.singletonList(category(10001, "模板分类一")));
        when(shippingTemplatesService.list())
                .thenReturn(Collections.singletonList(template(10001)));
        when(storeProductService.save(any(StoreProductAddRequest.class))).thenReturn(true);

        ProductImportResponse response = service.importProducts(file(twoProductsFirstInvalidJson()), false);

        assertEquals(Integer.valueOf(2), response.getTotal());
        assertEquals(Integer.valueOf(1), response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getFailed());
        assertFalse(response.getItems().get(0).getSuccess());
        assertTrue(response.getItems().get(1).getSuccess());
        verify(storeProductService).save(any(StoreProductAddRequest.class));
    }

    private MockMultipartFile file(String json) {
        return new MockMultipartFile("file", "products.json", "application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private Category category(Integer id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(1);
        category.setStatus(true);
        return category;
    }

    private ShippingTemplates template(Integer id) {
        ShippingTemplates template = new ShippingTemplates();
        template.setId(id);
        template.setName("默认包邮模板");
        template.setType(1);
        template.setAppoint(0);
        return template;
    }

    private String singleProductJson(String categoryName) {
        return "{"
                + "\"products\":["
                + productJsonObject("模板商品A", categoryName)
                + "]"
                + "}";
    }

    private String productJsonObject(String storeName, String categoryName) {
        return "{"
                + "\"storeName\":\"" + storeName + "\","
                + "\"categoryName\":\"" + categoryName + "\","
                + "\"keyword\":\"模板商品\","
                + "\"unitName\":\"件\","
                + "\"image\":\"crmebimage/public/product/demo-a.jpg\","
                + "\"sliderImages\":[\"crmebimage/public/product/demo-a.jpg\"],"
                + "\"content\":\"<p>商品详情</p>\","
                + "\"skus\":[{"
                + "\"specs\":{\"规格\":\"默认\"},"
                + "\"price\":99,"
                + "\"otPrice\":129,"
                + "\"cost\":50,"
                + "\"stock\":100,"
                + "\"weight\":0,"
                + "\"volume\":0,"
                + "\"image\":\"crmebimage/public/product/demo-a.jpg\""
                + "}]"
                + "}";
    }

    private String twoProductsFirstInvalidJson() {
        return "{"
                + "\"products\":["
                + "{"
                + "\"categoryName\":\"模板分类一\","
                + "\"keyword\":\"模板商品\","
                + "\"unitName\":\"件\","
                + "\"image\":\"crmebimage/public/product/demo-a.jpg\","
                + "\"sliderImages\":[\"crmebimage/public/product/demo-a.jpg\"],"
                + "\"skus\":[{\"specs\":{\"规格\":\"默认\"},\"price\":99,\"otPrice\":129,\"cost\":50,\"stock\":100}]"
                + "},"
                + productJsonObject("模板商品B", "模板分类一")
                + "]"
                + "}";
    }
}
