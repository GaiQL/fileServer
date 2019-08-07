package com.server.fileServer.socketIo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import com.server.fileServer.socketIo.*;

import com.server.fileServer.httpAction.HttpAction;


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
    

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }

    public void receieveFile() {
    	
    	String filePath = "C:\\Users\\EDZ\\AppData\\Local\\Temp\\" + "248.zip";
    	
        while (true) {
        	
        	Socket socket = null;
            try {
            	
                socket = serverSocket.accept();

                System.out.println("接收到客户端的连接，，，，");
                
                OutputStream out = socket.getOutputStream();
                sendMessage( out , "建立连接成功" );
                
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath));
                
                
                byte[] buf = new byte[ 1024*1024 ];
                int len = 0;
                int endSize = 100;
                JSONObject jsonObj = null; 
                Object haha = new Object();
                
                while ( true ) {
                	len = dis.read(buf);
                	byte[] jsonBuf = subBytes( buf , len-endSize , endSize );
                	String content = new String( jsonBuf );
                	System.out.println( content );
                	if( Pattern.matches(".*/end.*", content) ) {
                		dos.write( subBytes( buf , 0 , len-endSize ) , 0 , len - endSize );
                		jsonObj = new JSONObject( content ); 
                		dos.flush();
                		break;
                	}else {
                		dos.write( buf , 0 , len );
                	}
                }

                sendMessage( out , "文件接受结束" );
                
                
                Object wrapperDirName = jsonObj.get("name");
                
                ZipFile zipFile = new ZipFile( filePath );
                System.out.println( zipFile.toString() );
                

//                String filePath = "d:/test0.jpg";
                String postUrl  = "http://39.105.208.219:8002/admin/login/userLogin";
                Map<String,String> postParam = new HashMap<String,String>();
                postParam.put("loginName", "admin");
                postParam.put("passWord", "123456");
//                File postFile = new File(filePath);
                Map<String,Object> resultMap = HttpAction.uploadFileByHTTP(postUrl,postParam);
                System.out.println( resultMap.get("cookie") );
                sendMessage( out , "登录后台成功，开始准备上传app" );
               
              String cookieData = (String) resultMap.get("cookie");
              
              


              	InputStream finput = new FileInputStream(filePath);
              	System.out.println( finput.available() );
              	System.out.println( filePath.length() );
				byte[] imageBytes = new byte[ finput.available() ];
				finput.read(imageBytes, 0, imageBytes.length);
				finput.close();
				String imageStr = "data:application/x-zip-compressed;base64," + Base64.encodeBase64String(imageBytes);

              String postUrlz  = "http://39.105.208.219:8002/admin/edition/update";
              Map<String,String> postParamz = new HashMap<String,String>();
              postParamz.put("id", jsonObj.get("id").toString());
              postParamz.put("orgId", wrapperDirName.toString() );
              postParamz.put("version", jsonObj.get("version").toString());
              postParamz.put("status", "test4");
              postParamz.put("fileName", wrapperDirName.toString() + ".zip" );
              postParamz.put("downloaUrl", imageStr);
              File downloaUrl = new File(filePath);
              
              Map<String,Object> resultMapz = HttpAction.uploadFileByHTTP(postUrlz,postParamz);
              System.out.println(resultMapz);
              sendMessage( out , "app发版成功" );
//                
                
               
                
        		
                System.out.println( wrapperDirName );
        		try {
        			
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
                    System.out.println( fil );
                    System.out.println( dir );
        			
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				} finally {
					
	                dis.close();
	                dos.close();
	                socket.close();
	                out.close();
					
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
