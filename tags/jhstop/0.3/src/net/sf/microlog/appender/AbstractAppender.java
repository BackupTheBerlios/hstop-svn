package net.sf.microlog.appender;

import net.sf.microlog.Appender;
import net.sf.microlog.Formatter;
import net.sf.microlog.Level;
import net.sf.microlog.MicroLogException;
import net.sf.microlog.format.SimpleFormatter;


/**
 * This is the abstract super class of all the appenders.
 * 
 * @author Johan Karlsson
 * @since 0.1
 */
public abstract class AbstractAppender implements Appender {

	protected Formatter formatter = new SimpleFormatter();

	/**
	 * Do the logging.
	 * 
	 * @param level
	 *            the level at which the logging shall be done.
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the exception to log.
	 */
	public abstract void doLog(Level level, Object message, Throwable t);

	/**
	 * Clear the log.
	 * 
	 * @see net.sf.microlog.Appender#clearLog()
	 */
	public abstract void clearLog();

	/**
	 * Close the log.
	 * 
	 * @see net.sf.microlog.Appender#closeLog()
	 */
	public abstract void closeLog();

	/**
	 * Open the log.
	 * 
	 * @see net.sf.microlog.Appender#openLog()
	 */
	public abstract void openLog() throws MicroLogException;

	/**
	 * Set the formatter.
	 * 
	 * @see net.sf.microlog.Appender#setFormatter(net.sf.microlog.Formatter)
	 */
	public final void setFormatter(Formatter formatter) {
		this.formatter = formatter;
	}

	/**
	 * Get the formatter.
	 * 
	 * @see net.sf.microlog.Appender#getFormatter()
	 */
	public final Formatter getFormatter() {
		return formatter;
	}

}
