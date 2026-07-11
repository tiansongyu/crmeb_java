package com.zbkj.service.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.result.CommonResultCode;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.QRCodeUtil;
import com.zbkj.common.vo.QrCodeVo;
import com.zbkj.service.service.QrCodeService;
import com.zbkj.service.service.WechatNewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
*  QrCodeServiceImpl 接口实现
*  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
*/
@Service
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private static final int HTTP_TIMEOUT_MILLIS = 5000;
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    @Autowired
    private WechatNewService wechatNewService;

    /**
     * 二维码
     *
     * @return QrCodeVo
     */
    @Override
    public QrCodeVo getWecahtQrCode(JSONObject data) {
//        StringBuilder scene = new StringBuilder();
//        String page = "";
//        try {
//            if (ObjectUtil.isNotNull(data)) {
//                Map<Object, Object> dataMap = JSONObject.toJavaObject(data, Map.class);
//
//                for (Map.Entry<Object, Object> m : dataMap.entrySet()) {
//                    if (m.getKey().equals("path")) {
//                        //前端路由， 不需要拼参数
//                        page = m.getValue().toString();
//                        continue;
//                    }
//                    if (scene.length() > 0) {
//                        scene.append(",");
//                    }
//                    scene.append(m.getKey()).append(":").append(m.getValue());
//                }
//            }
//        } catch (Exception e) {
//            throw new CrmebException("url参数错误 " + e.getMessage());
//        }
        if (ObjectUtil.isNull(data) || data.isEmpty())
            throw new CrmebException(CommonResultCode.VALIDATE_FAILED, "生成微信参数不能为空");
        QrCodeVo vo = new QrCodeVo();
        vo.setCode(wechatNewService.createQrCode(data));
        return vo;
    }

    /**
     * 远程图片转base64
     *
     * @param url 图片链接地址
     * @return QrCodeVo
     */
    @Override
    public QrCodeVo urlToBase64(String url) {
        String base64Image = toBase64(downloadPublicImage(url));
        QrCodeVo vo = new QrCodeVo();
        vo.setCode(base64Image);
        return vo;
    }

    /**
     * 字符串转base64
     *
     * @param text 待转换字符串
     * @return QrCodeVo base64格式
     */
    @Override
    public QrCodeVo strToBase64(String text, Integer width, Integer height) {
        validateQrCodeParams(text, width, height);
        String base64Image;
        try {
            base64Image = QRCodeUtil.crateQRCode(text, width, height);
        } catch (Exception e) {
            throw new CrmebException("生成二维码异常");
        }
        QrCodeVo vo = new QrCodeVo();
        vo.setCode(base64Image);
        return vo;
    }

    /**
     * 二维码
     * @return Object
     */
    @Override
    public Map<String, Object> get(JSONObject data) {
        Map<String, Object> map = new HashMap<>();
        StringBuilder scene = new StringBuilder();
        String page = "";
        try{
            if(null != data){
                Map<Object, Object> dataMap = JSONObject.toJavaObject(data, Map.class);

                for (Map.Entry<Object, Object> m : dataMap.entrySet()) {
                    if(m.getKey().equals("path")){
                        //前端路由， 不需要拼参数
                        page = m.getValue().toString();
                        continue;
                    }
                    if (scene.length() > 0) {
                        scene.append(",");
                    }
                    scene.append(m.getKey()).append(":").append(m.getValue());
                }
            }
        } catch (Exception e) {
            log.warn("解析二维码参数失败", e);
            throw new CrmebException("url参数错误");
        }
        map.put("code", wechatNewService.createQrCode(page, scene.length() > 0 ? scene.toString() : ""));
        return map;
    }

    @Override
    public Map<String, Object> base64(String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", toBase64(downloadPublicImage(url)));
        return map;
    }

    /**
     * 讲字符串转为QRcode
     * @param text 待转换字符串
     * @return QRcode base64格式
     */
    @Override
    public Map<String, Object> base64String(String text,int width, int height) {
        validateQrCodeParams(text, width, height);
        String base64Image;
        try {
            base64Image = QRCodeUtil.crateQRCode(text,width,height);
        } catch (Exception e) {
            throw new CrmebException("生成二维码异常");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("code", base64Image);
        return map;
    }

    private void validateQrCodeParams(String text, Integer width, Integer height) {
        if (StrUtil.isBlank(text) || width == null || height == null
                || width < 50 || width > 500 || height < 50 || height > 500 || text.length() >= 999) {
            throw new CrmebException(CommonResultCode.VALIDATE_FAILED, "生成二维码参数不合法");
        }
    }

    private String toBase64(byte[] bytes) {
        return CrmebUtil.getBase64Image(Base64.encodeBase64String(bytes));
    }

    private byte[] downloadPublicImage(String sourceUrl) {
        URI current = parsePublicUri(sourceUrl);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current.toASCIIString()).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
                connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "image/*");

                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (redirects == MAX_REDIRECTS || StrUtil.isBlank(location)) {
                        throw new CrmebException("远程图片重定向异常");
                    }
                    current = parsePublicUri(current.resolve(location).toString());
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new CrmebException("远程图片下载失败");
                }
                String contentType = connection.getContentType();
                if (StrUtil.isBlank(contentType) || !contentType.toLowerCase().startsWith("image/")) {
                    throw new CrmebException("远程地址不是图片");
                }
                int contentLength = connection.getContentLength();
                if (contentLength > MAX_IMAGE_BYTES) {
                    throw new CrmebException("远程图片不能超过5MB");
                }
                try (InputStream input = connection.getInputStream()) {
                    return readLimited(input);
                }
            } catch (CrmebException e) {
                throw e;
            } catch (IOException e) {
                log.warn("远程图片下载失败", e);
                throw new CrmebException("远程图片下载失败");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw new CrmebException("远程图片重定向异常");
    }

    private byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMAGE_BYTES) {
                throw new CrmebException("远程图片不能超过5MB");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private URI parsePublicUri(String sourceUrl) {
        if (StrUtil.isBlank(sourceUrl)) {
            throw new CrmebException("图片地址无效");
        }
        try {
            URI uri = URI.create(sourceUrl);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new CrmebException("仅支持公网HTTP(S)图片地址");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublicAddress(address)) {
                    throw new CrmebException("不允许访问内网图片地址");
                }
            }
            return uri;
        } catch (CrmebException e) {
            throw e;
        } catch (IllegalArgumentException | UnknownHostException e) {
            throw new CrmebException("图片地址无效");
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return (bytes[0] & 0xfe) != 0xfc;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first != 0 && first != 127 && !(first == 100 && second >= 64 && second <= 127)
                && !(first == 192 && second == 0) && !(first == 198 && (second == 18 || second == 19))
                && first < 224;
    }
}
