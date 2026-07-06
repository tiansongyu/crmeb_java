package com.zbkj.service.service.impl;

import com.zbkj.common.model.category.Category;
import com.zbkj.common.model.express.ShippingTemplates;
import com.zbkj.common.request.CategorySearchRequest;
import com.zbkj.common.request.StoreProductAddRequest;
import com.zbkj.common.response.ProductImportResponse;
import com.zbkj.service.service.CategoryService;
import com.zbkj.service.service.ShippingTemplatesService;
import com.zbkj.service.service.StoreProductService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Test
    public void excelImportMergesRowsWithSameProductCodeIntoOneMultiSkuProduct() {
        when(categoryService.getList(any(CategorySearchRequest.class)))
                .thenReturn(Collections.singletonList(category(10001, "户外用品")));
        when(shippingTemplatesService.list())
                .thenReturn(Collections.singletonList(template(10001)));
        when(storeProductService.save(any(StoreProductAddRequest.class))).thenReturn(true);

        ProductImportResponse response = service.importExcelProducts(excelFile(new Object[][]{
                {"TENT001", "骆驼帐篷", "户外用品", "户外,帐篷", "件", "crmebimage/public/product/tent-main.jpg",
                        "crmebimage/public/product/tent-main.jpg,crmebimage/public/product/tent-side.jpg",
                        "<p>户外便携帐篷</p>", "颜色", "卡其色", "尺寸", "双人", 299, 399, 180, 50, 2.5, 0,
                        "crmebimage/public/product/tent-khaki-double.jpg", "TENT-KH-D"},
                {"TENT001", "骆驼帐篷", "户外用品", "户外,帐篷", "件", "crmebimage/public/product/tent-main.jpg",
                        "crmebimage/public/product/tent-main.jpg,crmebimage/public/product/tent-side.jpg",
                        "<p>户外便携帐篷</p>", "颜色", "军绿色", "尺寸", "双人", 319, 419, 190, 30, 2.5, 0,
                        "crmebimage/public/product/tent-green-double.jpg", "TENT-GR-D"}
        }), false);

        ArgumentCaptor<StoreProductAddRequest> captor = ArgumentCaptor.forClass(StoreProductAddRequest.class);
        verify(storeProductService).save(captor.capture());
        StoreProductAddRequest request = captor.getValue();

        assertFalse(response.getDryRun());
        assertEquals(Integer.valueOf(1), response.getTotal());
        assertEquals(Integer.valueOf(1), response.getSuccess());
        assertEquals("骆驼帐篷", request.getStoreName());
        assertEquals("户外,帐篷", request.getKeyword());
        assertEquals("[\"crmebimage/public/product/tent-main.jpg\",\"crmebimage/public/product/tent-side.jpg\"]",
                request.getSliderImage());
        assertEquals(Boolean.TRUE, request.getSpecType());
        assertEquals("颜色", request.getAttr().get(0).getAttrName());
        assertEquals("卡其色,军绿色", request.getAttr().get(0).getAttrValues());
        assertEquals("尺寸", request.getAttr().get(1).getAttrName());
        assertEquals("双人", request.getAttr().get(1).getAttrValues());
        assertEquals(2, request.getAttrValue().size());
        assertEquals("crmebimage/public/product/tent-khaki-double.jpg", request.getAttrValue().get(0).getImage());
        assertEquals("TENT-KH-D", request.getAttrValue().get(0).getBarCode());
    }

    @Test
    public void excelImportReportsOneProductFailureWhenRequiredColumnIsMissing() {
        when(categoryService.getList(any(CategorySearchRequest.class)))
                .thenReturn(Collections.singletonList(category(10001, "户外用品")));
        when(shippingTemplatesService.list())
                .thenReturn(Collections.singletonList(template(10001)));

        ProductImportResponse response = service.importExcelProducts(excelFile(new Object[][]{
                {"", "骆驼帐篷", "户外用品", "户外,帐篷", "件", "crmebimage/public/product/tent-main.jpg",
                        "crmebimage/public/product/tent-main.jpg", "<p>户外便携帐篷</p>", "颜色", "卡其色",
                        "", "", 299, 399, 180, 50, 2.5, 0, "crmebimage/public/product/tent-khaki.jpg", ""}
        }), false);

        assertEquals(Integer.valueOf(1), response.getTotal());
        assertEquals(Integer.valueOf(0), response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getFailed());
        assertFalse(response.getItems().get(0).getSuccess());
        assertEquals("商品编码不能为空", response.getItems().get(0).getMessage());
        verify(storeProductService, never()).save(any(StoreProductAddRequest.class));
    }

    @Test
    public void excelImportReportsProductLevelParseFailureWithoutAbortingWholeFile() {
        when(categoryService.getList(any(CategorySearchRequest.class)))
                .thenReturn(Collections.singletonList(category(10001, "户外用品")));
        when(shippingTemplatesService.list())
                .thenReturn(Collections.singletonList(template(10001)));
        when(storeProductService.save(any(StoreProductAddRequest.class))).thenReturn(true);

        ProductImportResponse response = service.importExcelProducts(excelFileWithHeaders(
                new String[]{"商品编码", "商品名称", "分类", "分类ID", "主图", "轮播图", "售价", "原价", "成本价", "库存"},
                new Object[][]{
                        {"TENT001", "骆驼帐篷", "户外用品", "不是数字", "crmebimage/public/product/tent-main.jpg",
                                "crmebimage/public/product/tent-main.jpg", 299, 399, 180, 50},
                        {"TENT002", "露营灯", "户外用品", "", "crmebimage/public/product/light-main.jpg",
                                "crmebimage/public/product/light-main.jpg", 59, 79, 30, 20}
                }), false);

        assertEquals(Integer.valueOf(2), response.getTotal());
        assertEquals(Integer.valueOf(1), response.getSuccess());
        assertEquals(Integer.valueOf(1), response.getFailed());
        assertFalse(response.getItems().get(0).getSuccess());
        assertEquals("分类ID必须是整数", response.getItems().get(0).getMessage());
        assertTrue(response.getItems().get(1).getSuccess());
        verify(storeProductService).save(any(StoreProductAddRequest.class));
    }

    private MockMultipartFile file(String json) {
        return new MockMultipartFile("file", "products.json", "application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile excelFile(Object[][] rows) {
        String[] headers = {
                "商品编码", "商品名称", "分类", "关键字", "单位", "主图", "轮播图", "详情",
                "规格1名", "规格1值", "规格2名", "规格2值", "售价", "原价", "成本价", "库存",
                "重量", "体积", "SKU图", "商品条码"
        };
        return excelFileWithHeaders(headers, rows);
    }

    private MockMultipartFile excelFileWithHeaders(String[] headers, Object[][] rows) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("商品导入");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = rows[rowIndex];
                for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
                    Cell cell = row.createCell(cellIndex);
                    Object value = values[cellIndex];
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", "products.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
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
