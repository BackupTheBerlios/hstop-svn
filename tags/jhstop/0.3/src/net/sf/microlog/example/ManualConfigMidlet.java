package net.sf.microlog.example;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import net.sf.microlog.Level;
import net.sf.microlog.Logger;
import net.sf.microlog.appender.ConsoleAppender;
import net.sf.microlog.appender.RecordStoreAppender;


/**
 * An example midlet that shows how to do the manual configuration.
 * @author Johan Karlsson
 *
 */
public class ManualConfigMidlet extends MIDlet {
	
	private final static Logger log = Logger.getLogger();
	
	public ManualConfigMidlet() {
		super();
		ConsoleAppender consoleAppender = new ConsoleAppender();
		log.addAppender(consoleAppender);
		RecordStoreAppender rsAppender = new RecordStoreAppender();
		log.addAppender(rsAppender);
		log.setLogLevel(Level.DEBUG);
		log.info("Setup of log finished");
	}

	protected void startApp() throws MIDletStateChangeException {
		log.info("Starting app");
	}

	protected void pauseApp() {
		log.info("Pausing app");

	}

	protected void destroyApp(boolean conditional) throws MIDletStateChangeException {
		log.info("Destroying app");
	}

}
