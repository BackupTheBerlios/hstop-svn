package de.un1337.items;

import javax.microedition.lcdui.ChoiceGroup;

import de.un1337.jhstop.tools.utils;

public class SaveChoiceGroup extends ChoiceGroup implements Saveable {
	private String data;

	private String rsStr = null;

	private int rsID = -1;

	/**
	 * 
	 * @param label
	 * @param constraints
	 * @param rsStr
	 */
	public SaveChoiceGroup(String label, int constraints, String rsStr) {
		super(label, constraints);
		data = null;
		this.rsStr = rsStr;
	}

	public void save() {
		try {
			data = getString(this.getSelectedIndex());
		} catch (Exception e) {
			data = null;
		}
	}

	public void load() {
		try {
			for (int i = 0; i < this.size(); i++) {
				if (this.getString(i).compareTo(data) == 0) {
					setSelectedIndex(i, true);
				}
			}
		} catch (java.lang.NullPointerException e) {
		}
	}

	public String getData() {
		return data;
	}

	public void loadFromString(String s) {
		data = s;
		utils.db("loaded from string: " + s);
	}

	public String saveToString() {
		if (data == null)
			return "NULL";
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
