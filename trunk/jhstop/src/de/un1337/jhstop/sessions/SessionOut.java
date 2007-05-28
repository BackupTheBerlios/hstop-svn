package de.un1337.jhstop.sessions;

import java.io.DataOutputStream;
import java.io.IOException;

import de.un1337.jhstop.midlet.Settings;

/**
 * handles data from hstopd and pus it to socket.
 * 
 * @author flx
 * 
 */
public class SessionOut implements Runnable {
	private DataOutputStream os;

	private Settings settings;

	public SessionOut(Settings settings, DataOutputStream os) {
		this.os = os;
		this.settings = settings;
		// TODO: fillme
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
