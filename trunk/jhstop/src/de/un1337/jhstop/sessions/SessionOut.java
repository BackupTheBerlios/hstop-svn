package de.un1337.jhstop.sessions;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.tools.Utils;

/**
 * handles data from hstopd and pus it to socket.
 * 
 * @author Felix Bechstein
 * 
 */
public class SessionOut implements Runnable {
	private DataOutputStream os;

	private Settings settings;

	private String host;

	private int port;

	private int type;

	private boolean alive;

	private String id;

	private Session s;

	public SessionOut(String sessionID, Settings settings, DataOutputStream os, String host, int port, int type,
			Session s) {
		this.os = os;
		this.settings = settings;
		this.host = host;
		this.port = port;
		this.type = type;
		this.alive = true;
		this.id = sessionID;
		this.s = s;
	}

	public void terminate() {
		alive = false;
		try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		// TODO: tcp/udp?
		if (this.type != Tunnel.TYPE_TCP)
			return;

		HttpConnection c = null;

		boolean first = true;

		String url = settings.getURL() + "?i=" + this.id;

		while (alive) {
			try {
				if (first) {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand() + "&t=tcp&h=" + this.host
							+ "&p=" + this.port + "&z=no");
					first = false;
				} else {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand());
				}

				if (settings.getPwd().length() > 0) {
					c.setRequestProperty("Authorization", "Basic "
							+ BasicAuth.encode(settings.getUser(), settings.getPwd()));
				}
				if (settings.getAgent().length() > 0) {
					c.setRequestProperty("Agent", settings.getAgent());
				}
				if (c.getResponseCode() != HttpConnection.HTTP_OK) {
					Utils.db("error out: " + c.getResponseCode());
					s.terminate();
				} else {
					InputStream is = c.openInputStream();

					byte[] buf;
					int bufsize = is.available();

					if (bufsize > 0) {
						buf = new byte[bufsize];
						bufsize = is.read(buf, 0, bufsize);
						if (bufsize > 0) {
							Utils.db("out: " + bufsize);
							s.stats.addOut(bufsize);
							// TODO: unzip
							try {
								os.write(buf, 0, bufsize);
								os.flush();
							} catch (Exception e) {
							}
						}
					}
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Utils.db("terminate: out run end");		
	}
}
