package de.un1337.jhstop.sessions;

import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.tools.Utils;

public class Tunnel {

	public static int TYPE_TCP = 1;

	public static int TYPE_UDP = 2;

	private String id = null;

	private Settings settings = null;

	private int localPort;

	private int port;

	private String host;

	private int type;

	public Tunnel(String tunnelID, Settings settings) {
		this.id = tunnelID;
		this.settings = settings;

		// defaults
		localPort = 12345;
		host = "127.0.0.1";
		port = localPort;
		type = TYPE_TCP;

		// parse tunnelID to lport:host:port:type
		int lpd = tunnelID.indexOf(":");
		int hd = tunnelID.indexOf(":", lpd + 1);
		int pd = tunnelID.indexOf(":", hd + 1);

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
	}

	public String toString() {
		return id;
	}
}
