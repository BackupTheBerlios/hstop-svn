package de.berlios.hstop.sessions;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.UDPDatagramConnection;

import de.berlios.hstop.tools.Utils;

public class DNS implements Runnable {

	boolean alive = true;

	private static byte[] answer = { (byte) 0xc0, (byte) 0x0c, // id
			(byte) 0x00, (byte) 0x01, // type: host
			(byte) 0x00, (byte) 0x01, // class: in
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, // ttl
			(byte) 0x00, (byte) 0x04, // datalength: 4
			(byte) 127, (byte) 0, (byte) 0, (byte) 1 // ip: 127.0.0.1
	};

	public void run() {

		try {
			UDPDatagramConnection uc = (UDPDatagramConnection) Connector.open("datagram://:53", Connector.READ_WRITE,
					false);

			while (alive) {
				Datagram dg = uc.newDatagram(0x80);
				uc.receive(dg);
				byte[] data = dg.getData();
				int len = dg.getLength();
				// Utils.debug(new String(data));
				Utils.debug("dns: new request");
				data[0x07] = (byte) 0x1;
				byte[] response = new byte[data.length + answer.length];
				for (int i = 0; i < len; i++) {
					response[i] = data[i];
				}
				for (int i = 0; i < answer.length; i++) {
					response[len + i] = answer[i];
				}

				Datagram dgr = uc.newDatagram(len + answer.length);
				dgr.setData(response, 0, len + answer.length);
				dgr.setAddress(dg);
				uc.send(dgr);
			}

		} catch (IOException e) {
			Utils.debug("dns: " + e.toString());
		}
	}

	public void terminate() {
		alive = false;
	}

}
