package apps.netty.push.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class HttpUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static JsonObject httpRequest(String requestUrl, String requestMethod, String outputStr) {
        JsonObject jsonObject = null;

        try {
            String result = get(requestUrl, "UTF-8", 5000, 5000);
            jsonObject = new JsonParser().parse(result).getAsJsonObject();

        } catch (Exception e) {
            logger.error("https call exception:", e);
        }
        return jsonObject;
    }


    public static JsonObject httpGetRequest(String requestUrl) {

        JsonObject jsonObject = null;

        CloseableHttpClient httpClient = null;

        if (requestUrl.startsWith("https")) {
            httpClient = (CloseableHttpClient) wrapClient();
        } else {
            httpClient = HttpClients.createDefault();
        }

        HttpGet httpGet = new HttpGet(requestUrl);

        CloseableHttpResponse httpResponse = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(8000).setConnectTimeout(8000).build();

            httpGet.setConfig(requestConfig);

            httpResponse = httpClient.execute(httpGet);

            HttpEntity httpEntity = httpResponse.getEntity();

            String result = EntityUtils.toString(httpEntity, "UTF-8");


            jsonObject = new JsonParser().parse(result).getAsJsonObject();
        } catch (Exception e) {
            logger.error("http call exception:", e);
        } finally {
            try {
                httpClient.close();
                if (null != httpResponse) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                logger.error("close http exception:", e);
            }
        }

        return jsonObject;
    }

    private static HttpClient wrapClient() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] arg0,
                                               String arg1) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0,
                                               String arg1) throws CertificateException {
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(
                    ctx, NoopHostnameVerifier.INSTANCE);


            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(ssf).build();
            return httpclient;
        } catch (Exception e) {
            return HttpClients.createDefault();
        }
    }


    public static String get(String url, String charset, Integer connTimeout, Integer readTimeout) throws ConnectTimeoutException, SocketTimeoutException, Exception {

        HttpClient client = null;
        HttpGet get = new HttpGet(url);
        String result = "";
        try {
            // 设置参数  
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            get.setConfig(customReqConf.build());

            HttpResponse res = null;

            if (url.startsWith("https")) {
                // 执行 Https 请求.  
                client = (CloseableHttpClient) wrapClient();
                res = client.execute(get);
            } else {
                // 执行 Http 请求.  
                client = HttpClients.createDefault();
                res = client.execute(get);
            }

            result = IOUtils.toString(res.getEntity().getContent(), charset);
        } finally {
            get.releaseConnection();
            if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
        return result;
    }

    /**
     * 创建 SSL连接
     *
     * @return
     * @throws GeneralSecurityException
     */
    /*private static CloseableHttpClient createSSLInsecureClient() throws GeneralSecurityException {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                        public boolean isTrusted(X509Certificate[] chain,String authType) throws CertificateException {
                            return true;
                        }
                    }).build();
            
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                        @Override
                        public boolean verify(String arg0, SSLSession arg1) {
                            return true;
                        }

                        @Override
                        public void verify(String host, SSLSocket ssl)
                                throws IOException {
                        }

                        @Override
                        public void verify(String host, X509Certificate cert)
                                throws SSLException {
                        }

                        @Override
                        public void verify(String host, String[] cns,
                                String[] subjectAlts) throws SSLException {
                        }

                    });
            
            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
            
        } catch (GeneralSecurityException e) {
            throw e;
        }
    }*/
    public static JsonObject httpPostRequest(String requestUrl, String jsonParam) {
        JsonObject jsonObject = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json;charset=UTF-8");

            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            String responseContent = null;

            if (requestUrl.startsWith("https")) {
            		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                    DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                    httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost);
            entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            logger.info(result);

            try {
                jsonObject = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("returnMessage", result);
            }
        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return jsonObject;
    }

    public static byte[] httpPostRequestWxQRcode(String requestUrl, String jsonParam) {
        byte[] result = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json;charset=UTF-8");

            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            String responseContent = null;

            if (requestUrl.startsWith("https")) {
            		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                    DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                    httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost);
            entity = response.getEntity();

            result = EntityUtils.toByteArray(entity);

        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return result;
    }


    public static JsonObject httpPostRequest(String requestUrl, String jsonParam, Map<String, String> headerMap) {
        JsonObject jsonObject = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

            if (null != headerMap && !headerMap.isEmpty()) {

                Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
                while (itor.hasNext()) {
                    Entry<String, String> next = itor.next();
                    httpPost.setHeader(next.getKey(), next.getValue());
                }
            }

            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json;charset=UTF-8");


            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            String responseContent = null;

            if (requestUrl.startsWith("https")) {
            		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                    DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                    httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost);

            entity = response.getEntity();

            String result = EntityUtils.toString(entity);
            logger.info(result);

            try {
                jsonObject = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("returnMessage", result);
            }
            jsonObject.addProperty("statusCode", response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return jsonObject;
    }

    public static JsonObject httpPostRequestFomoPos(String requestUrl, String jsonParam, Map<String, String> headerMap) {
        JsonObject returnJson = new JsonObject();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            if (null != headerMap && !headerMap.isEmpty()) {

                Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
                while (itor.hasNext()) {
                    Entry<String, String> next = itor.next();
                    httpPost.setHeader(next.getKey(), next.getValue());
                }
            }

            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json");

            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            String responseContent = null;

            if (requestUrl.startsWith("https")) {
            		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                    DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                    httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost);

            JsonObject responseHeader = new JsonObject();

            HeaderIterator itor = response.headerIterator();
            while (itor.hasNext()) {
                Header header = itor.nextHeader();
                if (header.getName().equals("X-Authentication-Version")
                        || header.getName().equals("X-Authentication-Method")
                        || header.getName().equals("X-Authentication-Nonce")
                        || header.getName().equals("X-Authentication-Timestamp")
                        || header.getName().equals("X-Authentication-Sign")
                        ) {
                    responseHeader.addProperty(header.getName(), header.getValue());
                }
            }

            entity = response.getEntity();

            String result = EntityUtils.toString(entity);

            logger.info("httpPostRequestFomoPos result:" + result);

            JsonObject payload = null;
            try {
                payload = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                payload = null;
                logger.error("exception while httpPostRequestFomo", e);
            }

            returnJson.add("responseHeader", responseHeader);
            returnJson.add("payload", payload);

        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return returnJson;
    }

    public static JsonObject httpPostRequestFomoWechatMp(String requestUrl, String jsonParam, Map<String, String> headerMap) {
        JsonObject returnJson = new JsonObject();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            if (null != headerMap && !headerMap.isEmpty()) {

                Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
                while (itor.hasNext()) {
                    Entry<String, String> next = itor.next();
                    httpPost.setHeader(next.getKey(), next.getValue());
                }
            }

            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json");

            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            if (requestUrl.startsWith("https")) {
        		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                //httpClient = createSSLInsecureClient();
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost);
            entity = response.getEntity();

            String result = EntityUtils.toString(entity);
            JsonObject payload = null;
            try {
                payload = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                payload = null;
                logger.error("exception while httpPostRequestFomo", e);
            }
            returnJson.add("payload", payload);
            returnJson.addProperty("statusCode", response.getStatusLine().getStatusCode());

        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return returnJson;
    }

    public static JsonObject httpGetRequestFomoWechatMp(String requestUrl, Map<String, String> headerMap) {
        JsonObject returnJson = new JsonObject();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(requestUrl);

            if (null != headerMap && !headerMap.isEmpty()) {

                Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
                while (itor.hasNext()) {
                    Entry<String, String> next = itor.next();
                    httpGet.setHeader(next.getKey(), next.getValue());
                }
            }
            HttpEntity entity = null;
            if (requestUrl.startsWith("https")) {
        		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpGet.setConfig(requestConfig);

            response = httpClient.execute(httpGet);
            entity = response.getEntity();

            String result = EntityUtils.toString(entity);
            JsonObject payload = null;
            try {
                payload = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                payload = null;
                logger.error("exception while httpPostRequestFomo", e);
            }
            returnJson.add("payload", payload);
            returnJson.addProperty("statusCode", response.getStatusLine().getStatusCode());

        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return returnJson;
    }


    public static JsonObject httpPostRequestBOC(String requestUrl, String jsonParam, String userName, String apiPassword) {
        JsonObject jsonObject = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;


        HttpClientContext httpClientContext = HttpClientContext.create();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        // Load credentials
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, apiPassword));
        httpClientContext.setCredentialsProvider(credentialsProvider);

        // Execute the request

        try {
            HttpPost httpPost = new HttpPost(requestUrl);

            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

            System.out.println("header begin...");
            for (Header header : httpPost.getAllHeaders()) {
                System.out.println(header);
            }
            System.out.println("header end...");

            StringEntity stringEntity = new StringEntity(jsonParam, "UTF-8");
            stringEntity.setContentType("application/json;charset=UTF-8");


            httpPost.setEntity(stringEntity);
            HttpEntity entity = null;
            String responseContent = null;

            if (requestUrl.startsWith("https")) {
            		/*PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.load(new URL(httpPost.getURI().toString()));  
                    DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);  
                    httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();*/
                httpClient = (CloseableHttpClient) wrapClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(15000)
                    .setConnectTimeout(15000)
                    .setConnectionRequestTimeout(15000)
                    .build();

            httpPost.setConfig(requestConfig);

            response = httpClient.execute(httpPost, httpClientContext);

            entity = response.getEntity();

            String result = EntityUtils.toString(entity);
            logger.info(result);

            try {
                jsonObject = new JsonParser().parse(result).getAsJsonObject();
            } catch (Exception e) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("returnMessage", result);
            }

        } catch (Exception e) {
            logger.error("exception", e);
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        return jsonObject;
    }


    public static JsonObject httpGetRequest(String requestUrl, Map<String, String> headerMap) {

        JsonObject jsonObject = null;

        CloseableHttpClient httpClient = null;

        if (requestUrl.startsWith("https")) {
            httpClient = (CloseableHttpClient) wrapClient();
        } else {
            httpClient = HttpClients.createDefault();
        }

        HttpGet httpGet = new HttpGet(requestUrl);

        if (null != headerMap && !headerMap.isEmpty()) {

            Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
            while (itor.hasNext()) {
                Entry<String, String> next = itor.next();
                httpGet.setHeader(next.getKey(), next.getValue());
            }
        }


        CloseableHttpResponse httpResponse = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(8000).setConnectTimeout(8000).build();

            httpGet.setConfig(requestConfig);

            httpResponse = httpClient.execute(httpGet);

            HttpEntity httpEntity = httpResponse.getEntity();

            String result = EntityUtils.toString(httpEntity, "UTF-8");


            if (StringUtils.isNotBlank(result)) {
                jsonObject = new JsonParser().parse(result).getAsJsonObject();
            }
        } catch (Exception e) {
            logger.error("http call exception:", e);
        } finally {
            try {
                httpClient.close();
                if (null != httpResponse) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                logger.error("close http exception:", e);
            }
        }

        return jsonObject;
    }

    public static JsonObject httpPutRequestBOC(String data, String requestUrl, String userName, String apiPassword) {

        JsonObject jsonObject = null;

        CloseableHttpClient httpClient = null;

        if (requestUrl.startsWith("https")) {
            httpClient = (CloseableHttpClient) wrapClient();
        } else {
            httpClient = HttpClients.createDefault();
        }

        HttpClientContext httpClientContext = HttpClientContext.create();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        // Load credentials
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, apiPassword));
        httpClientContext.setCredentialsProvider(credentialsProvider);

        HttpPut httpPut = new HttpPut(requestUrl);
        httpPut.setEntity(new StringEntity(data, "UTF-8"));

        CloseableHttpResponse httpResponse = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(8000).setConnectTimeout(8000).build();

            httpPut.setConfig(requestConfig);

            httpResponse = httpClient.execute(httpPut, httpClientContext);

            HttpEntity httpEntity = httpResponse.getEntity();

            String result = EntityUtils.toString(httpEntity, "UTF-8");

            if (StringUtils.isNotBlank(result)) {
                jsonObject = new JsonParser().parse(result).getAsJsonObject();
            }
        } catch (Exception e) {
            logger.error("http call exception:", e);
        } finally {
            try {
                httpClient.close();
                if (null != httpResponse) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                logger.error("close http exception:", e);
            }
        }

        return jsonObject;
    }

    public static JsonObject httpPutRequest(String requestUrl, Map<String, String> headerMap) {

        JsonObject jsonObject = null;

        CloseableHttpClient httpClient = null;

        if (requestUrl.startsWith("https")) {
            httpClient = (CloseableHttpClient) wrapClient();
        } else {
            httpClient = HttpClients.createDefault();
        }

        HttpPut httpPut = new HttpPut(requestUrl);

        if (null != headerMap && !headerMap.isEmpty()) {

            Iterator<Entry<String, String>> itor = headerMap.entrySet().iterator();
            while (itor.hasNext()) {
                Entry<String, String> next = itor.next();
                httpPut.setHeader(next.getKey(), next.getValue());
            }
        }


        CloseableHttpResponse httpResponse = null;
        try {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(8000).setConnectTimeout(8000).build();

            httpPut.setConfig(requestConfig);

            httpResponse = httpClient.execute(httpPut);

            HttpEntity httpEntity = httpResponse.getEntity();

            String result = EntityUtils.toString(httpEntity, "UTF-8");

            jsonObject = new JsonParser().parse(result).getAsJsonObject();
        } catch (Exception e) {
            logger.error("http call exception:", e);
        } finally {
            try {
                httpClient.close();
                if (null != httpResponse) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                logger.error("close http exception:", e);
            }
        }

        return jsonObject;
    }


    //	----------------------aliPay请求form表单提交--------------------------
    public static String aliPaySubmit(String url, Map<String, Object> params) {
        URL u = null;
        HttpURLConnection con = null;
        // 构建请求参数
        StringBuffer sb = new StringBuffer();
        if (params != null) {
            for (Entry<String, Object> e : params.entrySet()) {
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());
                sb.append("&");
            }
            sb.substring(0, sb.length() - 1);
        }
        System.out.println("send_url:" + url);
        System.out.println("send_data:" + sb.toString());
        // 尝试发送请求
        try {
            u = new URL(url);
            con = (HttpURLConnection) u.openConnection();
            //// POST 只能为大写，严格限制，post会不识别
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
            osw.write(sb.toString());
            osw.flush();
            osw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        // 读取返回内容
        StringBuffer buffer = new StringBuffer();
        try {
            //一定要有返回值，否则无法把请求发送给server端。
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String temp;
            while ((temp = br.readLine()) != null) {
                buffer.append(temp);
                buffer.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buffer.toString();
    }
}
