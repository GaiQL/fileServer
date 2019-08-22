package com.server.fileServer.socketIo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.server.fileServer.controller.fileController;
import com.server.fileServer.envManage.switchEnv;
import com.server.fileServer.socketIo.*;

import com.server.fileServer.httpAction.HttpAction;
import com.server.fileServer.httpAction.commonApi;


public class FileTransferServer {

    private int port = 9999;
    private ServerSocket serverSocket;
    private static String fileName="D:\\[The King's Avatar][09].mp4";

    public FileTransferServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("服务器已经启动，，，，，，");
    }
    
    public static byte[] subBytes(byte[] src, int begin, int count) {
    	byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }
    
    public static void sendMessage( OutputStream out , String message , String... type ) throws IOException {
    	JSONObject jsonObj = new JSONObject();
    	jsonObj.put( "message", message );
    	jsonObj.put( "type", type.length > 0?type[0]:"text" );
        out.write( jsonObj.toString().getBytes() );
        out.flush();//清空缓存区的内容
    }
    
    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(in));
    }
    
    public void receieveFile() throws Exception  {
    	
        while (true) {
        	
        	Socket socket = null;
            try {
            	
                socket = serverSocket.accept();
                OutputStream out = socket.getOutputStream();
                sendMessage( out , "建立连接成功" );                

                BufferedReader reader = getReader(socket);
                JSONObject reqData = new JSONObject( reader.readLine() );
                
                Object type = reqData.get("type");
                Object wrapperDirName = reqData.get("name");
                switchEnv.action( (String) reqData.get("envType") );
 
                if( type.equals("fileInfo") ) {
                	
                	ZipFile zipFile = fileController.zipBase64ToFile( (String) reqData.get("fileContent"), reqData.get("name") + ".zip" );
                	
                	if( zipFile == null ) {
                		sendMessage( out , "失败，zip文件为空" );
                		out.close();
                		socket.close();
                	}
                    
                    Map<String,Object> loginData = commonApi.loginManagement();
                    
                    try {
                    	Object errData = new JSONObject( loginData.get("data").toString() ).get("userSession");
                    	sendMessage( out , "登录后台成功，开始准备上传app" );
                    }catch( Exception e ) {
				    	sendMessage( out , "登录后台失败" );
				    	out.close();
					  	socket.close();
                    }
                  	
                    Map<String,Object> releaseAppData = commonApi.releaseApp( "C:\\Users\\EDZ\\AppData\\Local\\Temp\\" + reqData.get("name") + ".zip" ,reqData );
                    sendMessage( out , "app发版成功，列表id-" + fileController.currentBankListInfo.get("id").toString() );
                    
            			
        			sendMessage( out , "开始清理远程文件夹中的打包文件..." );
        			if( fileController.clearFile( wrapperDirName ) ) {
        				sendMessage( out , "远程文件已删除，开始准备上传" );
        			}else {
        				sendMessage( out , "远程没有此项目打包文件，开始准备上传" );
        			}
    					
        			@SuppressWarnings("unchecked")
                    Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries(); 
                    int fil = 0;
                    int dir = 0;
                    
                    fileController.createDir( wrapperDirName );
                    
                    while ( enu.hasMoreElements() ) {
                    	
                        ZipEntry zipElement = (ZipEntry) enu.nextElement();  
                        InputStream read = zipFile.getInputStream(zipElement);  
                        String fileName = zipElement.getName();  
                        if (fileName != null && fileName.indexOf(".") != -1) {// 是否为文件 
                        	fileController.uploadFile( fileName,read );
                        	++fil;
                        	sendMessage( out ,Integer.toString(fil) , "upload" );
                        }else {
                        	fileController.createDir( fileName );
                        	++dir;
                        }
                        BufferedInputStream bf = null;
                        bf = new BufferedInputStream( read );

                    }
                    
                    sendMessage( out , "H5发版成功" );
                	
                }
        

                
                
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                
            } finally {
            	
            	try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
				}
                
            }

        }

    }

}
