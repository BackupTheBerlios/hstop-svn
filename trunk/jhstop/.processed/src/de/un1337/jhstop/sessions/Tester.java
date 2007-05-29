package de.un1337.jhstop.sessions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import de.un1337.jhstop.items.StatsField;
import de.un1337.jhstop.midlet.jhstopc;
import de.un1337.jhstop.tools.Utils;
import de.un1337.jhstop.tools.Waiter;

public class Tester implements Runnable {

	private StatsField stats = null;

	private Waiter waiter;

	public Tester() {
		stats = new StatsField("test");
		jhstopc.midlet.formMain.append(stats);
		waiter = new Waiter();
	}

	/**
	 * connects to localhost19887 and echos everything.
	 */
	public void run() {
		SocketConnection sc;
		try {
			sc = (SocketConnection) Connector.open("socket://localhost:19887", Connector.READ_WRITE, true);
			sc.setSocketOption(SocketConnection.LINGER, 5);
			sc.setSocketOption(SocketConnection.DELAY, 1);
			sc.setSocketOption(SocketConnection.RCVBUF, jhstopc.BUFSIZE);
			sc.setSocketOption(SocketConnection.SNDBUF, jhstopc.BUFSIZE);
			InputStream is = sc.openInputStream();
			OutputStream os = sc.openOutputStream();

			stats.setDebug("+");

			os.write("\r\n---------------------------\r\n".getBytes());

			stats.setDebug("++");

			byte[] buf = new byte[jhstopc.BUFSIZE];
			int ch = 0;
			while (ch != -1) {

				stats.setDebug("+++");
				ch = is.available();
				if (ch < 1) {
					waiter.sleep();
					ch = is.read(buf, 0, ch);
					continue;
				}

				waiter.reduce();

				if (ch > buf.length)
					ch = buf.length;

				ch = is.read(buf, 0, ch);
				// ch = is.read();

				stats.setDebug("++--");

				if (ch > -1) {
					stats.addOut(ch);
					stats.addIn(ch);
					os.write(buf, 0, ch);
					// os.write(ch);
				}
			}

			stats.setDebug("++---");

			is.close();
			os.close();
			sc.close();
			stats.setDebug("++------");

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
