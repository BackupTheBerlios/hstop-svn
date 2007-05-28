package de.un1337.jhstop.sessions;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.un1337.jhstop.items.StatsField;
import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.midlet.jhstopc;
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

	private StatsField stats = null;

	private SessionIn in = null;

	public SessionOut(String sessionID, Settings settings, DataOutputStream os, String host, int port, int type,
			StatsField stats) {
		this.os = os;
		this.settings = settings;
		this.host = host;
		this.port = port;
		this.type = type;
		this.alive = true;
		this.id = sessionID;
		this.stats = stats;
	}


	/**
	 * 
	 *@deprecated
	 */
	public void terminate() {
		terminate(true);
	}

	public void terminate(boolean recursive) {
		alive = false;
		if (stats != null) {
			for (int i = 0; i < jhstopc.midlet.formMain.size(); i++) {
				if (jhstopc.midlet.formMain.get(i).getLabel().compareTo(stats.getLabel()) == 0) {
					jhstopc.midlet.formMain.delete(i);
					break;
				}
			}
		}
		if (recursive && (in != null))
			in.terminate(false);
	}

	public void run() {
		// TODO: tcp/udp?
		if (this.type != Tunnel.TYPE_TCP)
			return;

		HttpConnection c = null;

		byte[] buf;
		buf = new byte[jhstopc.BUFSIZE];
		int bufsize = 0;

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
					terminate(true);
				} else {
					InputStream is = c.openInputStream();
					bufsize = is.read(buf);
					if (bufsize > 0) {
						Utils.db("out: " + bufsize);
						stats.addOut(bufsize);
						// TODO: unzip
						try {
						os.write(buf, 0, bufsize);
						os.flush();
						} catch (Exception e) {
							stats.addOut(10000);
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
	}
	
	public void setIn(SessionIn in) {
		this.in = in;		
	}
}
