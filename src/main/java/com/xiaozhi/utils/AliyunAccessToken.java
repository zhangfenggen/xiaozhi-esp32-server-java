package com.xiaozhi.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.alibaba.fastjson.JSONObject;

public class AliyunAccessToken {

  /**
   * 创建阿里云NLS服务的访问Token
   * 
   * @param accessKeyId     访问密钥ID
   * @param accessKeySecret 访问密钥Secret
   * @return 包含token和过期时间的Map，如果获取失败则返回null
   */
  public static Map<String, String> createToken(String accessKeyId, String accessKeySecret) {
    try {
      // 构建参数映射
      Map<String, String> parameters = new HashMap<>();
      parameters.put("AccessKeyId", accessKeyId);
      parameters.put("Action", "CreateToken");
      parameters.put("Format", "JSON");
      parameters.put("RegionId", "cn-shanghai");
      parameters.put("SignatureMethod", "HMAC-SHA1");
      parameters.put("SignatureNonce", UUID.randomUUID().toString());
      parameters.put("SignatureVersion", "1.0");

      // 设置GMT时间格式
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      parameters.put("Timestamp", df.format(new Date()));
      parameters.put("Version", "2019-02-28");

      // 构造规范化的请求字符串
      String queryString = encodeDict(parameters);

      // 构造待签名字符串
      String stringToSign = "GET" + "&" + encodeText("/") + "&" + encodeText(queryString);

      // 计算签名
      String signature = calculateSignature(accessKeySecret + "&", stringToSign);

      // 进行URL编码
      signature = encodeText(signature);

      // 构建完整URL
      String fullUrl = "http://nls-meta.cn-shanghai.aliyuncs.com/?Signature=" + signature + "&" + queryString;

      // 发送HTTP GET请求
      URL url = new URL(fullUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      // 读取响应
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        // 解析JSON响应，使用阿里巴巴FastJSON
        JSONObject jsonResponse = JSONObject.parseObject(response.toString());
        if (jsonResponse.containsKey("Token")) {
          JSONObject tokenObj = jsonResponse.getJSONObject("Token");
          String token = tokenObj.getString("Id");
          String expireTime = tokenObj.getString("ExpireTime");

          Map<String, String> result = new HashMap<>();
          result.put("token", token);
          result.put("expireTime", expireTime);
          return result;
        }
      }

      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 对文本进行URL编码
   * 
   * @param text 要编码的文本
   * @return 编码后的文本
   */
  private static String encodeText(String text) {
    try {
      String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
      return encoded.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 对字典进行URL编码
   * 
   * @param dict 要编码的字典
   * @return 编码后的查询字符串
   */
  private static String encodeDict(Map<String, String> dict) {
    List<String> keys = new ArrayList<>(dict.keySet());
    Collections.sort(keys);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      String value = dict.get(key);

      if (i > 0) {
        sb.append("&");
      }

      sb.append(encodeText(key)).append("=").append(encodeText(value));
    }

    return sb.toString();
  }

  /**
   * 计算HMAC-SHA1签名
   * 
   * @param key  密钥
   * @param data 待签名数据
   * @return Base64编码的签名
   */
  private static String calculateSignature(String key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}