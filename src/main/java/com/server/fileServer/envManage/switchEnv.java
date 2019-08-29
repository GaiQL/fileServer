package com.server.fileServer.envManage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.springframework.util.ResourceUtils;

import com.server.fileServer.controller.fileController;

public class switchEnv {

	public static String getTelefileBaseUrl( String envType ) throws IOException {
		
		String posteriorUrl = "/app-h5-api/banks/";
		
		if( envType.equals( "preheat" ) ) { posteriorUrl = "/banks/"; }
		
		return "smb://apph5:apph5@" + readUlServerConfig(envType).get("sharedFoldersAddress") + "/" + readUlServerConfig(envType).get("sharedFoldersName") + posteriorUrl; 
		
	}
	
	public static String getManagementAddress( String envType ) throws IOException {
		
		return (String) readUlServerConfig(envType).get("managementAddress");
		
	}
	
	public static JSONObject readUlServerConfig( String envType ) throws IOException {
		
		StringBuffer ulServerConfig = new StringBuffer();
        
        readToBuffer( ulServerConfig, "classpath:ulServerConfig.json" );
        
        JSONObject currentEnv = (JSONObject) new JSONObject( ulServerConfig.toString() ).get( envType );
    	
    	return currentEnv;
		
	}

    public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
    	
    	String filestr =  ResourceUtils.getFile("classpath:jsonDir/ulServerConfig.json").getPath();
    	FileInputStream smbfile = new FileInputStream( filestr );
        String line;
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