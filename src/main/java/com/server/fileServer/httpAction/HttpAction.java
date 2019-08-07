package com.server.fileServer.httpAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpAction {
	
	static String CookieStr = "";
    public static Map<String,Object> uploadFileByHTTP(String postUrl,Map<String,String> postParam){
        Logger log = LoggerFactory.getLogger(HttpAction.class);

        Map<String,Object> resultMap = new HashMap<String,Object>();
        
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        
        
        try{
            //把一个普通参数和文件上传给下面这个地址    是一个servlet
            HttpPost httpPost = new HttpPost(postUrl);
            
            httpPost.setHeader("Cookie",CookieStr);
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
                log.info("----------------------------------------");

                //log.info(response.getStatusLine());
                resultMap.put("statusCode", response.getStatusLine().getStatusCode());
                
                
                if( Pattern.matches(".*userLogin*.", postUrl) ) {
                	
                    List<Cookie> cookies = cookieStore.getCookies();
                    String cookieStrTemporary = "";
                    for (int i = 0; i < cookies.size(); i++) {
                    	
                    	cookieStrTemporary += cookies.get(i).getName() + "=" + cookies.get(i).getValue() + ";";
                    	System.out.println( cookies.get(i) );
                    	
                    }
                    CookieStr = cookieStrTemporary;
                    resultMap.put("cookie", CookieStr);
                	
                }

                
                
         
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
    
}