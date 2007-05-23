package de.un1337.items;

import javax.microedition.lcdui.TextField;

import de.un1337.jhstop.tools.utils;

public class SaveTextField extends TextField implements Saveable {

	private String data;

	private String rsStr = null;

	private int rsID = -1;

	/**
	 * 
	 * @param label
	 * @param text
	 * @param maxSize
	 * @param constraints
	 * @param rsStr
	 */
	public SaveTextField(String label, String text, int maxSize, int constraints, String rsStr) {
		super(label, text, maxSize, constraints);
		data = text;
		if (rsStr == null)
			this.rsStr = "";
		else
			this.rsStr = rsStr;
	}

	public void save() {
		data = getString();
	}

	public void load() {
		setString(data);
	}

	public String getData() {
		return data;
	}

	public void loadFromString(String s) {
		data = s;
		utils.db("loaded from string: " + s);
	}

	public String saveToString() {
		return data;
	}

	public int getRSid() {
		// TODO Auto-generated method stub
		return rsID;
	}

	public String getRSstr() {
		return rsStr;
	}

	public void setRSid(int id) {
		rsID = id;
	}

	public void setRSstr(String id) {
		rsStr = id;
	}

}
