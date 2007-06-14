package de.berlios.hstop.midlet;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import de.berlios.hstop.items.SaveTextField;
import de.berlios.hstop.tools.Utils;

public class Forwardings extends SettingForm {

	private static String NAME_FORWARDING = "+for";

	public Forwardings(Display d, Form mainform, jhstopc listener, String name) {
		super(d, mainform, listener, name, jhstopc.NAME_RS_SETTINGS);

		form = new Form("forwardings");
		form.addCommand(jhstopc.cmdAdd);
		form.addCommand(jhstopc.cmdBack);
		form.setCommandListener(listener);

		loadFromFile();
	}

	public void addField() {
		SaveTextField stf = new SaveTextField("forward", "1337:localhost:1337:tcp", 50, TextField.ANY, NAME_FORWARDING);
		stf.addCommand(jhstopc.cmdDel);
		stf.setDefaultCommand(jhstopc.cmdOK);
		stf.setItemCommandListener(this);
		form.append(stf);
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

				if (dataString.startsWith(NAME_FORWARDING)) {
					SaveTextField stf = new SaveTextField("forward",
							dataString.substring(NAME_FORWARDING.length() + 1), 50, TextField.ANY, NAME_FORWARDING);
					// stf.setRSid(i);

					stf.addCommand(jhstopc.cmdDel);
					stf.setDefaultCommand(jhstopc.cmdOK);
					stf.setItemCommandListener(this);
					form.append(stf);
				} else if (dataString.startsWith("+")) {
					rs.deleteRecord(i);
				}
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

	public void commandAction(Command cmd, Item itm) {
		__commandAction(cmd, itm);
		if (cmd.getLabel().compareTo(jhstopc.cmdDel.getLabel()) == 0) {
			for (int i = 0; i < form.size(); i++) {
				if (form.get(i).equals(itm))
					form.delete(i);
			}
		}
	}
	
	public String getForwards() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < form.size(); i++){
			buf.append(( (SaveTextField)form.get(i)).getData());
			buf.append('#');
		}
		return buf.toString();
	}
}
