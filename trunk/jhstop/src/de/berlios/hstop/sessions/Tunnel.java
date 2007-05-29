package de.berlios.hstop.sessions;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import de.berlios.hstop.midlet.Settings;
import de.berlios.hstop.tools.Utils;

public class Tunnel implements Runnable {

	public static int TYPE_TCP = 1;

	public static int TYPE_UDP = 2;

	private String id = null;

	private int localPort;

	private int port;

	private String host;

	private int type;

	private ServerSocketConnection scn = null;

	private boolean alive;

	public Tunnel(String tunnelID, Settings settings) {
		this.id = tunnelID;

		// defaults
		localPort = 12345;
		host = "127.0.0.1";
		port = localPort;
		type = TYPE_TCP;

		// parse tunnelID to lport:host:port:type
		int lpd = tunnelID.indexOf(":");
		int hd = tunnelID.indexOf(":", lpd + 1);
		int pd = tunnelID.indexOf(":", hd + 1);
		this.alive = true;

		try {
			localPort = Integer.parseInt(tunnelID.substring(0, lpd).trim());
			host = tunnelID.substring(lpd + 1, hd).trim();
			port = Integer.parseInt(tunnelID.substring(hd + 1, pd).trim());
			if (tunnelID.substring(pd + 1).toUpperCase().trim().compareTo("UDP") == 0)
				type = TYPE_UDP;
		} catch (Exception e) {
			Utils.db(e.toString());
			this.terminate();
		}
	}

	public void terminate() {
		alive = false;
	}

	public String toString() {
		return id;
	}

	public void run() {
		// TODO: tcp/udp?
		if (this.type != TYPE_TCP)
			return;

		// listen to socket
		try {
			this.scn = (ServerSocketConnection) Connector.open("socket://:" + localPort, Connector.READ_WRITE, true);

			while (alive) {
				try {

					// Wait for a connection.
					SocketConnection sc = (SocketConnection) scn.acceptAndOpen();

					new Session(sc, this.host, this.port, this.type);

					// sc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			scn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
