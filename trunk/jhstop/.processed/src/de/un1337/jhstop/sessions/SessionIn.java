package de.un1337.jhstop.sessions;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.un1337.jhstop.items.StatsField;
import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.midlet.jhstopc;
import de.un1337.jhstop.tools.Utils;

/**
 * handles data from socket and pushs it to hstopd.
 * 
 * @author Felix Bechstein
 * 
 */
public class SessionIn implements Runnable {
	private DataInputStream is;

	private Settings settings;

	private String host;

	private int port;

	private int type;

	private boolean alive;

	private String id;

	private StatsField stats = null;

	private SessionOut out = null;

	public SessionIn(String sessionID, Settings settings, DataInputStream is, String host, int port, int type,
			StatsField stats) {
		this.is = is;
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
		if (recursive && (out != null))
			out.terminate(false);
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
				bufsize = is.read(buf);

				if (bufsize <= 0)
					continue;

				stats.addIn(bufsize);
				Utils.db("in: " + bufsize);

				if (first) {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand() + "&t=tcp&h=" + this.host
							+ "&p=" + this.port + "&z=no");
					first = false;
				} else {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand());
				}

				c.setRequestMethod(HttpConnection.POST);
				// + ";CertificateErrorHandling=warn" +
				// ";HandshakeCommentary=on");

				if (settings.getPwd().length() > 0) {
					c.setRequestProperty("Authorization", "Basic "
							+ BasicAuth.encode(settings.getUser(), settings.getPwd()));
				}
				if (settings.getAgent().length() > 0) {
					c.setRequestProperty("Agent", settings.getAgent());
				}
				c.setRequestProperty("Content-Length", "" + buf.length);

				OutputStream os = c.openOutputStream();
				c.setRequestMethod(HttpConnection.POST);
				os.write(buf, 0, bufsize);

				if (c.getResponseCode() != HttpConnection.HTTP_OK) {
					Utils.db("error in" + c.getResponseCode());
					terminate();
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
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setOut(SessionOut out) {
		this.out = out;
	}

}
