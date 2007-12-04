/**
 * 
 */
package de.berlios.hstop.jhstop2.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import com.sun.net.httpserver.HttpServer;

/**
 * @author flx
 * 
 */
public class Jhstop2d {

	public static final String PROPERTIESFILENAME = "jhstop2.properties";

	static int PORT = 8980;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTIESFILENAME));
			PORT = Integer.valueOf(properties.getProperty("jhstop2d.port", PORT + ""));
		} catch (IOException e) {
		}
		HttpServer httpd;
		try {
			httpd = HttpServer.create(new InetSocketAddress(PORT), 0);
			httpd.createContext("/", new DataHandler());
			httpd.start();
			System.out.print("hit enter to exit");
			try {
				System.in.read();
			} catch (Throwable t) {
			}
			httpd.stop(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
