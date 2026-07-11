package com.zbkj.common.utils;


import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.exception.CrmebException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * http通用工具类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Slf4j
public class UploadWeChatMediaUtil {
    /**
     * 把文件上传到指定url上去
     * @param url 上传地址
     * @param file 待上传文件
     */
    public static JSONObject uploadFile(String url, InputStream file, String fileName) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault(); InputStream media = file) {
            HttpPost httppost = new HttpPost(url);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .setSocketTimeout(30000)
                    .build();
            httppost.setConfig(requestConfig);

            // 直接流式上传，避免先把整个素材复制到内存。
            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("media", media, ContentType.DEFAULT_BINARY, fileName)
                    .build();

            httppost.setEntity(reqEntity);
            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 300) {
                    throw new CrmebException("上传微信素材失败");
                }
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    String responseEntityStr = EntityUtils.toString(resEntity, StandardCharsets.UTF_8);
                    return JSONObject.parseObject(responseEntityStr);
                }
            }
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            log.warn("上传微信素材失败", e);
            throw new CrmebException("上传微信素材失败");
        }

        return null;
    }
}
