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
			StatsField statsf = new StatsField(host + ":" + port + " - " + i);
			jhstopc.midlet.formMain.append(statsf);
			sout = new SessionOut(i, settings, os, host, port, type, statsf, this);
			sin = new SessionIn(i, settings, is, host, port, type, statsf, this);

			new Thread(sin).start();
			new Thread(sout).start();

			sin.setOut(sout);
			sout.setIn(sin);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void terminate() {
		sin.terminate(false);
		sout.terminate(false);
		try {
			this.sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
