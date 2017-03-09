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
 * �Լ�д��һ�ݺܼ��׵�httpserver��
 * 
 * @author Soon
 * @Date 2016/10
 * @url www.so-on.cn
 */
public class HttpServer {
	// ��վ��Ŀ¼��
	private String webRoot = "";
	// http����˿ڡ�
	private int serverPort = 80;
	// �����·�����������վ��Ŀ¼
	private String requestPath = "";
	// ����post�ļ��õĽ��ޱ�ʶ
	private String boundary = null;
	// post�ύ��ʽ�����ݳ���
	private int contentLength = 0;

	public HttpServer(String root, int port) {
		this.webRoot = root;
		this.serverPort = port;
		this.requestPath = null;
	}

	@SuppressWarnings("resource")
	public void run() throws Exception {
		// ��������
		ServerSocket serverSocket = new ServerSocket(this.serverPort);

		System.out.println("httpserver running at " + this.serverPort
				+ "  port");// ������־
		while (true) {// ����
			Socket socket = serverSocket.accept();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));// ��ȡ���������
			reader.mark(1024);
			String line = reader.readLine();// ��ȡ��һ�У���ȡ����·��
			if (line == null || line.trim().length() < 1) {// ������׳����ֹΪ������쳣
				continue;
			}
			this.requestPath = line.split(" ")[1] + "    ";
			switch (line.substring(0, 4).trim().toUpperCase()) {// ��ȡ���󷽷�
			case "GET":
				if (this.requestPath.trim().equals("/")
						|| this.requestPath.substring(0, 2).trim().equals("/?")) {// ����Ĭ����index.html
					this.requestPath = "/index.html"
							+ this.requestPath.substring(1,
									this.requestPath.length()).trim();
				}
				String fileType = requestPath.substring(
						requestPath.lastIndexOf("."), requestPath.length());// �ж�����
				this.doGet(socket, fileType);
				break;
			case "POST":
				this.doPost(socket, reader);
				break;
			}
		}
	}

	/*
	 * ����get����
	 */
	@SuppressWarnings("resource")
	private void doGet(Socket socket, String type) throws Exception {
		System.out.println(type);// ������־
		this.requestPath = this.requestPath.trim();
		// this.requestPath = line.split(" ")[1] + "    ";
		System.out.println("handle GET request");// ������־
		System.out.println("request path : " + this.requestPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		if (new File(this.webRoot + this.requestPath).exists()) {// �������
			InputStream fileStream = new FileInputStream(this.webRoot
					+ this.requestPath);// ��ȡ�ļ�
			OutputStream out = socket.getOutputStream();// ������������
			byte[] buf = new byte[fileStream.available()];// ��Ӧʵ��
			fileStream.read(buf);
			String response = "";// ��Ӧͷ��
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
			// �ر�֮ǰ�򿪵Ķ���
			out.close();
			fileStream.close();
			reader.close();
			socket.close();
			System.out.println("http get connection colsed");// ������־
		} else {// ���������
			InputStream fileStream = new FileInputStream(this.webRoot
					+ "/404.html");// ��ȡ�Զ���404�ļ�
			OutputStream out = socket.getOutputStream();// ������������
			byte[] buf = new byte[fileStream.available()];// ��Ӧʵ��
			fileStream.read(buf);
			String response = "";// ��Ӧͷ��
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
			System.out.println("http get connection colsed");// ������־
		}
	}

	/*
	 * ����post����
	 */
	private void doPost(Socket socket, BufferedReader reader) throws Exception {
		System.out.println("handle POST request");// ������־
		String line = reader.readLine();
		while (line != null) {
			System.out.println(line);// ������־
			line = reader.readLine();
			if ("".equals(line)) {// �����βû�л��з���ɿյȵ����
				break;
			} else if (line.indexOf("Content-Length") != -1) {
				this.contentLength = Integer.parseInt(line.substring(line
						.indexOf("Content-Length") + 16));// ȡ�ó���
			}
			// ���Ҫ�ϴ���������תmultiltipart��������
			else if (line.indexOf("multipart/form-data") != -1) {
				this.boundary = line.substring(line.indexOf("boundary") + 9); // ��ȡmultiltipart�ķָ���
				this.doMultiPart(socket, reader);
				return;
			}
		}
		System.out.println("getting post data");// ������־
		char[] buf = null;
		if (this.contentLength != 0) {
			buf = new char[this.contentLength];
			reader.read(buf, 0, contentLength);
			System.out.println("post data: " + new String(buf));// ������־
		}
		OutputStream out = socket.getOutputStream();
		String body = "<!DOCTYPE html><html><head><title>success</title></head><body><p>success!</p><p>your post data:</p>"
				+ (new String(buf).replaceAll("&", "<br>")) + "</body></html>";// ����ʵ��
		System.out.println(body);// ������־
		String response = "";// ��Ӧͷ��
		response += "HTTP/1.1 200 OK\n";
		response += "Server: soonServer 1.0\n";
		response += "Content-Length: " + body.length() + "\n";
		response += "Content-Type: text/html\n";
		response += "Accept-ranges: bytes\n";
		response += "\n";
		out.write(response.getBytes());
		out.write(body.getBytes());
		// �ر�֮ǰ�򿪵�
		out.flush();
		reader.close();
		socket.close();
		System.out.println("http post connection colsed");// ������־
	}

	/*
	 * post�ϴ��ļ�
	 */
	private void doMultiPart(Socket socket, BufferedReader reader)
			throws Exception {
		System.out.println("handle POST_FILE request");// ������־
		String line = reader.readLine();
		while (line != null) {
			System.out.println(line);// ������־
			line = reader.readLine();
			if ("".equals(line)) {// �����βû�л��з���ɿյȵ����
				break;
			}
			// ������Ϊ����׳��ֹ����
			else if (line.indexOf("Content-Length") != -1) {
				this.contentLength = Integer.parseInt(line.substring(line
						.indexOf("Content-Length") + 16));// ȡ�ó���
			} else if (line.indexOf("boundary") != -1) {
				this.boundary = line.substring(line.indexOf("boundary") + 9);// ȡ�ñ�ʶ
			}
		}
		System.out.println("getting post file data");// ������־
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
			// �õ������Ŀ�ʼλ�úͽ���λ��
			System.out.println("boundary: " + boundary);// ������־
			int indexCount = this.getSubString(dataString, boundary);
			int pos = dataString.indexOf(boundary);
			for (int i = 0; i < indexCount; i++) {
				int pos_2 = dataString.indexOf("\n", pos) + 1;
				int pos_3 = dataString.indexOf("\n", pos_2) + 1;
				if (dataString.substring(pos, pos_3).trim()
						.contains("filename")) {// ������ļ�����
					continue;
				}
				int pos_4 = dataString.indexOf("\n", pos_3) + 1;
				pos = dataString.indexOf(boundary, pos_2) - 2;
				System.out.println(dataString.substring(pos_4, pos).trim());// ������ļ�����
			}
			int fileNameBegin = dataString.indexOf("filename") + 10;
			int fileNameEnd = dataString.indexOf("\n", fileNameBegin) - 2;
			String fileName = dataString.substring(fileNameBegin, fileNameEnd);//��ȡ�ļ���
			int fileIndexStart_1 = dataString.indexOf("\n", fileNameEnd);
			int fileIndexStart_2 = dataString.indexOf("\n", fileIndexStart_1);
			int fileIndexStart = dataString.indexOf("\n", fileIndexStart_2);
			fileIndexStart = dataString.substring(0, fileIndexStart + 29).getBytes().length;//��ȡ�ļ������������е���ʼλ��
			int fileIndexEnd = dataString.indexOf(boundary, fileIndexStart) - 2;
			fileIndexEnd = dataString.substring(0, fileIndexEnd).getBytes().length;//��ȡ�ļ������������е���ֹλ��
			System.out.println("fileName: " + fileName);// ������־
			OutputStream fileOut = new FileOutputStream(this.webRoot + "/"
					+ fileName);
			fileOut.write(buf, fileIndexStart, fileIndexEnd - fileIndexStart);
			fileOut.close();
			fileOut.close();
		}
		String body = "<html><head><title>success</title></head><body><p>success!</p><p>post file saved</p></body></html>";// ��Ӧʵ��
		String response = "";// ��Ӧͷ��
		response += "HTTP/1.1 200 OK\n";
		response += "Server: soonServer 1.0\n";
		response += "Content-Length: " + body.length() + "\n";
		response += "Content-Type: text/html\n";
		response += "Accept-ranges: bytes\n";
		response += "\n";
		OutputStream out = socket.getOutputStream();
		out.write(response.getBytes());
		out.write(body.getBytes());
		// �ر�֮ǰ�򿪵�
		out.flush();
		reader.close();
		socket.close();
		System.out.println("http post_file connection colsed");// ������־
	}

	/*
	 * ͳ���ַ����ڲ��ַ����ִ���
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
