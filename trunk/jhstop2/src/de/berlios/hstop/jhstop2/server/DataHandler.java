/**
 * 
 */
package de.berlios.hstop.jhstop2.server;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.berlios.hstop.jhstop2.common.TunnelData;

/**
 * @author flx
 * 
 */
public class DataHandler implements HttpHandler {
	public void handle(HttpExchange httpExchange) throws IOException {
		byte[] response;
		int responseCode;
		try {
			if (httpExchange.getRequestMethod() == "GET") {
				response = handleGET(httpExchange);
			} else if (httpExchange.getRequestMethod() == "PUT") {
				response = handlePUT(httpExchange);
			} else {
				response = null;
			}
			responseCode = 200;
		} catch (IOException e) {
			response = "404".getBytes();
			httpExchange.getResponseHeaders().add("Content-type", "text/html");
			responseCode = 404;
		}

		if (response != null) {
			httpExchange.sendResponseHeaders(responseCode, response.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(response);
			os.close();
		} else
			httpExchange.sendResponseHeaders(responseCode, 0);
	}

	private byte[] handleGET(HttpExchange httpExchange) throws IOException {

		TunnelData td = new TunnelData();
		// FIXME: fill me with logic
		// build up tunneldata and return it to handle()
		return td.toArray();
	}

	private byte[] handlePUT(HttpExchange httpExchange) throws IOException {
		// fetch data from httpexchange and put it into tunneldata
		// FIXME: fill me with logic
		TunnelData td = new TunnelData(null); // replace null with httpexchange.data here!
		// we don't need to return anything!
		return null;
	}

}
