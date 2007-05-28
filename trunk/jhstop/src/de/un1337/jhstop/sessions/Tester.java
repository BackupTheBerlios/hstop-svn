package de.un1337.jhstop.sessions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import de.un1337.jhstop.tools.Utils;

public class Tester implements Runnable {
	public Tester() {

	}

	/**
	 * connects to localhost19887 and echos everything.
	 */
	public void run() {
		SocketConnection sc;
		try {
			sc = (SocketConnection) Connector.open("socket://localhost:19887");
			sc.setSocketOption(SocketConnection.LINGER, 5);
			InputStream is = sc.openInputStream();
			OutputStream os = sc.openOutputStream();
			// os.write("\r\n".getBytes());
			int ch = 0;
			while (ch != -1) {
				ch = is.read();
				os.write(ch);
			}
			is.close();
			os.close();
			sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Utils.db("end Tester");

	}

}
