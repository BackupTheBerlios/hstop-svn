package de.berlios.hstop.items;

import javax.microedition.lcdui.TextField;

import de.berlios.hstop.midlet.jhstopc;

public class StatsField extends TextField {

	private int statsIn = 0;

	private int statsOut = 0;

	private StatsField overall = null;

	private String debug = "";

	public StatsField(String sessionID) {
		super(sessionID, "", 25, TextField.UNEDITABLE);
		if (jhstopc.midlet.stats != null)
			overall = jhstopc.midlet.stats;
	}

	public void addIn(int amount) {
		statsIn += amount;
		update();
		if (overall != null)
			overall.addIn(amount);
	}

	public void addOut(int amount) {
		statsOut += amount;
		update();
		if (overall != null)
			overall.addOut(amount);
	}

	private void update() {
		this.setString(statsOut + " / " + statsIn + " : " + debug);
	}

	public void setDebug(String debug) {
		this.debug = debug;
		update();
	}
}
