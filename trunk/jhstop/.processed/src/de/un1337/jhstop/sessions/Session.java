package de.un1337.jhstop.sessions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.io.SocketConnection;

import de.un1337.jhstop.items.StatsField;
import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.midlet.jhstopc;
import de.un1337.jhstop.tools.Utils;

public class Session {

	private SessionOut sout;

	private SessionIn sin;
	
	private SocketConnection sc;
	
	public StatsField stats;

	public Session(SocketConnection sc, Settings settings, String host, int port, int type) {
		try {
			this.sc = sc;
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
			String i = TunnelHandler.genID();
			stats = new StatsField(host + ":" + port + " - " + i);
			jhstopc.midlet.formMain.append(stats);
			sout = new SessionOut(i, settings, os, host, port, type, this);
			sin = new SessionIn(i, settings, is, host, port, type, this);

			new Thread(sin).start();
			new Thread(sout).start();

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void terminate() {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sc = null;
		
		Utils.db("terminate: terminated");
	}

}
