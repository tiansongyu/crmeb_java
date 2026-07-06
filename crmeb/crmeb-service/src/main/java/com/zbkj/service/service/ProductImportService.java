package com.zbkj.service.service;

import com.zbkj.common.response.ProductImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImportService {

    ProductImportResponse importProducts(MultipartFile file, Boolean dryRun);
}
