package de.un1337.jhstop.sessions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import de.un1337.jhstop.items.StatsField;
import de.un1337.jhstop.midlet.jhstopc;
import de.un1337.jhstop.tools.Utils;

public class Tester implements Runnable {

	private StatsField stats = null;

	public Tester() {
		stats = new StatsField("test");
		jhstopc.midlet.formMain.append(stats);
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
			os.write("\r\n---------------------------\r\n".getBytes());
			byte[] buf = new byte[jhstopc.BUFSIZE];
			int ch = 0;
			while (ch != -1) {
				ch = is.read(buf);
				if (ch > 0) {
					stats.addOut(ch);
					stats.addIn(ch);
					os.write(buf, 0, ch);
				}
			}
			is.close();
			os.close();
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < jhstopc.midlet.formMain.size(); i++) {
			if (jhstopc.midlet.formMain.get(i).getLabel().compareTo(stats.getLabel()) == 0) {
				jhstopc.midlet.formMain.delete(i);
				break;
			}
		}
		Utils.db("end Tester");

	}

}
