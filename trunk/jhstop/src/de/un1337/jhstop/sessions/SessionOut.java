package de.un1337.jhstop.sessions;

import java.io.DataOutputStream;
import java.io.IOException;

import de.un1337.jhstop.midlet.Settings;

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

	public SessionOut(Settings settings, DataOutputStream os, String host, int port, int type) {
		this.os = os;
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
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
