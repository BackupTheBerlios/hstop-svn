package de.un1337.jhstop.sessions;

import java.io.DataInputStream;
import java.io.IOException;

import de.un1337.jhstop.midlet.Settings;

/**
 * handles data from socket and pushs it to hstopd.
 * 
 * @author flx
 * 
 */
public class SessionIn implements Runnable {
	private DataInputStream is;

	private Settings settings;

	public SessionIn(Settings settings, DataInputStream is) {
		this.is = is;
		this.settings = settings;
		// TODO: fillme

	}

	public void run() {
		// TODO Auto-generated method stub

		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
