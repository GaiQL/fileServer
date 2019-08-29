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
	
	public static Map<String, Object> loginManagement( String envType ) throws IOException {
	
        String postUrl  = "/admin/login/userLogin";
        Map<String,String> postParam = new HashMap<String,String>();
        if( envType.equals("preheat") ) {
            postParam.put("loginName", "h5_wby");
            postParam.put("passWord", "123456");
        }else if( envType.equals("dev") ) {
            postParam.put("loginName", "adminzz");
            postParam.put("passWord", "123456");
        }else {
            postParam.put("loginName", "h5");
            postParam.put("passWord", "123456");
        }
        Map<String,Object> resultMap = HttpAction.uploadFileByHTTP(envType,postUrl,postParam);
        
        return resultMap;
		
	}
	
	public static JSONObject getListDat( String envType,int bankListPage,int bankListLimit ) throws IOException {
		
        String getUrl  = "/admin/edition/query?page=" + bankListPage + "&limit=" + bankListLimit;
        
        JSONObject listData = HttpAction.httpGet( envType,getUrl );
        
        return listData;
		
	}
	
	public static Map<String, Object> releaseApp( JSONObject currentBankListInfo,String envType,String bankId,JSONObject postData ) throws IOException {
		
		String filePath = "C:\\Users\\EDZ\\AppData\\Local\\Temp\\" + bankId + ".zip";
		
      	InputStream finput = new FileInputStream(filePath);
      	System.out.println( finput.available() );	
      	System.out.println( filePath.length() );
		byte[] imageBytes = new byte[ finput.available() ];
		finput.read(imageBytes, 0, imageBytes.length);
		finput.close();
		String imageStr = "data:application/x-zip-compressed;base64," + Base64.encodeBase64String(imageBytes);
		
		String releasePost = null;
		Map<String,String> releasePostParam = new HashMap<String,String>();
		
		try {
			releasePost = "/admin/edition/update";
			releasePostParam.put( "id", currentBankListInfo.get("id").toString() );  //编辑的时候要传id;
		}catch( Exception e ) {
			releasePost = "/admin/edition/add";	
		}
        
        releasePostParam.put( "orgId", bankId );
        releasePostParam.put( "version", postData.get("version").toString() );
        releasePostParam.put( "status", envType );
        releasePostParam.put( "fileName", bankId + ".zip" );
        releasePostParam.put( "downloaUrl", imageStr );
      
        Map<String,Object> releaseAppData = HttpAction.uploadFileByHTTP( envType,releasePost,releasePostParam );
		return releaseAppData;
		
	}
	
	
}