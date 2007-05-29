package de.berlios.hstop.midlet;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import de.berlios.hstop.items.Saveable;
import de.berlios.hstop.tools.Utils;

public class SettingForm implements ItemCommandListener {

	private Display display = null;

	private Form formMain = null;

	protected Form form = null;

	protected String rsName;

	protected RecordStore rs = null;

	protected static String SEPERATOR = ":";

	protected jhstopc listener = null;

	/**
	 * 
	 * @param d
	 *            dsplay
	 * @param mainform
	 * @param listener
	 * @param name
	 * @param rsName
	 */
	public SettingForm(Display d, Form mainform, jhstopc listener, String name, String rsName) {
		Utils.db("spawn");
		display = d;
		formMain = mainform;
		this.listener = listener;
		form = new Form(name);
		// form.addCommand(jhstopc.cmdOK);
		form.addCommand(jhstopc.cmdBack);
		form.setCommandListener(this.listener);

		this.rsName = rsName;
		// loadFromFile();
	}

	public void display() {
		Utils.db("display");
		load();
		display.setCurrent(form);
	}

	public void hide(boolean save) {
		Utils.db("close");
		if (save)
			save();
		display.setCurrent(formMain);
	}

	public void close() {
		saveToFile();
		try {
			rs.closeRecordStore();
		} catch (Exception e) {
		}
	}

	public boolean isMe(Displayable d) {
		return (d.getTitle() == form.getTitle());
	}

	public String getTitle() {
		return form.getTitle();
	}

	private void save() {
		Utils.db("save");
		int i = 0;
		while (i < form.size()) {
			try {
				Saveable s = (Saveable) form.get(i);
				s.save();
			} catch (java.lang.ClassCastException e) {
				// do nothing
			}
			++i;
		}
	}

	private void load() {
		Utils.db("load");
		int i = 0;
		while (i < form.size()) {
			try {
				Saveable s = (Saveable) form.get(i);
				s.load();
			} catch (java.lang.ClassCastException e) {
				// do nothing
			}
			++i;
		}
	}

	protected void loadFromFile() {
		try {
			// Create the record store if it does not exist
			// RecordStore.deleteRecordStore(this.rsName);
			rs = RecordStore.openRecordStore(this.rsName, true);
		} catch (Exception e) {
			Utils.db(e.toString());
			Utils.db(this.rsName);
			return;
		}

		try {
			Utils.db("in store: " + rs.getNumRecords());

			byte[] dataBytes = new byte[50];
			int dataLen;
			String dataString;
			int er = 0;
			for (int i = 1; i <= rs.getNumRecords(); i++) {
				dataLen = -1;
				boolean fetched = false;
				while (!fetched) {
					try {
						dataLen = rs.getRecord(i + er, dataBytes, 0);
						fetched = true;
					} catch (InvalidRecordIDException e) {
						++er;
					}
				}
				dataString = new String(dataBytes, 0, dataLen);
				boolean found = false;
				for (int j = 0; j < form.size(); j++) {
					try {
						Saveable s = (Saveable) form.get(j);
						Utils.db("check: " + dataString + " starts with " + s.getRSstr());
						if (s.getRSid() < 0 && dataString.startsWith(s.getRSstr())) {
							s.loadFromString(dataString.substring(s.getRSstr().length() + 1));
							// s.setRSid(j);
							found = true;
							break;
						}
					} catch (java.lang.ClassCastException e) {
						// do nothing
					}
				}
				if (!found && !dataString.startsWith("+"))
					rs.deleteRecord(i);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			rs.closeRecordStore();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveToFile() {
		try {
			// Create the record store if it does not exist
			// RecordStore.deleteRecordStore(this.rsName);
			rs = RecordStore.openRecordStore(this.rsName, true);
		} catch (Exception e) {
			Utils.db(e.toString());
			Utils.db(this.rsName);
			return;
		}

		byte[] rec;
		int i = 0;
		while (i < form.size()) {
			try {
				Saveable s = (Saveable) form.get(i);
				rec = (s.getRSstr() + SEPERATOR + s.saveToString()).getBytes();
				Utils.db("save: " + (s.getRSstr() + SEPERATOR + s.saveToString()));
				Utils.db(s.getRSid() + "");
				if (s.getRSid() < 0) {
					try {
						s.setRSid(rs.addRecord(rec, 0, rec.length));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						rs.setRecord(s.getRSid(), rec, 0, rec.length);
					} catch (javax.microedition.rms.InvalidRecordIDException e) {
						try {
							s.setRSid(rs.addRecord(rec, 0, rec.length));
						} catch (Exception e2) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (java.lang.ClassCastException e) {
				// do nothing
			}
			++i;
		}
		try {
			rs.closeRecordStore();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void __commandAction(Command cmd, Item itm) {
		Utils.db(cmd.getLabel() + "-----" + itm.getLabel());
		Utils.db(cmd.toString() + "-----" + itm.toString());
		if (cmd.getLabel().compareTo(jhstopc.cmdOK.getLabel()) == 0) {
			listener.commandAction(cmd, form);
		}
	}

	public void commandAction(Command cmd, Item itm) {
		__commandAction(cmd, itm);
	}
}
