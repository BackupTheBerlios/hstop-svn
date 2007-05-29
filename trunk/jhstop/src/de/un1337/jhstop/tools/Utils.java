package de.un1337.jhstop.tools;

import javax.microedition.midlet.MIDlet;

import net.sf.microlog.Level;
import net.sf.microlog.Logger;
import net.sf.microlog.appender.ConsoleAppender;
import net.sf.microlog.appender.FileAppender;
import net.sf.microlog.util.Properties;

public class Utils {

	static Properties properties;

	static Logger log = Logger.getLogger();

	public static void db(String str) {
		//System.err.println("Msg: " + str);
		debug(str);
	}

	public static void info(String str) {
		log.info(str);
	}

	public static void debug(String str) {
		log.debug(str);
	}

	public static void error(String str) {
		log.error(str);
	}

	public static void startLogger(MIDlet m) {
		ConsoleAppender consoleAppender = new ConsoleAppender();
		log.addAppender(consoleAppender);
		FileAppender fileAppendender = new FileAppender();
		log.addAppender(fileAppendender);
		log.setLogLevel(Level.DEBUG);
		log.info("Setup of log finished");
	}
}
