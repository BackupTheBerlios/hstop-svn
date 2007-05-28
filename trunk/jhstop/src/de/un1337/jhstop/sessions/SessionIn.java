package de.un1337.jhstop.sessions;

import java.io.DataInputStream;
import java.io.IOException;

import de.un1337.jhstop.midlet.Settings;

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

	public SessionIn(Settings settings, DataInputStream is, String host, int port, int type) {
		this.is = is;
		this.settings = settings;
		this.host = host;
		this.port = port;
		this.type = type;
		// TODO: fillme
	}

	public void run() {
		// TODO: tcp/udp?
		if (this.type != Tunnel.TYPE_TCP)
			return;

		// TODO Auto-generated method stub

		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
