package com.server.fileServer.envManage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.springframework.util.ResourceUtils;

import com.server.fileServer.controller.fileController;

public class switchEnv {
	
	public static String sharedFoldersAddress = "39.106.200.171/";
	
	public static String sharedFoldersName = "app-test4";
	
	public static void action( String envType ) throws IOException {
    	
        StringBuffer ulServerConfig = new StringBuffer();
        
        readToBuffer( ulServerConfig, "classpath:ulServerConfig.json" );
        
        JSONObject currentEnv = (JSONObject) new JSONObject( ulServerConfig.toString() ).get( envType );
        
    	sharedFoldersAddress = (String) currentEnv.get("sharedFoldersAddress");
    	
    	sharedFoldersName = (String) currentEnv.get("sharedFoldersName");
    	
    	fileController.baseUrl = "smb://apph5:apph5@" + sharedFoldersAddress + "/" + sharedFoldersName + "/app-h5-api/banks/";
        
    }

    public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
    	String filestr =  ResourceUtils.getFile("classpath:jsonDir/ulServerConfig.json").getPath();
    	FileInputStream smbfile = new FileInputStream( filestr );
        String line; // 用来保存每行读取的内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(smbfile));
        line = reader.readLine();
        while (line != null) {
            buffer.append(line);
            buffer.append("\n"); 
            line = reader.readLine(); 
        }
        reader.close();
        smbfile.close();
    }
	
}