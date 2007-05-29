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

	private boolean alive;

	private Session s;

	public SessionOut(DataOutputStream os, Session s) {
		this.alive = true;
		this.s = s;
		this.os = os;
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
		Utils.debug("tick: out");
		// TODO: tcp/udp?
		if (s.type != Tunnel.TYPE_TCP)
			return;

		HttpConnection c = null;

		boolean first = true;

		String url = jhstopc.midlet.settings.getURL() + "?i=" + s.id;

		while (alive) {
			Utils.debug("tick: out");
			try {
				if (first) {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand() + "&t=tcp&h=" + s.host
							+ "&p=" + s.port + "&z=no");
					first = false;
				} else {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand());
				}

				if (jhstopc.midlet.settings.getPwd().length() > 0) {
					c.setRequestProperty("Authorization", "Basic "
							+ BasicAuth.encode(jhstopc.midlet.settings.getUser(), jhstopc.midlet.settings.getPwd()));
				}
				if (jhstopc.midlet.settings.getAgent().length() > 0) {
					c.setRequestProperty("Agent", jhstopc.midlet.settings.getAgent());
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
			e.printStackTrace();
		}

		Utils.db("terminate: out run end");
	}
}
