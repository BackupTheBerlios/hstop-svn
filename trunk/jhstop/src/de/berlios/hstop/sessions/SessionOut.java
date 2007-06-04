package de.berlios.hstop.sessions;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.berlios.hstop.midlet.jhstopc;
import de.berlios.hstop.tools.Utils;

/**
 * handles data from hstopd and pus it to socket.
 * 
 * @author Felix Bechstein
 * 
 */
public class SessionOut implements Runnable {
	private DataOutputStream os;

	private Session s;

	public SessionOut(DataOutputStream os, Session s) {
		this.s = s;
		this.os = os;
	}

	public void terminate() {
		try {
			os.close();
		} catch (IOException e) {
			Utils.error("out.terminate: " + e.toString());
		}
	}

	public void run() {
		// TODO: tcp/udp?
		if (s.type != Tunnel.TYPE_TCP)
			return;

		HttpConnection c = null;

		boolean first = true;

		String url = jhstopc.midlet.settings.getURL() + "?i=" + s.id;

		while (s.alive) {
			try {
				String r = TunnelHandler.genRand();
				if (first) {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand() + "&t=tcp&h=" + s.host
							+ "&p=" + s.port + "&z=no");
					first = false;
				} else {
					c = (HttpConnection) Connector.open(url + "&b=" + r);
				}

				if (jhstopc.midlet.settings.getPwd().length() > 0) {
					c.setRequestProperty("Authorization", "Basic "
							+ BasicAuth.encode(jhstopc.midlet.settings.getUser(), jhstopc.midlet.settings.getPwd()));
				}
				if (jhstopc.midlet.settings.getAgent().length() > 0) {
					c.setRequestProperty("Agent", jhstopc.midlet.settings.getAgent());
				}
				
				Utils.db("out waiting: " + r);
				
				int resp = c.getResponseCode();
				if (resp != HttpConnection.HTTP_OK) {
					Utils.db("error out: " + resp);
					s.terminate();
				} else {
					InputStream is = c.openInputStream();

					byte[] buf;
					//int bufsize = is.available();
					int bufsize = Integer.parseInt(c.getHeaderField("Content-Length"));
					if (bufsize > 0) {
						buf = new byte[bufsize];
						bufsize = is.read(buf, 0, bufsize);
						if (bufsize > 0) {
							Utils.debug("out "+bufsize + " > " + new String(buf));
							s.stats.addOut(bufsize);
							// TODO: unzip
							try {
								os.write(buf, 0, bufsize);
								os.flush();
							} catch (IOException e) {
								Utils.error("error writing data to socket. terminating session");
								s.terminate();
							}
						}
					}
					is.close();
				}

			} catch (Exception e) {
				Utils.error("out: " + e.toString());
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					Utils.error("out: " + e1.toString());
				}
			}
			System.gc();
		}

		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Utils.db("terminate: out run end");
	}
}
