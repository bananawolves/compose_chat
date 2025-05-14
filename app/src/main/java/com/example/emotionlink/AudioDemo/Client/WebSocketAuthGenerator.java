package com.example.emotionlink.AudioDemo.Client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
//鉴权生成器
public class WebSocketAuthGenerator {

    // 将字节数组转换为十六进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // 生成 HMAC-SHA256 签名的十六进制字符串
    public static String hmacSha256Hex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }

    // 生成 Authorization 字符串
    public static String generateAuthorization(String xAppId, String xAppKey, String region, String url,
                                               int expirationSeconds, String originName) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String authStringPrefix = String.format("%s/%s/%s/%d/%d", originName, xAppId, region, timestamp, expirationSeconds);

        // 解析 URL 并提取路径
        URI uri = new URI(url);
        String canonicalUri = uri.getPath() != null && !uri.getPath().isEmpty() ? URLEncoder.encode(uri.getPath(), "UTF-8") : "/";

        // 构建 CanonicalRequest
        String httpMethod = "GET";
        String canonicalQueryString = ""; // WebSocket 无查询参数
        String canonicalHeaders = "x-app-id:" + xAppId;
        String signedHeaders = "x-app-id";

        String canonicalRequest = httpMethod + "\n" +
                canonicalUri + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders;

        // 生成签名 key 和 signature
        String signingKey = hmacSha256Hex(authStringPrefix, xAppKey);
        String signature = hmacSha256Hex(canonicalRequest, signingKey);

        // 构建最终 Authorization 字符串
        return String.format("%s/%s/%s/%d/%d/%s/%s",
                originName, xAppId, region, timestamp, expirationSeconds, signedHeaders, signature);
    }

    // 示例调用
//    public static void main(String[] args) {
//        try {
//            String xAppId = "d0a488df365749648010ec85133e6273";
//            String xAppKey = "40c27006325c480a804c4162c9234b60";
//            String region = "SH";
//            String url = "wss://openapi.teleagi.cn:443/aipaas/voice/v1/asr/fy";
//            String originName = "teleai-cloud-auth-v1";
//            int expiration = 1800;
//
//            String authorization = generateAuthorization(xAppId, xAppKey, region, url, expiration, originName);
//            System.out.println("Generated Authorization: " + authorization);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
