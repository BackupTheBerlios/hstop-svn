package de.berlios.hstop.sessions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.io.SocketConnection;

import de.berlios.hstop.items.StatsField;
import de.berlios.hstop.midlet.jhstopc;
import de.berlios.hstop.tools.Utils;

public class Session {

	private SessionOut sout;

	private SessionIn sin;

	private SocketConnection sc;

	public StatsField stats;

	public String host;

	public int port;

	public int type;

	public String id;

	public boolean alive;

	public Session(SocketConnection sc, String host, int port, int type) {
		Utils.debug("new session");
		alive = true;
		try {
			this.sc = sc;
			this.host = host;
			this.port = port;
			this.type = type;

			// Set application specific hints on the socket.
			this.sc.setSocketOption(SocketConnection.DELAY, 1);
			this.sc.setSocketOption(SocketConnection.LINGER, 5);
			this.sc.setSocketOption(SocketConnection.KEEPALIVE, 0);
			this.sc.setSocketOption(SocketConnection.RCVBUF, jhstopc.BUFSIZE);
			this.sc.setSocketOption(SocketConnection.SNDBUF, jhstopc.BUFSIZE);

			// Get the input stream of the connection.
			DataInputStream is = this.sc.openDataInputStream();
			// Get the output stream of the connection.
			DataOutputStream os = this.sc.openDataOutputStream();

			Utils.db("new tunnel: " + host + ":" + port);
			// push streams to session
			this.id = TunnelHandler.genID();
			stats = new StatsField(host + ":" + port + " - " + id);
			jhstopc.midlet.formMain.append(stats);
			sout = new SessionOut(os, this);
			sin = new SessionIn(is, this);
			Utils.debug("start thread: in");
			new Thread(sin).start();

			Utils.debug("start thread: out");
			new Thread(sout).start();

		} catch (Exception e) {
			Utils.error("session.start: " + e.toString());
		}
	}

	public void terminate() {
		if (!alive)
			return;
		alive = false;
		Utils.db("terminate:");
		sin.terminate();
		Utils.db("terminate: in terminated");
		sout.terminate();
		Utils.db("terminate: out terminated");

		if (stats != null) {
			for (int i = 0; i < jhstopc.midlet.formMain.size(); i++) {
				if (jhstopc.midlet.formMain.get(i).getLabel().compareTo(stats.getLabel()) == 0) {
					jhstopc.midlet.formMain.delete(i);
					break;
				}
			}
		}

		try {
			this.sc.close();
		} catch (IOException e) {
			Utils.info("session.sc.close() " + e.toString());
		}

		sc = null;

		Utils.db("terminate: terminated");
	}

}
