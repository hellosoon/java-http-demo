package com.soon.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 自己写了一份很简易的httpserver类
 * 
 * @author Soon
 * @Date 2016/10
 * @url www.so-on.cn
 */
public class HttpServer {
	// 网站根目录。
	private String webRoot = "";
	// http服务端口。
	private int serverPort = 80;
	// 请求的路径，相对于网站根目录
	private String requestPath = "";
	// 处理post文件用的界限标识
	private String boundary = null;
	// post提交方式的数据长度
	private int contentLength = 0;

	public HttpServer(String root, int port) {
		this.webRoot = root;
		this.serverPort = port;
		this.requestPath = null;
	}

	@SuppressWarnings("resource")
	public void run() throws Exception {
		// 服务运行
		ServerSocket serverSocket = new ServerSocket(this.serverPort);

		System.out.println("httpserver running at " + this.serverPort
				+ "  port");// 运行日志
		while (true) {// 监听
			Socket socket = serverSocket.accept();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));// 获取输入输出流
			reader.mark(1024);
			String line = reader.readLine();// 读取第一行，获取请求路径
			if (line == null || line.trim().length() < 1) {// 程序茁壮，防止为空造成异常
				continue;
			}
			this.requestPath = line.split(" ")[1] + "    ";
			switch (line.substring(0, 4).trim().toUpperCase()) {// 获取请求方法
			case "GET":
				if (this.requestPath.trim().equals("/")
						|| this.requestPath.substring(0, 2).trim().equals("/?")) {// 加入默认项index.html
					this.requestPath = "/index.html"
							+ this.requestPath.substring(1,
									this.requestPath.length()).trim();
				}
				String fileType = requestPath.substring(
						requestPath.lastIndexOf("."), requestPath.length());// 判断类型
				this.doGet(socket, fileType);
				break;
			case "POST":
				this.doPost(socket, reader);
				break;
			}
		}
	}

	/*
	 * 处理get请求
	 */
	@SuppressWarnings("resource")
	private void doGet(Socket socket, String type) throws Exception {
		System.out.println(type);// 运行日志
		this.requestPath = this.requestPath.trim();
		// this.requestPath = line.split(" ")[1] + "    ";
		System.out.println("handle GET request");// 运行日志
		System.out.println("request path : " + this.requestPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		if (new File(this.webRoot + this.requestPath).exists()) {// 如果存在
			InputStream fileStream = new FileInputStream(this.webRoot
					+ this.requestPath);// 获取文件
			OutputStream out = socket.getOutputStream();// 获得输入输出流
			byte[] buf = new byte[fileStream.available()];// 响应实体
			fileStream.read(buf);
			String response = "";// 响应头部
			response += "HTTP/1.1 200 OK\n";
			response += "Server: soonServer 1.0\n";
			response += "Content-Length: " + buf.length + "\n";
			if (type.trim().equalsIgnoreCase(".html")) {
				response += "Content-Type: text/html\n";
			} else if (type.trim().equalsIgnoreCase(".css")) {
				response += "Content-Type: text/css\n";
			} else if (type.trim().equalsIgnoreCase(".js")) {
				response += "Content-Type: application/x-javascript\n";
			} else if (type.trim().equalsIgnoreCase(".jpg")) {
				response += "Content-Type: image/jpeg\n";
			}
			response += "Accept-ranges: bytes\n";
			response += "\n";
			out.write(response.getBytes());
			out.write(buf);
			// 关闭之前打开的东西
			out.close();
			fileStream.close();
			reader.close();
			socket.close();
			System.out.println("http get connection colsed");// 运行日志
		} else {// 如果不存在
			InputStream fileStream = new FileInputStream(this.webRoot
					+ "/404.html");// 获取自定义404文件
			OutputStream out = socket.getOutputStream();// 获得输入输出流
			byte[] buf = new byte[fileStream.available()];// 响应实体
			fileStream.read(buf);
			String response = "";// 响应头部
			response += "HTTP/1.1 404 Not Found\n";
			response += "Server: soonServer 1.0\n";
			response += "Content-Length: " + buf.length + "\n";
			response += "Content-Type: text/html\n";
			response += "Accept-ranges: bytes\n";
			response += "\n";
			out.write(response.getBytes());
			out.write(buf);
			out.flush();
			reader.close();
			socket.close();
			System.out.println("http get connection colsed");// 运行日志
		}
	}

	/*
	 * 处理post请求
	 */
	private void doPost(Socket socket, BufferedReader reader) throws Exception {
		System.out.println("handle POST request");// 运行日志
		String line = reader.readLine();
		while (line != null) {
			System.out.println(line);// 运行日志
			line = reader.readLine();
			if ("".equals(line)) {// 处理结尾没有换行符造成空等的情况
				break;
			} else if (line.indexOf("Content-Length") != -1) {
				this.contentLength = Integer.parseInt(line.substring(line
						.indexOf("Content-Length") + 16));// 取得长度
			}
			// 如果要上传附件，跳转multiltipart函数处理
			else if (line.indexOf("multipart/form-data") != -1) {
				this.boundary = line.substring(line.indexOf("boundary") + 9); // 获取multiltipart的分隔符
				this.doMultiPart(socket, reader);
				return;
			}
		}
		System.out.println("getting post data");// 运行日志
		char[] buf = null;
		if (this.contentLength != 0) {
			buf = new char[this.contentLength];
			reader.read(buf, 0, contentLength);
			System.out.println("post data: " + new String(buf));// 运行日志
		}
		OutputStream out = socket.getOutputStream();
		String body = "<!DOCTYPE html><html><head><title>success</title></head><body><p>success!</p><p>your post data:</p>"
				+ (new String(buf).replaceAll("&", "<br>")) + "</body></html>";// 返回实体
		System.out.println(body);// 运行日志
		String response = "";// 响应头部
		response += "HTTP/1.1 200 OK\n";
		response += "Server: soonServer 1.0\n";
		response += "Content-Length: " + body.length() + "\n";
		response += "Content-Type: text/html\n";
		response += "Accept-ranges: bytes\n";
		response += "\n";
		out.write(response.getBytes());
		out.write(body.getBytes());
		// 关闭之前打开的
		out.flush();
		reader.close();
		socket.close();
		System.out.println("http post connection colsed");// 运行日志
	}

	/*
	 * post上传文件
	 */
	private void doMultiPart(Socket socket, BufferedReader reader)
			throws Exception {
		System.out.println("handle POST_FILE request");// 运行日志
		String line = reader.readLine();
		while (line != null) {
			System.out.println(line);// 运行日志
			line = reader.readLine();
			if ("".equals(line)) {// 处理结尾没有换行符造成空等的情况
				break;
			}
			// 下面是为了茁壮防止崩溃
			else if (line.indexOf("Content-Length") != -1) {
				this.contentLength = Integer.parseInt(line.substring(line
						.indexOf("Content-Length") + 16));// 取得长度
			} else if (line.indexOf("boundary") != -1) {
				this.boundary = line.substring(line.indexOf("boundary") + 9);// 取得标识
			}
		}
		System.out.println("getting post file data");// 运行日志
		if (this.contentLength != 0) {
			byte[] buf = new byte[this.contentLength];
			int totalRead = 0;
			int size = 0;
			while (totalRead < this.contentLength) {
				size = socket.getInputStream().read(buf, totalRead,
						this.contentLength - totalRead);
				totalRead += size;
			}
			System.out.println("readLength: " + totalRead);
			String dataString = new String(buf, 0, totalRead);
			// System.out.println("post data :\n" + dataString);
			// 得到附件的开始位置和结束位置
			System.out.println("boundary: " + boundary);// 运行日志
			int indexCount = this.getSubString(dataString, boundary);
			int pos = dataString.indexOf(boundary);
			for (int i = 0; i < indexCount; i++) {
				int pos_2 = dataString.indexOf("\n", pos) + 1;
				int pos_3 = dataString.indexOf("\n", pos_2) + 1;
				if (dataString.substring(pos, pos_3).trim()
						.contains("filename")) {// 不输出文件数据
					continue;
				}
				int pos_4 = dataString.indexOf("\n", pos_3) + 1;
				pos = dataString.indexOf(boundary, pos_2) - 2;
				System.out.println(dataString.substring(pos_4, pos).trim());// 输出非文件数据
			}
			int fileNameBegin = dataString.indexOf("filename") + 10;
			int fileNameEnd = dataString.indexOf("\n", fileNameBegin) - 2;
			String fileName = dataString.substring(fileNameBegin, fileNameEnd);//获取文件名
			int fileIndexStart_1 = dataString.indexOf("\n", fileNameEnd);
			int fileIndexStart_2 = dataString.indexOf("\n", fileIndexStart_1);
			int fileIndexStart = dataString.indexOf("\n", fileIndexStart_2);
			fileIndexStart = dataString.substring(0, fileIndexStart + 29).getBytes().length;//获取文件流在数据流中的起始位置
			int fileIndexEnd = dataString.indexOf(boundary, fileIndexStart) - 2;
			fileIndexEnd = dataString.substring(0, fileIndexEnd).getBytes().length;//获取文件流在数据流中的终止位置
			System.out.println("fileName: " + fileName);// 运行日志
			OutputStream fileOut = new FileOutputStream(this.webRoot + "/"
					+ fileName);
			fileOut.write(buf, fileIndexStart, fileIndexEnd - fileIndexStart);
			fileOut.close();
			fileOut.close();
		}
		String body = "<html><head><title>success</title></head><body><p>success!</p><p>post file saved</p></body></html>";// 相应实体
		String response = "";// 响应头部
		response += "HTTP/1.1 200 OK\n";
		response += "Server: soonServer 1.0\n";
		response += "Content-Length: " + body.length() + "\n";
		response += "Content-Type: text/html\n";
		response += "Accept-ranges: bytes\n";
		response += "\n";
		OutputStream out = socket.getOutputStream();
		out.write(response.getBytes());
		out.write(body.getBytes());
		// 关闭之前打开的
		out.flush();
		reader.close();
		socket.close();
		System.out.println("http post_file connection colsed");// 运行日志
	}

	/*
	 * 统计字符串内部字符出现次数
	 */
	private int getSubString(String str, String key) {
		int count = 0;
		int index = 0;
		while ((index = str.indexOf(key, index)) != -1) {
			index = index + key.length();
			count++;
		}
		return count;
	}
}
