package com.binfun.update.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtil {
    /**从网络上获取JSON数据*/
//	public final static <T> T getNetJson(String url, Class<T> clazz){
//		String text = getNetText4Https(url);
//		if(text == null) return null;
//		
//		T data = null;
//		try{
//			data = JSON.parseObject(text, clazz);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//		return data;
//	}

    /**
     * 从网络上读取一个字符串
     */
    public final static String getNetText(String url) {
        URLConnection connection = null;
        BufferedReader reader = null;
        String line = null;
        StringBuffer stringBuffer = new StringBuffer();

        // 下载源
        try {
            URL urlInfo = new URL(url);
            connection = urlInfo.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
//            connection.setRequestProperty("User-Agent", ApiUrlFactory.USER_AGENT);
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 处理源
        String info = stringBuffer.toString();
        return info;
    }

    public final static String getNetText4Https(String httpsUrl) {
        BufferedReader reader = null;
        HttpsURLConnection conn = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());
            conn = (HttpsURLConnection) new URL(httpsUrl).openConnection();

            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.connect();
            if (httpsUrl.contains("sysinfo")) {
                Map<String, List<String>> map = conn.getHeaderFields();
                List<String> server = map.get("Date");
                Date time = new Date(server.get(0));
//                Api.diffTime = time.getTime() - System.currentTimeMillis();
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            // 处理源
            String info = sb.toString();
            sb.setLength(0);
//             System.out.println("数据:" + info);

            return info;
        } catch (Exception e) {
             e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null)
                    conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static class MyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static class MyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static boolean submit4Get(String url) {
        boolean result = false;
        try {
            URL urlInfo = new URL(url);
            urlInfo.openConnection();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * POST提交
     */
    public static boolean submit4Post(String url, String data, String charsetName) {
        boolean result = false;
        OutputStreamWriter out = null;
        HttpURLConnection connection = null;
        try {
            URL urlInfo = new URL(url);
            connection = (HttpURLConnection) urlInfo.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            out = new OutputStreamWriter(connection.getOutputStream(), charsetName);
            out.write(data);
            out.flush();
            out.close();
            out = null;
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(connection != null){
                connection.disconnect();
            }
        }

        return result;
    }

}
