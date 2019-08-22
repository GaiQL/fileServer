package com.server.fileServer.httpAction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.server.fileServer.controller.fileController;

public class commonApi{
	
	public static Map<String, Object> loginManagement() throws IOException {
	
        String postUrl  = "/admin/login/userLogin";
        Map<String,String> postParam = new HashMap<String,String>();
        postParam.put("loginName", "h5");
        postParam.put("passWord", "123456");
        Map<String,Object> resultMap = HttpAction.uploadFileByHTTP(postUrl,postParam);
        
        return resultMap;
		
	}
	
	public static JSONObject getListDat( int bankListPage,int bankListLimit ) throws IOException {
		
        String getUrl  = "/admin/edition/query?page=" + bankListPage + "&limit=" + bankListLimit;
        
        JSONObject listData = HttpAction.httpGet( getUrl );
        
        return listData;
		
	}
	
	public static Map<String, Object> releaseApp( String filePath,JSONObject postData ) throws IOException {
		
      	InputStream finput = new FileInputStream(filePath);
      	System.out.println( finput.available() );
      	System.out.println( filePath.length() );
		byte[] imageBytes = new byte[ finput.available() ];
		finput.read(imageBytes, 0, imageBytes.length);
		finput.close();
		String imageStr = "data:application/x-zip-compressed;base64," + Base64.encodeBase64String(imageBytes);

        String releasePost  = "/admin/edition/update";
        Map<String,String> releasePostParam = new HashMap<String,String>();
        releasePostParam.put( "id", fileController.currentBankListInfo.get("id").toString() );
        releasePostParam.put( "orgId", postData.get("name").toString() );
        releasePostParam.put( "version", postData.get("version").toString() );
        releasePostParam.put( "status", postData.get("envType").toString() );
        releasePostParam.put( "fileName", postData.get("name").toString() + ".zip" );
        releasePostParam.put( "downloaUrl", imageStr );
      
        Map<String,Object> releaseAppData = HttpAction.uploadFileByHTTP( releasePost,releasePostParam );
		return releaseAppData;
		
	}
	
	
}