package com.server.fileServer;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.server.fileServer.socketIo.FileTransferServer;
import com.server.fileServer.socketNio.SocketServer;

@SpringBootApplication
public class FileServerApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(FileServerApplication.class, args);
		
////		起socket服务
//		SocketServer server = new SocketServer();
//		server.startSocketServer(9999);
		
		try {
			new FileTransferServer().receieveFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

