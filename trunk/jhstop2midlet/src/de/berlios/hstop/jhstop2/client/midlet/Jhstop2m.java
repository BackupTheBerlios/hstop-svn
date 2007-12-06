/**
 * 
 */
package de.berlios.hstop.jhstop2.client.midlet;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * @author flx
 * 
 */
public class Jhstop2m extends MIDlet implements CommandListener {
	public static final Command cmdBack = new Command("Back", Command.BACK, 0);

	public static final Command cmdOK = new Command("OK", Command.OK, 1);

	public static final Command cmdSettings = new Command("Settings", Command.SCREEN, 5);

	public static final Command cmdForward = new Command("Forwarding", Command.SCREEN, 6);

	public static final Command cmdAbout = new Command("About", Command.SCREEN, 7);

	public static final Command cmdTest = new Command("Test", Command.SCREEN, 15);

	public static final Command cmdAdd = new Command("add", Command.ITEM, 9);

	public static final Command cmdDel = new Command("del", Command.ITEM, 10);

	public static final Command cmdLogs = new Command("Logs", Command.SCREEN, 17);

	public static final Command cmdPing = new Command("Ping", Command.SCREEN, 16);

	static final Command cmdExit = new Command("Exit", Command.EXIT, 8);
	
	public Display display; // The display for this MIDlet

	public Form formMain = null;

	public Form formSettings = null;

	public Form formForward = null;

	public Form formAbout = null;
	
	public static String NAME_RS_SETTINGS = "JHSTOP_settings";

	static final String NAME = "jhstop2m";

	static final String AUTHOR = "Felix Bechstein";

	static final String MAIL = "flx@users.berlios.de";

	static final String VERSION = "$HEAD$";

	static final String LICENSE = "GPL";

	static final String ABOUT_STRING = NAME + "\nVersion: " + VERSION + "\nAuthor: " + AUTHOR + "\nSupport: " + MAIL
			+ "\nLicense: " + LICENSE;
	
	static public String PROXY = "proxy.arcor-ip.de:8080";
	
	public static Jhstop2m midlet = null;
	
	public Jhstop2m() {
		display = Display.getDisplay(this);
		Jhstop2m.midlet = this;
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// TODO Auto-generated method stub

	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
		formMain = new Form("jhstop2m");
		formAbout = new Form("about");

		formMain.addCommand(cmdExit);
		//formMain.addCommand(cmdSettings);
		//formMain.addCommand(cmdForward);
		formMain.addCommand(cmdAbout);
		formMain.addCommand(cmdTest);
		//formMain.addCommand(cmdLogs);
		//formMain.addCommand(cmdPing);
		
		formMain.setCommandListener(this);
		formAbout.setCommandListener(this);

		formAbout.addCommand(cmdBack);
		formAbout.append(ABOUT_STRING);
		
		display.setCurrent(formMain);
	}

	public void commandAction(Command arg0, Displayable arg1) {
		if (arg0.getLabel() == cmdExit.getLabel()) {
			try {
				destroyApp(true);
				notifyDestroyed();
			} catch (MIDletStateChangeException e) {
			}
		} else if (arg0.getLabel() == cmdOK.getLabel()) {
				display.setCurrent(formMain);
		} else if (arg0.getLabel() == cmdBack.getLabel()) {
				display.setCurrent(formMain);
		}  else if (arg0.getLabel() == cmdAbout.getLabel()) {
			display.setCurrent(formAbout);
		}  else if (arg0.getLabel().compareTo(cmdTest.getLabel()) == 0) {
			new Thread(new Tester()).start();
		} 
	}

}
