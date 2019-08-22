package com.server.fileServer.httpAction;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.fileServer.envManage.switchEnv;


public class HttpAction {
	
	private static String baseUrl = "https://openapi-centretest4.bicai365.com";
	
	static String cookieStr = "";
	
	static JSONObject cookieJson = new JSONObject();
	
    public static Map<String,Object> uploadFileByHTTP(String postUrl,Map<String,String> postParam) throws IOException{
    	
        Logger log = LoggerFactory.getLogger(HttpAction.class);

        Map<String,Object> resultMap = new HashMap<String,Object>();
        
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        
        
        try{
            //把一个普通参数和文件上传给下面这个地址    是一个servlet
            HttpPost httpPost = new HttpPost( baseUrl + postUrl );
            
            httpPost.setHeader("Cookie",cookieStr);
//          Content-Type: application/x-www-form-urlencoded; charset=UTF-8
            
            //把文件转换成流对象FileBody
//            FileBody fundFileBin = new FileBody(postFile);
            //设置传输参数
            MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
//            multipartEntity.addPart(postFile.getName(), fundFileBin);//相当于<input type="file" name="media"/>
            //设计文件以外的参数
            Set<String> keySet = postParam.keySet();
            for (String key : keySet) {
                //相当于<input type="text" name="name" value=name>
                multipartEntity.addPart(key, new StringBody(postParam.get(key), ContentType.create("text/plain", Consts.UTF_8)));
            }

            HttpEntity reqEntity =  multipartEntity.build();
            httpPost.setEntity(reqEntity);

            log.info("发起请求的页面地址 " + httpPost.getRequestLine());
           
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {

                //log.info(response.getStatusLine());
                resultMap.put("statusCode", response.getStatusLine().getStatusCode());
                
                
                if( Pattern.matches(".*userLogin*.", postUrl) ) {
                	
                	
                	HttpAction.upLoadCookie( cookieStore.getCookies() );
                	
                    resultMap.put("cookie", cookieStr);
                	
                }

                
                Header[] list  = response.getHeaders("set-cookie");
         
                HttpEntity resEntity = response.getEntity();
                
                if (resEntity != null) {
                   
                    log.info("Response content length: " + resEntity.getContentLength());
                    
                    resultMap.put("data", EntityUtils.toString(resEntity,Charset.forName("UTF-8")));
                }
            
                EntityUtils.consume(resEntity);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally{
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("uploadFileByHTTP result:"+resultMap);
        return resultMap;
    }
    
    
	public static JSONObject httpGet( String getUrl ) throws IOException {
		
		// 获得Http客户端(可以理解为:你得先有一个浏览器;注意:实际上HttpClient与浏览器是不一样的)
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		
		JSONObject resultJson = null;
 
		// 创建Get请求
		HttpGet httpGet = new HttpGet( baseUrl + getUrl );
		// 响应模型
		httpGet.setHeader("Cookie",cookieStr);
		CloseableHttpResponse response = null;
		try {
			// 配置信息
			RequestConfig requestConfig = RequestConfig.custom()
					// 设置连接超时时间(单位毫秒)
					.setConnectTimeout(5000)
					// 设置请求超时时间(单位毫秒)
					.setConnectionRequestTimeout(5000)
					// socket读写超时时间(单位毫秒)
					.setSocketTimeout(5000)
					// 设置是否允许重定向(默认为true)
					.setRedirectsEnabled(true).build();
 
			// 将上面的配置信息 运用到这个Get请求里
			httpGet.setConfig(requestConfig);
 
			// 由客户端执行(发送)Get请求
			response = httpClient.execute(httpGet);
 
			// 从响应模型中获取响应实体
			HttpEntity responseEntity = response.getEntity();
			System.out.println( "响应状态为:" + response.getStatusLine().getStatusCode() );
			if (responseEntity != null) {
				System.out.println("响应内容长度为:" + responseEntity.getContentLength());	
				String responseEntityStr = EntityUtils.toString(responseEntity);
				resultJson = new JSONObject( responseEntityStr );
				System.out.println("响应内容为:" + resultJson);
			}
			EntityUtils.consume( responseEntity );
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// 释放资源
				if (httpClient != null) {
					httpClient.close();
				}
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return resultJson;
	}
	
	//  每一次第二遍请求都只能返回两个cookie... = =，我也摸不到头脑；
	private static void upLoadCookie( List<Cookie> cookies ) {
		
        String cookieStrTemporary = "";
        
        for (int i = 0; i < cookies.size(); i++) {
        	
        	cookieJson.put( cookies.get(i).getName(),cookies.get(i).getValue() );
        	
        }
      
        for(String str:cookieJson.keySet()){

        	cookieStrTemporary += str + "=" + cookieJson.get(str) + ";";

    	}
        
        cookieStr = cookieStrTemporary;
		
	}
	
	public static  Map<String,String> getParam( String url ) {
        int index = url.indexOf("?");
        String param = url.substring(index+1);

        String[] params = param.split("&");

        Map<String,String> map = new HashMap<>();

        for (String item:params) {
            String[] kv = item.split("=");
            map.put(kv[0],kv[1]);
        }
        
        return map;
	}
    
}