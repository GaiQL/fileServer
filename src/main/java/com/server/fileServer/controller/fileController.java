package com.server.fileServer.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.server.fileServer.envManage.switchEnv;
import com.server.fileServer.httpAction.HttpAction;
import com.server.fileServer.httpAction.commonApi;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

@RestController
public class fileController {
		
	//  读取远程文件；
    public static void readToBuffer(StringBuffer buffer, String filePath) throws IOException {
    	SmbFileInputStream smbfile = new SmbFileInputStream( filePath );
        String line; // 用来保存每行读取的内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(smbfile));
        line = reader.readLine(); // 读取第一行
        while (line != null) { // 如果 line 为空说明读完了
            buffer.append(line); // 将读到的内容添加到 buffer 中
            buffer.append("\n"); // 添加换行符
            line = reader.readLine(); // 读取下一行
        }
        reader.close();
        smbfile.close();
    }
    
    //  查询后台银行列表
    private static int bankListPage = 1;
    private static int bankListLimit = 12;
    public static JSONObject appListFind( int bankId,String envType ) throws Exception {
    	
    	JSONObject listData = commonApi.getListDat( envType,bankListPage,bankListLimit );
        
        if( (int) listData.get("code") == 0 ) {
        	
        	JSONArray bankList = new JSONArray( listData.get("data").toString() );
        	
        	if( bankList.length() == 0 ) { return new JSONObject(); }
        	
        	for(int i=0; i<bankList.length(); i++) {
        		JSONObject jsonObj = bankList.getJSONObject(i);
        		String prepareEnvType = (String) jsonObj.get("status");
        		int prepareOrgId = (int) jsonObj.get("orgId");
        		if(  prepareEnvType.equals(envType) && prepareOrgId == bankId ) {
        			return bankList.getJSONObject(i);
        		}
        	}
        	
        	++bankListPage;
        	JSONObject currentData = fileController.appListFind( bankId,envType );
        	if( !currentData.isEmpty() ) {
        		return currentData;
        	}
        	
        }
        
        return new JSONObject();
        
    }
    
    //  获取APP版本
    public static JSONObject getAppVersion( int bankId,String envType ) throws Exception {
    	
    	Map<String,Object> loginData = commonApi.loginManagement(envType);
    	
    	System.out.println( loginData );
        bankListPage = 1;
        bankListLimit = 12;
    	return fileController.appListFind( bankId,envType );
        
    }
	
    //  获取版本；
	public static String getVersion( String envType,String bankIdStr ) throws Exception {
    	
		Integer bankId = Integer.valueOf( bankIdStr );
    	
		String fileUrl = switchEnv.getTelefileBaseUrl(envType) + bankId + "/appConfig.json";
		
		SmbFile smbfile = new SmbFile( fileUrl );
		JSONObject resJson = new JSONObject(); 
		resJson.put( "type", "versionData" );
		resJson.put( "h5", new JSONObject() );
		resJson.put( "app", new JSONObject() );
		if( smbfile.exists() ) {
			
	        StringBuffer sb = new StringBuffer();
	        fileController.readToBuffer(sb, fileUrl);
			resJson.put( "h5", new JSONObject( sb.toString() ).get("app")  );
			
		}
		
		
		resJson.put( "app", fileController.getAppVersion( bankId,envType ) );

        System.out.println( resJson );
        
        return resJson.toString();
        
    }
	
	private static void recursiveFiles( String path ) throws SmbException, MalformedURLException{
        
        // 创建 File对象
		SmbFile smbfile = new SmbFile(path);
		SmbFile[] smbfiles = smbfile.listFiles();
        
        // 对象为空 直接返回
        if(smbfiles == null){
            return;
        }
                
        // 目录下文件
        if(smbfiles.length == 0){
            System.out.println(path + "该文件夹下没有文件");
        }
        
        // 存在文件 遍历 判断
        for (SmbFile f : smbfiles) {
            
            // 判断是否为 文件夹
            if( f.isDirectory() ){
                
                System.out.print("文件夹: "); 
                System.out.println(f.getPath());  
                
                // 为 文件夹继续遍历
//                recursiveFiles(f.getPath());
                f.delete();
                
            
            // 判断是否为 文件
            } else if(f.isFile()){
                
                System.out.print("文件: "); 
                System.out.println(f.getPath());  
                f.delete();
                
            } else {
                System.out.print("未知错误文件"); 
            }
            
        }
        
    }
    
	public static boolean clearFile( String envType,Object fileName ) throws Exception {
    	
		boolean result;
		String deleteUrl = switchEnv.getTelefileBaseUrl(envType) + fileName + '/';
		SmbFile smbfile = new SmbFile( deleteUrl ); 
		
	    if(smbfile.exists()){
	    	smbfile.delete();
	    	result = true;
	    }else {
	    	result = false;
	    }
	    
	    return result;
        
    }
	
    public static void createDir( String envType,Object wrapperDirName) throws Exception {
	    SmbFile smbFile;
	    try {
	        smbFile = new SmbFile( switchEnv.getTelefileBaseUrl(envType)  + wrapperDirName);
	        if (!smbFile.exists()) {
	            smbFile.mkdir();
	        }
	    } catch (MalformedURLException e) {
	        e.printStackTrace();
	    } catch (SmbException e) {
	        e.printStackTrace();
	    }
    }
    
    public static void uploadFile(String envType,String name,InputStream read){
        BufferedInputStream bf = null; 
        SmbFileOutputStream smbOut = null;
        try{
            smbOut = new SmbFileOutputStream( switchEnv.getTelefileBaseUrl(envType) + "/" + name, false ); 
            bf = new BufferedInputStream( read ); 
            byte[] bt = new byte[8192]; 
            int n = bf.read(bt); 
            while (n != -1){
                smbOut.write(bt, 0, n); 
                smbOut.flush(); 
                n = bf.read(bt); 
            }
        }catch(Exception e) {
            e.printStackTrace(); 
        }finally{
            try {
                if(null != smbOut)
                    smbOut.close(); 
                if(null != bf) 
                    bf.close(); 
            }catch(Exception e2) {
                e2.printStackTrace(); 
            }
        }
    }
    
    public static ZipFile zipBase64ToFile(String base64, String fileName) {
    	
        File file = null;
        String filePath = "C:\\Users\\EDZ\\AppData\\Local\\Temp";
        String zipPath = filePath + "\\" + fileName;
        File  dir = new File(filePath);
        if (!dir.exists() && !dir.isDirectory()) {
                dir.mkdirs();
        }
        BufferedOutputStream bos = null;
        java.io.FileOutputStream fos = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            file = new File( zipPath );
            fos = new java.io.FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
            
            return new ZipFile( zipPath );
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		return null;
        
    }
    
}