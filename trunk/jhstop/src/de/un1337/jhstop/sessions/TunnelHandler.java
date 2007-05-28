package de.un1337.jhstop.sessions;

import java.util.Vector;

import javax.microedition.lcdui.TextField;

import de.un1337.jhstop.midlet.Settings;
import de.un1337.jhstop.tools.Utils;

public class TunnelHandler {

	private TextField fieldCounter = null;

	private Settings settings = null;

	private Vector tunnels = null;

	public TunnelHandler(Settings settings, TextField fieldCounter) {
		this.fieldCounter = fieldCounter;
		this.settings = settings;
		tunnels = new Vector();
	}

	public void remove(String tunnelID) {
		Tunnel s = get(tunnelID);
		if (s == null)
			return;
		s.terminate();
		tunnels.removeElement(s);
		updateCounter();
	}

	public Tunnel get(String tunnelID) {
		for (int i = 0; i < tunnels.size(); i++) {
			if (tunnelID == tunnels.elementAt(i).toString()) {
				return (Tunnel) tunnels.elementAt(i);
			}
		}
		return null;
	}

	public Tunnel add(String tunnelID) {
		Tunnel s = get(tunnelID);
		if (s != null)
			return s;
		s = new Tunnel(tunnelID, settings);
		new Thread(s).start();
		tunnels.addElement(s);
		updateCounter();
		return null;
	}

	public void clean(String tunnels) {
		Vector tmp = new Vector();
		int i = 0;
		int j = 0;
		while (i < tunnels.length()) {
			j = tunnels.indexOf("#", i);
			if (j < 0)
				break;
			String s = tunnels.substring(i, j);
			tmp.addElement(s);
			i = j + 1;
		}

		// del old tunnels
		i = 0;
		while (i < this.tunnels.size()) {
			boolean found = false;
			for (j = 0; j < tmp.size(); j++) {
				if (this.tunnels.elementAt(i).toString().compareTo(tmp.elementAt(j).toString()) == 0) {
					found = true;
					break;
				}
			}
			if (found)
				++i;
			else {
				Utils.db("rm: " + this.tunnels.elementAt(i));
				this.tunnels.removeElementAt(i);
			}
		}

		// add new tunnels
		i = 0;
		while (i < tmp.size()) {
			boolean found = false;
			for (j = 0; j < this.tunnels.size(); j++) {
				if (tmp.elementAt(i).toString().compareTo(this.tunnels.elementAt(j).toString()) == 0) {
					found = true;
					break;
				}
			}
			if (!found) {
				Utils.db("add: " + tmp.elementAt(i));
				this.add(tmp.elementAt(i).toString());
			}
			++i;
		}
	}

	public void terminate() {
		clean("");
	}

	private void updateCounter() {
		if (fieldCounter == null)
			return;
		fieldCounter.setString(tunnels.size() + "");
	}

}
