package de.berlios.hstop.midlet;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStore;

import de.berlios.hstop.items.StatsField;
import de.berlios.hstop.sessions.Tester;
import de.berlios.hstop.sessions.TunnelHandler;
import de.berlios.hstop.tools.Utils;

/**
 * 
 * @author Felix Bechstein
 * @version $HEAD$
 * 
 */
public class jhstopc extends MIDlet implements CommandListener {
	
	public static final int BUFSIZE = 512;
	//public static final int BUFSIZE = 1;

	public static final Command cmdBack = new Command("Back", Command.BACK, 0);

	public static final Command cmdOK = new Command("OK", Command.OK, 1);

	public static final Command cmdSettings = new Command("Settings", Command.SCREEN, 5);

	public static final Command cmdForward = new Command("Forwarding", Command.SCREEN, 6);

	public static final Command cmdAbout = new Command("About", Command.SCREEN, 7);

	public static final Command cmdTest = new Command("Test", Command.SCREEN, 15);

	public static final Command cmdAdd = new Command("add", Command.ITEM, 9);

	public static final Command cmdDel = new Command("del", Command.ITEM, 10);

	static final Command cmdExit = new Command("Exit", Command.EXIT, 8);

	public static String NAME_RS_SETTINGS = "JHSTOP_settings";

	static final String NAME = "jhstopc";

	static final String AUTHOR = "Felix Bechstein";

	static final String MAIL = "flx@users.berlios.de";

	static final String VERSION = "$HEAD$";

	static final String LICENSE = "GPL";

	static final String ABOUT_STRING = NAME + "\nVersion: " + VERSION + "\nAuthor: " + AUTHOR + "\nSupport: " + MAIL
			+ "\nLicense: " + LICENSE;

	private Display display; // The display for this MIDlet

	public Form formMain = null;

	public Form formSettings = null;

	public Form formForward = null;

	public Form formAbout = null;

	public Settings settings = null;

	private Forwardings forwardings = null;

	private TunnelHandler tunnels = null;
	
	public static jhstopc midlet = null;
	
	public StatsField stats = null;

	public jhstopc() {
		display = Display.getDisplay(this);
		jhstopc.midlet = this;
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		tunnels.terminate();
	}

	protected void pauseApp() {
		// TODO Auto-generated method stub
	}

	protected void startApp() throws MIDletStateChangeException {
		Utils.startLogger(this);
		Utils.db("start");

		formMain = new Form("jhstopc");
		formAbout = new Form("about");

		formMain.addCommand(cmdExit);
		formMain.addCommand(cmdSettings);
		formMain.addCommand(cmdForward);
		formMain.addCommand(cmdAbout);
		formMain.addCommand(cmdTest);

		formMain.setCommandListener(this);
		formAbout.setCommandListener(this);

		formAbout.addCommand(cmdBack);

		formAbout.append(ABOUT_STRING);

		settings = new Settings(display, formMain, this, "settings");
		forwardings = new Forwardings(display, formMain, this, "forwardings");

		TextField tf = new TextField("active tunnels", "", 50, TextField.UNEDITABLE);
		formMain.append(tf);
		stats = new StatsField("overall traffic");
		formMain.append(stats);
		
		tunnels = new TunnelHandler(settings, tf);
		tunnels.clean(forwardings.getForwards());

		display.setCurrent(formMain);

	}

	public void commandAction(Command arg0, Displayable arg1) {
		Utils.db("command: " + arg0.getLabel() + " - displayable: " + arg1.getTitle());
		if (arg0.getLabel() == cmdExit.getLabel()) {
			try {
				Utils.db("stop");
				try {
					RecordStore.deleteRecordStore(NAME_RS_SETTINGS);
				} catch (Exception e) {
					e.printStackTrace();
				}
				settings.close();
				forwardings.close();
				destroyApp(true);
				notifyDestroyed();
			} catch (MIDletStateChangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (arg0.getLabel() == cmdOK.getLabel()) {
			// TODO: save settings
			if (settings.isMe(arg1)) {
				settings.hide(true);
			} else if (forwardings.isMe(arg1)) {
				forwardings.hide(true);
				tunnels.clean(forwardings.getForwards());
			} else
				display.setCurrent(formMain);
		} else if (arg0.getLabel() == cmdBack.getLabel()) {
			// TODO: cancle settings
			if (settings.isMe(arg1)) {
				settings.hide(false);
			} else if (forwardings.isMe(arg1)) {
				forwardings.hide(false);
				tunnels.clean(forwardings.getForwards());
			} else
				display.setCurrent(formMain);
		} else if (arg0.getLabel() == cmdSettings.getLabel()) {
			// display.setCurrent(formSettings);
			settings.display();
		} else if (arg0.getLabel() == cmdForward.getLabel()) {
			forwardings.display();
		} else if (arg0.getLabel() == cmdAbout.getLabel()) {
			display.setCurrent(formAbout);
		} else if (arg0.getLabel().compareTo(cmdAdd.getLabel()) == 0) {
			forwardings.addField();
		} else if (arg0.getLabel().compareTo(cmdTest.getLabel()) == 0) {
			new Thread(new Tester()).start();
		} else {
			Utils.db(arg0.toString() + "-----" + arg1.toString());
		}

	}
}
