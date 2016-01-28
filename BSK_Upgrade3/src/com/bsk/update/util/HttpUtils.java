package com.bsk.update.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.bsk.update1.Constants;

public class HttpUtils {
	
	private static URL url;
	private static HttpURLConnection conn;
	
	public HttpUtils() {
		
	}
	
	public static String doGet(String url_path) {
		
		try {
			url = new URL(url_path);
			conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(8000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			int code = conn.getResponseCode();
			if(code == 200) {
				return convertStreamToString(conn.getInputStream());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}
		return Constants.VISIT_SERVER_ERROR;
	}
	
	public static String doPost(String url_path, Map<String,String> params) {
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url_path);
		
		ArrayList<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
		if(params != null) {
			Set<String> keys = params.keySet();
			for(Iterator<String> i = keys.iterator();i.hasNext();) {
				String key = (String)i.next();
				pairs.add(new BasicNameValuePair(key,params.get(key)));
			}
		}
		
		try {
			UrlEncodedFormEntity p_entity = new UrlEncodedFormEntity(pairs,"utf-8");
			
			httpPost.setEntity(p_entity);
			
			HttpResponse response = client.execute(httpPost);
			
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				return convertStreamToString(content);
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	
	
	/**
	 * 根据URL访问服务器返回InputStream
	 * @return
	 */
	public static InputStream getInputStream(String url) {
		InputStream is = null;
		DefaultHttpClient httpclient = null;
		HttpGet httpget = null;
		HttpResponse response = null;
		HttpEntity entity = null;
		
		try {
			httpclient = new DefaultHttpClient();
	        httpget = new HttpGet(url);
			response = httpclient.execute(httpget);
			entity = response.getEntity();
			is = entity.getContent();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}

	private static String convertStreamToString(InputStream is) {
		String jsonString = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int len = 0;
		byte[] data = new byte[1024];
		try {
			while((len = is.read(data)) != -1) {
				baos.write(data, 0, len);
			}
			jsonString = new String(baos.toByteArray());
		}catch(IOException e) {
			
		}
		
//		Log.i("", "jsonString= " + jsonString);
		return jsonString;
	}
}
