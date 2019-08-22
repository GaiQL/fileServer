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
	
	//  有文件但无版本号， 
	
	public static String baseUrl = "smb://apph5:apph5@" + switchEnv.sharedFoldersAddress + "/" + switchEnv.sharedFoldersName + "/app-h5-api/banks/";
	
	public static JSONObject currentBankListInfo = new JSONObject();
	
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
    public static void appListFind( int bankId,String envType ) throws IOException {
    	
    	JSONObject listData = commonApi.getListDat( bankListPage,bankListLimit );
        
        if( (int) listData.get("code") == 0 ) {
        	
        	JSONArray bankList = new JSONArray( listData.get("data").toString() );
        	
        	if( bankList.length() == 0 ) { return; }
        	
        	for(int i=0; i<bankList.length(); i++) {
        		JSONObject jsonObj = bankList.getJSONObject(i);
        		String prepareEnvType = (String) jsonObj.get("status");
        		int prepareOrgId = (int) jsonObj.get("orgId");
        		if(  prepareEnvType.equals(envType) && prepareOrgId == bankId ) {
        			currentBankListInfo = bankList.getJSONObject(i);
        			break;
        		}
        	}
        	
        	if( !currentBankListInfo.has("version") ) { ++bankListPage;fileController.appListFind( bankId,envType ); }
        	
        }
        
    }
    
    //  获取APP版本
    public static void getAppVersion( int bankId,String envType ) throws IOException {
    	
    	Map<String,Object> loginData = commonApi.loginManagement();
    	
    	System.out.println( loginData );
        bankListPage = 1;
        bankListLimit = 12;
        currentBankListInfo = new JSONObject();
    	fileController.appListFind( bankId,envType );
        
    }
	
    //  获取版本；
    @PostMapping(path = "/getVersion")
	public static String getVersion(@RequestBody Map<String, String> params) throws Exception {
    	
    	int bankId = Integer.valueOf( params.get("bankId") );
    	String envType = (String) params.get("envType");
    	switchEnv.action(envType);
    	
    	
		String fileUrl = baseUrl + bankId + "/appConfig.json";
		
		SmbFile smbfile = new SmbFile( fileUrl );
		JSONObject resJson = new JSONObject(); 
		resJson.put( "h5", new JSONObject() );
		resJson.put( "app", new JSONObject() );
		if( smbfile.exists() ) {
			
	        StringBuffer sb = new StringBuffer();
	        fileController.readToBuffer(sb, fileUrl);
			resJson.put( "h5", new JSONObject( sb.toString() ).get("app")  );
			
		}
		
		fileController.getAppVersion( bankId,envType );
		resJson.put( "app", currentBankListInfo  );

        System.out.println( resJson );
        
        return resJson.toString();
        
    }
    
	public static boolean clearFile( Object fileName ) throws Exception {
    	
		boolean result;
		String deleteUrl = baseUrl + fileName + '/';
		SmbFile smbfile = new SmbFile( deleteUrl );
		
	    if(smbfile.exists()){
	    	smbfile.delete();
	    	result = true;
	    }else {
	    	result = false;
	    }
	    
	    return result;
        
    }
	
    public static void createDir(Object wrapperDirName) {
	    SmbFile smbFile;
	    try {
	        smbFile = new SmbFile(baseUrl + wrapperDirName);
	        if (!smbFile.exists()) {
	            smbFile.mkdir();
	        }
	    } catch (MalformedURLException e) {
	        e.printStackTrace();
	    } catch (SmbException e) {
	        e.printStackTrace();
	    }
    }
    
    @PostMapping(path = "/uploadFile")
    @ResponseBody
    public String handleFileUpload( @RequestParam("file") MultipartFile inFile ) throws Exception {
    	
    	String wrapperDirName;
        if (!inFile.isEmpty()) {
        	
	        String content = inFile.getOriginalFilename();
	        String pattern = ".*.zip$";
	        if( !Pattern.matches(pattern, content) ) { return "文件必须是zpi哦"; }
	        wrapperDirName = content.split("\\.")[0];
        	
            InputStream fileStream = null;
            File file = null;
            Stream<? extends ZipEntry> signStream = null;
            Stream<? extends ZipEntry> zipStream = null;
            ZipFile zipFile = null;
            
            //临时文件
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), wrapperDirName + UUID.randomUUID() + ".zip");
            file = path.toFile();
            try {
                fileStream = inFile.getInputStream();
                org.apache.commons.io.FileUtils.copyInputStreamToFile(fileStream, file);
                System.out.println( inFile.getOriginalFilename() );
                System.out.println( Charset.defaultCharset() );
                zipFile = new ZipFile(file, Charset.defaultCharset());
                signStream = zipFile.stream();
                zipStream = zipFile.stream();
                
//                //断言
//                Predicate<ZipEntry> signTxt = ze -> ze.getName().contains("sign,txt");
//                Predicate<ZipEntry> zipTxt = ze -> ze.getName().endsWith(".zip");
//                //，过滤
//                Optional<ZipEntry> signInfo = (Optional<ZipEntry>) signStream.filter(signTxt).findFirst();
//                Optional<ZipEntry> zipInfo = (Optional<ZipEntry>) zipStream.filter(zipTxt).findFirst();
//                if (signInfo.isPresent() && zipInfo.isPresent()) {
//                    System.out.println("hello");
//                }
                
                @SuppressWarnings("unchecked")
                Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries(); 
                int fil = 0;
                int dir = 0;
                
                createDir( wrapperDirName );
                
                while ( enu.hasMoreElements() ) {  
                    ZipEntry zipElement = (ZipEntry) enu.nextElement();  
                    InputStream read = zipFile.getInputStream(zipElement);  
                    String fileName = zipElement.getName();  
                    if (fileName != null && fileName.indexOf(".") != -1) {// 是否为文件 
                    	uploadFile( fileName,read );
                    	++fil;
                    }else {
                    	createDir( fileName );
                    	++dir;
                    }
                    BufferedInputStream bf = null;
                    bf = new BufferedInputStream( read ); 

                }
                
                System.out.println( fil );
                System.out.println( dir );
     
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭流
     
                    fileStream.close();
                    signStream.close();
                    zipStream.close();
                    zipFile.close();
                    //删除临时文件
     
                    org.apache.commons.io.FileUtils.deleteQuietly(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
     
            }

        	
        } else {
            return "文件为空";    
        }
        
        return "success";
        
        
    }
    
    /**上传文件到服务器*/
    public static void uploadFile(String name,InputStream read){
        BufferedInputStream bf = null; 
        SmbFileOutputStream smbOut = null;
        try{
            smbOut = new SmbFileOutputStream(baseUrl + "/" + name, false); 
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