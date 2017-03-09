package com.soon.http;

/**
 * ≤‚ ‘httpserver¿‡µƒdemo≥Ã–Ú
 * 
 * @author Soon
 * @Date 2016/10
 * @url www.so-on.cn
 */
public class demo {

	public static void main(String args[]) throws Exception {
		String webRoot = "";
		int serverPort = 800;
		HttpServer server = new HttpServer(webRoot, serverPort);
		server.run();
	}
}
