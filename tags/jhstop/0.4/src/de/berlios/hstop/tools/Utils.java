package de.berlios.hstop.tools;

import javax.microedition.midlet.MIDlet;

import net.sf.microlog.Level;
import net.sf.microlog.Logger;
import net.sf.microlog.appender.ConsoleAppender;
import net.sf.microlog.appender.FormAppender;
import de.berlios.hstop.midlet.jhstopc;

public class Utils {
	
	static int MAXLOGSIZE = 128;

	static Logger log = Logger.getLogger();

	public static void db(String str) {
		debug(str);
	}

	public static void info(String str) {
		if (str.length() > MAXLOGSIZE)
			str = str.substring(0, MAXLOGSIZE);
		log.info(str);
		//System.err.println("info: " + str);
	}

	public static void debug(String str) {
		if (str.length() > MAXLOGSIZE)
			str = str.substring(0, MAXLOGSIZE);
		log.debug(str);
		//System.err.println("debug: " + str);
	}

	public static void error(String str) {
		if (str.length() > MAXLOGSIZE)
			str = str.substring(0, MAXLOGSIZE);
		log.error(str);
		//System.err.println("error: " + str);
	}

	public static void startLogger(MIDlet m) {
		jhstopc.midlet.logs.addCommand(jhstopc.cmdBack);
		jhstopc.midlet.logs.setCommandListener(jhstopc.midlet);
		FormAppender formAppender = new FormAppender(jhstopc.midlet.logs);
		log.addAppender(formAppender);
		ConsoleAppender consoleAppender = new ConsoleAppender();
		log.addAppender(consoleAppender);
		log.setLogLevel(Level.DEBUG);
		log.info("Setup of log finished");
	}
}
