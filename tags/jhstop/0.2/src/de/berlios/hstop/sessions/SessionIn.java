package de.berlios.hstop.sessions;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.berlios.hstop.midlet.jhstopc;
import de.berlios.hstop.tools.Utils;

/**
 * handles data from socket and pushs it to hstopd.
 * 
 * @author Felix Bechstein
 * 
 */
public class SessionIn implements Runnable {
	private DataInputStream is;

	private Session s;

	public SessionIn(DataInputStream is, Session s) {
		this.is = is;
		this.s = s;
	}

	public void terminate() {
		try {
			is.close();
		} catch (IOException e) {
			Utils.error("in.terminate: " + e.toString());
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
		int offset = 0;
		boolean first = true;

		String url = jhstopc.midlet.settings.getURL() + "?i=" + s.id;

		while (s.alive) {
			try {
				bufsize = is.available();

				if (bufsize < 1) {
					offset = is.read(buf, 0, 1);
					bufsize = is.available();

					if (bufsize < 1) {
						try {
							Thread.sleep(200);
						} catch (Exception e) {
						}
						bufsize = is.available();
					}
				} else
					offset = 0;

				while (bufsize > 0 && offset < buf.length) {
					if (bufsize > buf.length - offset)
						bufsize = buf.length - offset;
					offset += is.read(buf, offset, bufsize);
					bufsize = is.available();
					if (bufsize < 1 && offset < buf.length && offset % 256 == 0) {
						Thread.sleep(200);
						bufsize = is.available();
					}
				}
				
				bufsize = offset;
				
				s.stats.addIn(bufsize);
				Utils.debug("in "+bufsize + " > " + new String(buf).substring(0, bufsize -1));

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
				//c.setRequestProperty("Content-Length", "" + bufsize);

				OutputStream os = c.openOutputStream();
				c.setRequestMethod(HttpConnection.POST);
				os.write(buf, 0, bufsize);
				os.close();
				
				Utils.db("in waiting for response");
				int resp = c.getResponseCode();
				if (resp != HttpConnection.HTTP_OK) {
					Utils.db("error in" + resp);
					s.terminate();
				}
			} catch (IOException e) {
				Utils.error("in io error: leaving");
				s.terminate();
			} catch (Exception e) {
				Utils.error("in io error: leaving");
				Utils.error("in: " + e.toString());
				s.terminate();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					Utils.error("in: " + e1.toString());
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
