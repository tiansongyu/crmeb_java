package com.zbkj.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwk.Jwk;
import io.jsonwebtoken.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

/**
 * 苹果工具类
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
public class AppleUtil {

    private static final Logger logger = LoggerFactory.getLogger(AppleUtil.class);
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_KEYS_URL = APPLE_ISSUER + "/auth/keys";
    private static final long KEY_CACHE_MILLIS = 60 * 60 * 1000L;
    private static final RestTemplate REST_TEMPLATE = createRestTemplate();
    private static volatile JSONArray cachedKeys;
    private static volatile long keysExpireAt;

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    /**
     * 获取苹果的公钥
     * @return
     */
    private static synchronized JSONArray getAuthKeys() {
        long now = System.currentTimeMillis();
        if (cachedKeys != null && now < keysExpireAt) {
            return cachedKeys;
        }
        try {
            JSONObject json = REST_TEMPLATE.getForObject(APPLE_KEYS_URL, JSONObject.class);
            JSONArray keys = ObjectUtil.isNull(json) ? null : json.getJSONArray("keys");
            if (keys == null || keys.isEmpty()) {
                logger.warn("获取苹果公钥失败：响应中没有密钥");
                return null;
            }
            cachedKeys = keys;
            keysExpireAt = now + KEY_CACHE_MILLIS;
            return keys;
        } catch (Exception e) {
            logger.warn("获取苹果公钥失败", e);
            return cachedKeys;
        }
    }

    public static Boolean verify(String jwt) {
        return verify(jwt, null);
    }

    /**
     * 校验苹果 identity token，并可强制校验客户端 audience（通常是 iOS Bundle ID）。
     */
    public static Boolean verify(String jwt, String expectedAudience) {
        JSONObject header = parseJwtPart(jwt, 0);
        if (header == null || StrUtil.isBlank(header.getString("kid"))) {
            return false;
        }
        JSONArray arr = getAuthKeys();
        if (arr == null || arr.isEmpty()) {
            return false;
        }
        String keyId = header.getString("kid");
        for (int i = 0; i < arr.size(); i++) {
            JSONObject authKey = arr.getJSONObject(i);
            if (authKey != null && keyId.equals(authKey.getString("kid")) && verifyExc(jwt, authKey, expectedAudience)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对前端传来的identityToken进行验证
     * @param jwt 对应前端传来的 identityToken
     * @param authKey 苹果的公钥 authKey
     * @return
     * @throws Exception
     */
    public static Boolean verifyExc(String jwt, JSONObject authKey) {
        return verifyExc(jwt, authKey, null);
    }

    private static Boolean verifyExc(String jwt, JSONObject authKey, String expectedAudience) {
        try {
            Jwk jwk = Jwk.fromValues(authKey);
            PublicKey publicKey = jwk.getPublicKey();
            JwtParser jwtParser = Jwts.parser().setSigningKey(publicKey).requireIssuer(APPLE_ISSUER);
            if (StrUtil.isNotBlank(expectedAudience)) {
                jwtParser.requireAudience(expectedAudience);
            }
            Jws<Claims> token = jwtParser.parseClaimsJws(jwt);
            Claims claims = token.getBody();
            return claims.containsKey("auth_time") && StrUtil.isNotBlank(claims.getSubject())
                    && StrUtil.isNotBlank(claims.getAudience());
        } catch (ExpiredJwtException e) {
            logger.debug("Apple identityToken已过期");
            return false;
        } catch (Exception e) {
            logger.debug("Apple identityToken校验失败", e);
            return false;
        }
    }

    /**
     * 对前端传来的JWT字符串identityToken的第二部分进行解码
     * 主要获取其中的aud和sub，aud大概对应ios前端的包名，sub大概对应当前用户的授权的openID
     * @param identityToken iosToken
     * @return  {"aud":"com.xkj.****","sub":"000***.8da764d3f9e34d2183e8da08a1057***.0***","c_hash":"UsKAuEoI-****","email_verified":"true","auth_time":1574673481,"iss":"https://appleid.apple.com","exp":1574674081,"iat":1574673481,"email":"****@qq.com"}
     */
    public static JSONObject parserIdentityToken(String identityToken){
        return parseJwtPart(identityToken, 1);
    }

    private static JSONObject parseJwtPart(String identityToken, int index) {
        if (StrUtil.isBlank(identityToken)) {
            return null;
        }
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            String decoded = new String(Base64.decodeBase64(parts[index]), StandardCharsets.UTF_8);
            return JSON.parseObject(decoded);
        } catch (Exception e) {
            return null;
        }
    }
}
