package de.un1337.jhstop.sessions;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.un1337.jhstop.midlet.jhstopc;
import de.un1337.jhstop.tools.Utils;
import de.un1337.jhstop.tools.Waiter;

/**
 * handles data from socket and pushs it to hstopd.
 * 
 * @author Felix Bechstein
 * 
 */
public class SessionIn implements Runnable {
	private DataInputStream is;

	private boolean alive;

	private Waiter waiter;

	private Session s;

	public SessionIn(DataInputStream is, Session s) {
		this.is = is;
		this.alive = true;
		this.waiter = new Waiter();
		this.s = s;
	}

	public void terminate() {
		alive = false;
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		// TODO: tcp/udp?
		if (s.type != Tunnel.TYPE_TCP)
			return;
		HttpConnection c = null;

		byte[] buf;
		buf = new byte[jhstopc.BUFSIZE];
		int bufsize = 0;

		boolean first = true;

		String url = jhstopc.midlet.settings.getURL() + "?i=" + s.id;

		while (alive) {

			try {
				bufsize = is.available();

				if (bufsize < 1) {
					waiter.sleep();
					continue;
				}
				waiter.reduce();
				if (bufsize > buf.length)
					bufsize = buf.length;

				bufsize = is.read(buf, 0, bufsize);

				s.stats.addIn(bufsize);
				Utils.db("in: " + bufsize);

				if (first) {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand() + "&t=tcp&h=" + s.host
							+ "&p=" + s.port + "&z=no");
					first = false;
				} else {
					c = (HttpConnection) Connector.open(url + "&b=" + TunnelHandler.genRand());
				}

				c.setRequestMethod(HttpConnection.POST);
				// + ";CertificateErrorHandling=warn" +
				// ";HandshakeCommentary=on");

				if (jhstopc.midlet.settings.getPwd().length() > 0) {
					c.setRequestProperty("Authorization", "Basic "
							+ BasicAuth.encode(jhstopc.midlet.settings.getUser(), jhstopc.midlet.settings.getPwd()));
				}
				if (jhstopc.midlet.settings.getAgent().length() > 0) {
					c.setRequestProperty("Agent", jhstopc.midlet.settings.getAgent());
				}
				c.setRequestProperty("Content-Length", "" + bufsize);

				OutputStream os = c.openOutputStream();
				c.setRequestMethod(HttpConnection.POST);
				os.write(buf, 0, bufsize);

				if (c.getResponseCode() != HttpConnection.HTTP_OK) {
					Utils.db("error in" + c.getResponseCode());
					s.terminate();
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
			e.printStackTrace();
		}

		Utils.db("terminate: in run end");
	}

}
