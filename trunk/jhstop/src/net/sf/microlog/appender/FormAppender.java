package net.sf.microlog.appender;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import net.sf.microlog.Level;

/**
 * An Appender that appends the logging to a Form.
 * 
 * Each logging is appended as a StringItem. The log is cleared by deleting all the items in the Form. Thus
 * the Form has to be a dedicated <code>Form</code> for logging purposes. The
 * <code>LogForm</code> is a convenience class for logging purposes.
 * 
 * @see net.sf.microlog.ui.LogForm
 * @author Johan Karlsson
 * @since 0.1
 */
public class FormAppender extends AbstractAppender {

	private boolean logOpen = true;

	private Form logForm;

	/**
	 * Create a FormAppender. A default <code>LogForm</code> object is
	 * created.
	 */
	public FormAppender() {
		this.logForm = getDefaultLogForm();
	}

	/**
	 * Create a FormAppender that uses the specified Form to log.
	 * 
	 * @param logForm
	 *            the <code>Form</code> to log to.
	 */
	public FormAppender(Form logForm) {
		this.logForm = logForm;
	}

	/**
	 * Get the default <code>LogForm</code>.
	 * 
	 * @return the default <code>LogForm</code>. If the class
	 *         <code>net.sf.microlog.ui.LogForm</code> could not be loaded, null
	 *         is returned.
	 */
	private Form getDefaultLogForm() {
		Form form = null;
		try {
			Class logFormClass = Class.forName("net.sf.microlog.ui.LogForm");
			form = (Form) logFormClass.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return form;
	}

	/**
	 * Get the <code>Form</code> that is used for logging.
	 * 
	 * @return Returns the logForm.
	 */
	public final Form getLogForm() {
		return logForm;
	}

	/**
	 * Set the <code>Form</code> that shall be used for logging.
	 * 
	 * @param logForm
	 *            The logForm to set.
	 */
	public final void setLogForm(Form logForm) {
		this.logForm = logForm;
	}

	/**
	 * Do the logging.
	 * 
	 * @param level
	 *            the level to use for the logging.
	 * @param message
	 *            the message to log.
	 * @param t
	 *            the exception to log.
	 */
	public void doLog(Level level, Object message, Throwable t) {
		if (logOpen && formatter != null && logForm != null) {
			StringItem str = new StringItem(null, formatter.format(level, message, t) + "\n");
			str.setFont(Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL));
			logForm.insert(0, str);
			if (logForm.size() > 200) logForm.delete(200);
			//logForm.append(str);
		}
	}

	/**
	 * Clear the underlying RecordStore from data. Note if logging is done when
	 * executing this method, these new logging events are not cleared.
	 * 
	 * @see net.sf.microlog.appender.AbstractAppender#clearLog()
	 */
	public void clearLog() {
		if (logForm != null) {
			logForm.deleteAll();
		}
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#closeLog()
	 */
	public void closeLog() {
		logOpen = false;
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#openLog()
	 */
	public void openLog() {
		logOpen = true;
	}

	/**
	 * Get the size of the log. The size is the number of items logged.
	 * 
	 * @return the size of the log.
	 */
	public long getLogSize() {

		long logSize = SIZE_UNDEFINED;

		if (logForm != null) {
			logSize = logForm.size();
		}

		return logSize;
	}

}
