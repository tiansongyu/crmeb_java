package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.category.Category;
import com.zbkj.common.model.express.ShippingTemplates;
import com.zbkj.common.request.CategorySearchRequest;
import com.zbkj.common.request.ProductImportFileRequest;
import com.zbkj.common.request.ProductImportItemRequest;
import com.zbkj.common.request.ProductImportSkuRequest;
import com.zbkj.common.request.StoreProductAddRequest;
import com.zbkj.common.request.StoreProductAttrAddRequest;
import com.zbkj.common.request.StoreProductAttrValueAddRequest;
import com.zbkj.common.response.ProductImportItemResponse;
import com.zbkj.common.response.ProductImportResponse;
import com.zbkj.service.service.CategoryService;
import com.zbkj.service.service.ProductImportService;
import com.zbkj.service.service.ShippingTemplatesService;
import com.zbkj.service.service.StoreProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductImportServiceImpl implements ProductImportService {

    private static final int MAX_PRODUCTS_PER_FILE = 200;
    private static final String DEFAULT_CATEGORY_PATH = "/0/";
    private static final String DEFAULT_SHIPPING_TEMPLATE_NAME = "默认包邮模板";
    private static final String DEFAULT_SPEC_NAME = "规格";
    private static final String DEFAULT_SPEC_VALUE = "默认";

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ShippingTemplatesService shippingTemplatesService;

    @Override
    public ProductImportResponse importProducts(MultipartFile file, Boolean dryRun) {
        boolean validateOnly = !Boolean.FALSE.equals(dryRun);
        ProductImportFileRequest request = parseFile(file);
        List<ProductImportItemRequest> products = request.getProducts();
        validateProductCount(products);

        ProductImportResponse response = new ProductImportResponse();
        response.setTotal(products.size());
        response.setDryRun(validateOnly);

        for (int i = 0; i < products.size(); i++) {
            ProductImportItemRequest item = products.get(i);
            ProductImportItemResponse itemResponse = importOne(i + 1, item, validateOnly);
            response.getItems().add(itemResponse);
            if (Boolean.TRUE.equals(itemResponse.getSuccess())) {
                response.setSuccess(response.getSuccess() + 1);
            } else {
                response.setFailed(response.getFailed() + 1);
            }
        }
        return response;
    }

    private ProductImportItemResponse importOne(Integer row, ProductImportItemRequest item, boolean dryRun) {
        ProductImportItemResponse response = new ProductImportItemResponse();
        response.setRow(row);
        response.setStoreName(item == null ? "" : item.getStoreName());
        try {
            if (item == null) {
                throw new CrmebException("商品数据不能为空");
            }
            validateProductBase(item);
            Integer categoryId = resolveCategoryId(item, dryRun);
            Integer tempId = resolveShippingTemplateId(item.getTempId(), dryRun);
            StoreProductAddRequest saveRequest = buildSaveRequest(item, categoryId, tempId);
            if (!dryRun) {
                Boolean saved = storeProductService.save(saveRequest);
                if (!Boolean.TRUE.equals(saved)) {
                    throw new CrmebException("商品保存失败");
                }
            }
            response.setSuccess(true);
            response.setMessage(dryRun ? "校验通过" : "导入成功");
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(normalizeMessage(e));
        }
        return response;
    }

    private ProductImportFileRequest parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CrmebException("请上传JSON文件");
        }
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            ProductImportFileRequest request = JSON.parseObject(json, ProductImportFileRequest.class);
            if (request == null) {
                throw new CrmebException("JSON文件内容不能为空");
            }
            return request;
        } catch (JSONException e) {
            throw new CrmebException("JSON格式不正确");
        } catch (IOException e) {
            throw new CrmebException("读取JSON文件失败");
        }
    }

    private void validateProductCount(List<ProductImportItemRequest> products) {
        if (CollUtil.isEmpty(products)) {
            throw new CrmebException("商品列表不能为空");
        }
        if (products.size() > MAX_PRODUCTS_PER_FILE) {
            throw new CrmebException("单次最多导入" + MAX_PRODUCTS_PER_FILE + "个商品");
        }
    }

    private void validateProductBase(ProductImportItemRequest item) {
        requireText(item.getStoreName(), "商品名称不能为空");
        requireText(item.getKeyword(), "关键字不能为空");
        requireText(item.getUnitName(), "单位不能为空");
        requireText(item.getImage(), "商品主图不能为空");
        if (CollUtil.isEmpty(item.getSliderImages())) {
            throw new CrmebException("轮播图不能为空");
        }
        if (CollUtil.isEmpty(item.getSkus())) {
            throw new CrmebException("SKU列表不能为空");
        }
        if (item.getCategoryId() == null && StrUtil.isBlank(item.getCategoryName())) {
            throw new CrmebException("商品分类不能为空");
        }
    }

    private Integer resolveCategoryId(ProductImportItemRequest item, boolean dryRun) {
        if (item.getCategoryId() != null) {
            Category category = categoryService.getById(item.getCategoryId());
            if (category == null || category.getType() == null || category.getType() != 1) {
                throw new CrmebException("商品分类不存在");
            }
            if (Boolean.FALSE.equals(category.getStatus())) {
                throw new CrmebException("商品分类已禁用");
            }
            return item.getCategoryId();
        }

        CategorySearchRequest searchRequest = new CategorySearchRequest();
        searchRequest.setType(1);
        searchRequest.setStatus(1);
        searchRequest.setName(item.getCategoryName().trim());
        List<Category> categories = categoryService.getList(searchRequest);
        for (Category category : categories) {
            if (item.getCategoryName().trim().equals(category.getName())) {
                return category.getId();
            }
        }
        if (dryRun) {
            return null;
        }
        return createRootCategory(item.getCategoryName().trim());
    }

    private Integer createRootCategory(String categoryName) {
        Category category = new Category();
        category.setPid(0);
        category.setPath(DEFAULT_CATEGORY_PATH);
        category.setName(categoryName);
        category.setType(1);
        category.setUrl("");
        category.setExtra("");
        category.setStatus(true);
        category.setSort(0);
        Date now = DateUtil.date();
        category.setCreateTime(now);
        category.setUpdateTime(now);
        Boolean created = categoryService.save(category);
        if (!Boolean.TRUE.equals(created) || category.getId() == null) {
            throw new CrmebException("创建商品分类失败");
        }
        return category.getId();
    }

    private Integer resolveShippingTemplateId(Integer tempId, boolean dryRun) {
        if (tempId != null) {
            ShippingTemplates template = shippingTemplatesService.getById(tempId);
            if (template == null) {
                throw new CrmebException("运费模板不存在");
            }
            return tempId;
        }
        List<ShippingTemplates> templates = shippingTemplatesService.list();
        if (CollUtil.isNotEmpty(templates)) {
            return templates.get(0).getId();
        }
        if (dryRun) {
            return null;
        }
        return createDefaultShippingTemplate();
    }

    private Integer createDefaultShippingTemplate() {
        ShippingTemplates template = new ShippingTemplates();
        template.setName(DEFAULT_SHIPPING_TEMPLATE_NAME);
        template.setType(1);
        template.setAppoint(0);
        template.setSort(0);
        Date now = DateUtil.date();
        template.setCreateTime(now);
        template.setUpdateTime(now);
        Boolean created = shippingTemplatesService.save(template);
        if (!Boolean.TRUE.equals(created) || template.getId() == null) {
            throw new CrmebException("创建默认运费模板失败");
        }
        return template.getId();
    }

    private StoreProductAddRequest buildSaveRequest(ProductImportItemRequest item, Integer categoryId, Integer tempId) {
        List<ProductImportSkuRequest> skus = normalizeSkus(item.getSkus());
        List<String> specNames = getSpecNames(skus);

        StoreProductAddRequest request = new StoreProductAddRequest();
        request.setImage(item.getImage().trim());
        request.setSliderImage(JSON.toJSONString(item.getSliderImages()));
        request.setStoreName(item.getStoreName().trim());
        request.setKeyword(item.getKeyword().trim());
        request.setCateId(String.valueOf(categoryId));
        request.setUnitName(item.getUnitName().trim());
        request.setSort(defaultInteger(item.getSort()));
        request.setIsHot(defaultBoolean(item.getIsHot()));
        request.setIsBenefit(defaultBoolean(item.getIsBenefit()));
        request.setIsBest(defaultBoolean(item.getIsBest()));
        request.setIsNew(defaultBoolean(item.getIsNew()));
        request.setIsGood(defaultBoolean(item.getIsGood()));
        request.setGiveIntegral(defaultInteger(item.getGiveIntegral()));
        request.setIsSub(false);
        request.setFicti(defaultInteger(item.getFicti()));
        request.setTempId(tempId);
        request.setSpecType(skus.size() > 1 || specNames.size() > 1);
        request.setAttr(buildAttrRequests(skus, specNames));
        request.setAttrValue(buildAttrValueRequests(item, skus, specNames));
        request.setContent(item.getContent() == null ? "" : item.getContent());
        request.setCouponIds(new ArrayList<>());
        return request;
    }

    private List<ProductImportSkuRequest> normalizeSkus(List<ProductImportSkuRequest> skus) {
        List<ProductImportSkuRequest> normalizedSkus = new ArrayList<>();
        for (ProductImportSkuRequest sku : skus) {
            if (sku == null) {
                throw new CrmebException("SKU不能为空");
            }
            requireAmount(sku.getPrice(), "SKU售价不能为空");
            requireAmount(sku.getOtPrice(), "SKU原价不能为空");
            requireAmount(sku.getCost(), "SKU成本价不能为空");
            if (sku.getStock() == null || sku.getStock() < 0) {
                throw new CrmebException("SKU库存不能为空且不能小于0");
            }
            if (sku.getSpecs() == null || sku.getSpecs().isEmpty()) {
                LinkedHashMap<String, String> defaultSpecs = new LinkedHashMap<>();
                defaultSpecs.put(DEFAULT_SPEC_NAME, DEFAULT_SPEC_VALUE);
                sku.setSpecs(defaultSpecs);
            }
            normalizedSkus.add(sku);
        }
        return normalizedSkus;
    }

    private List<String> getSpecNames(List<ProductImportSkuRequest> skus) {
        ProductImportSkuRequest firstSku = skus.get(0);
        List<String> specNames = new ArrayList<>(firstSku.getSpecs().keySet());
        if (CollUtil.isEmpty(specNames)) {
            throw new CrmebException("SKU规格不能为空");
        }
        for (String specName : specNames) {
            requireText(specName, "SKU规格名不能为空");
        }
        for (ProductImportSkuRequest sku : skus) {
            if (!sku.getSpecs().keySet().equals(firstSku.getSpecs().keySet())) {
                throw new CrmebException("所有SKU规格名必须一致");
            }
            for (String specName : specNames) {
                requireText(sku.getSpecs().get(specName), "SKU规格值不能为空");
            }
        }
        return specNames;
    }

    private List<StoreProductAttrAddRequest> buildAttrRequests(List<ProductImportSkuRequest> skus, List<String> specNames) {
        Map<String, Set<String>> valuesByName = new LinkedHashMap<>();
        for (String specName : specNames) {
            valuesByName.put(specName, new LinkedHashSet<>());
        }
        for (ProductImportSkuRequest sku : skus) {
            for (String specName : specNames) {
                valuesByName.get(specName).add(sku.getSpecs().get(specName).trim());
            }
        }

        List<StoreProductAttrAddRequest> attrs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : valuesByName.entrySet()) {
            StoreProductAttrAddRequest attr = new StoreProductAttrAddRequest();
            attr.setAttrName(entry.getKey().trim());
            attr.setAttrValues(String.join(",", entry.getValue()));
            attrs.add(attr);
        }
        return attrs;
    }

    private List<StoreProductAttrValueAddRequest> buildAttrValueRequests(ProductImportItemRequest item,
                                                                          List<ProductImportSkuRequest> skus,
                                                                          List<String> specNames) {
        List<StoreProductAttrValueAddRequest> attrValues = new ArrayList<>();
        for (ProductImportSkuRequest sku : skus) {
            LinkedHashMap<String, String> orderedSpecs = new LinkedHashMap<>();
            for (String specName : specNames) {
                orderedSpecs.put(specName.trim(), sku.getSpecs().get(specName).trim());
            }

            StoreProductAttrValueAddRequest attrValue = new StoreProductAttrValueAddRequest();
            attrValue.setProductId(0);
            attrValue.setStock(sku.getStock());
            attrValue.setPrice(sku.getPrice());
            attrValue.setImage(StrUtil.isBlank(sku.getImage()) ? item.getImage().trim() : sku.getImage().trim());
            attrValue.setCost(sku.getCost());
            attrValue.setOtPrice(sku.getOtPrice());
            attrValue.setWeight(defaultBigDecimal(sku.getWeight()));
            attrValue.setVolume(defaultBigDecimal(sku.getVolume()));
            attrValue.setBrokerage(defaultBigDecimal(sku.getBrokerage()));
            attrValue.setBrokerageTwo(defaultBigDecimal(sku.getBrokerageTwo()));
            attrValue.setAttrValue(JSON.toJSONString(orderedSpecs));
            attrValue.setBarCode(StrUtil.isBlank(sku.getBarCode()) ? "" : sku.getBarCode().trim());
            attrValues.add(attrValue);
        }
        return attrValues;
    }

    private void requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new CrmebException(message);
        }
    }

    private void requireAmount(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new CrmebException(message);
        }
    }

    private Boolean defaultBoolean(Boolean value) {
        return value == null ? false : value;
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeMessage(Exception e) {
        if (e instanceof CrmebException) {
            return e.getMessage();
        }
        return e.getMessage() == null ? "导入失败" : e.getMessage();
    }
}
