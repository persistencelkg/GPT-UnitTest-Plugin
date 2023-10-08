package org.lkg.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * @author: likaiguang
 */
public class HttpUtils {

    public static final int HTTP_CONN_TIMEOUT = 30_000;
    public static final int HTTP_SOCKET_TIMEOUT = 60_000;

    public static final int MAX_RETRY_COUNT = 2;
    public static CloseableHttpClient httpClient;

    static {
        try {
            httpClient = getHttpClientBuilder(HTTP_CONN_TIMEOUT, HTTP_SOCKET_TIMEOUT, MAX_RETRY_COUNT);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

    }
    private static CloseableHttpClient getHttpClientBuilder(int connectTimeOut, int sockTimeOut,
                                                          final int maxRetryCount) throws NoSuchAlgorithmException, KeyStoreException {
        final SSLConnectionSocketFactory sslsf;
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();

        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(100);
        httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .build();

//        HttpClientBuilder builder = HttpClientBuilder.create();
//        builder.setDefaultRequestConfig(RequestConfig.custom()
//                .setConnectTimeout(connectTimeOut)
//                .setSocketTimeout(sockTimeOut).build())
//                .setRetryHandler((exception, executionCount, context) -> {
//                    //最多重试Max_Retry_count次
//                    if (executionCount > maxRetryCount) {
//                        return false;
//                    }
//                    if (exception instanceof ConnectTimeoutException || exception instanceof SocketTimeoutException) {
//                        return true;
//                    }
//                    return false;
//                });
//        return builder;
        return httpClient;
    }

    /**
     * 发送 POST 请求（HTTP），K-V形式
     *
     * @param url    API接口URL
     * @param params 参数map
     */
    public static String doPost(String url, Map<String, Object> params) throws IOException {
        return doPost(url, params, new HashMap<>());
    }

    public static String doGet(String url, Map<String, Object> params) {
        return doGet(url, params, new HashMap<>());
    }
    /**
     * 发送 POST 请求（HTTP），K-V形式
     *
     * @param url    API接口URL
     * @param params 参数map
     */
    public static String doPost(String url, Map<String,  Object> params, Map<String, String> headers)
            throws IOException {
        String httpStr;
        HttpPost httpPost = new HttpPost(url);
        HttpResponse response = null;
        try {
//            List<NameValuePair> pairList = new ArrayList<>(params.size());
//            for (Map.Entry<String, ? extends Object> entry : params.entrySet()) {
//                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue().toString());
//                pairList.add(pair);
//            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }
//            httpPost.setEntity(new UrlEncodedFormEntity(pairList, StandardCharsets.UTF_8));
            httpPost.setEntity(new StringEntity(getUrl(url, params), StandardCharsets.UTF_8));
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }

    /**
     *
     * @param url
     * @param headers
     * @return
     */
    public static String doGet(String url, Map<String, Object> params, Map<String, String> headers) {
        String apiUrl = getUrl(url, params);
//        StopWatch watch = new StopWatch();
//        watch.start();
        HttpResponse response = null;
        String result = null;
        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            if (Objects.nonNull(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }
            response = httpClient.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (Objects.nonNull(entity)) {
                result = EntityUtils.toString(entity, StandardCharsets.UTF_8.toString());
            }
//            watch.stop();
//            log.info("third {} request finish, response code:{} time cost:{}", url, status, watch.getTime());
        } catch (IOException e) {
//            log.error(e.getMessage(), e);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;

    }

    @NotNull
    private static String getUrl(String url, Map<String, Object> params) {
        String apiUrl = url;
        StringBuffer param = new StringBuffer();
        if (Objects.nonNull(params) && !url.contains("?")) {
            int i = 0;
            for (String key : params.keySet()) {
                if (i == 0) {
                    param.append("?");
                } else {
                    param.append("&");
                }
                param.append(key).append("=").append(params.get(key));
                i++;
            }
        }
        apiUrl += param.toString();
        return apiUrl;
    }



//    public static void main(String[] args) {
//        System.out.println(doGet("https://api.apihubs.cn/holiday/get?cn=1&size=2&year=2022", null));
//    }
}
