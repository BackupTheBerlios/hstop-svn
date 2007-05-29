package net.sf.microlog.ui;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import net.sf.microlog.util.GlobalProperties;
import net.sf.microlog.util.Util;


/**
 * A MIDlet that is used for viewing a log created with the RecordStoreAppender.
 * 
 * @author Johan Karlsson
 * @author Darius Katz
 */
public class RecordStoreLogViewer extends MIDlet implements CommandListener {

	private static final int MAX_NAME_LENGTH = 32;
    
    private RecordComparator ascComparator = new AscendingComparator();
    private RecordComparator descComparator = new DescendingComparator();
    private RecordComparator currentComparator = ascComparator;

    private String recordStoreName = "LogRecordStore";

	private RecordStore logRecordStore;

	private Display display;

	private Form logScreen;

	private Form preferenceScreen;

	private TextField nameField;

	private Command loadLogCommand = new LoadLogCommand();

	private Command clearLogCommand = new ClearLogCommand();

	private Command exitCommand = new ExitCommand();

	private Command preferenceCommand = new PreferenceCommand();

	private Command preferenceOkCommand = new PreferenceOkCommand();

	private Command preferenceCancelCommand = new PreferenceCancelCommand();
    
    private Command switchComparatorCommand = new SwitchComparatorCommand((AbstractCommand)loadLogCommand);

	/**
	 * Create a RecordStoreLogViewer.
	 */
	public RecordStoreLogViewer() {
		super();
		display = Display.getDisplay(this);

		logScreen = new Form("Log content");
		logScreen.addCommand(loadLogCommand);
		logScreen.addCommand(clearLogCommand);
		logScreen.addCommand(preferenceCommand);
		logScreen.addCommand(exitCommand);
		logScreen.addCommand(switchComparatorCommand);
        logScreen.setCommandListener(this);

		preferenceScreen = new Form("Preferences");
		nameField = new TextField("RecordStore name ", null, MAX_NAME_LENGTH,
				TextField.ANY);
		preferenceScreen.append(nameField);
		preferenceScreen.addCommand(preferenceOkCommand);
		preferenceScreen.addCommand(preferenceCancelCommand);
		preferenceScreen.addCommand(exitCommand);
		preferenceScreen.setCommandListener(this);

	}

	/**
	 * Start the MIDlet.
	 * 
	 * @throws MIDletStateChangeException
	 *             if the MIDlet fails to change the state.
	 */
	protected void startApp() throws MIDletStateChangeException {
        //Test of Properties
        GlobalProperties.init(this);
        GlobalProperties props = GlobalProperties.getInstance();
                
        System.out.println("microlog.level = "+props.getString("microlog.level"));
        System.out.println("microlog.appender = "+props.getString("microlog.appender"));
        System.out.println("microlog.appender.RecordStoreAppender.maxLogEntries = "+
                props.getString("microlog.appender.RecordStoreAppender.maxLogEntries"));
        System.out.println("microlog.formatter = "+props.getString("microlog.formatter"));
        System.out.println("test.defaultvalue = "+props.getString("test.defaultvalue"));
        System.out.println("test.propertyfile = "+props.getString("test.propertyfile"));        
		
        display.setCurrent(logScreen);        
	}

	/**
	 * Pause the MIDlet.
	 */
	protected void pauseApp() {
	}

	/**
	 * Destroy the MIDlet.
	 * 
	 * @param unconditional
	 *            If true when this method is called, the MIDlet must cleanup
	 *            and release all resources. If false the MIDlet may throw
	 *            <code>MIDletStateChangeException</code> to indicate it does
	 *            not want to be destroyed at this time.
	 * @throws MIDletStateChangeException
	 *             if the MIDlet fails to change the state.
	 */
	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {
	}

	/**
	 * Close the log.
	 */
	private void closeLog() {
		try {
			if (logRecordStore != null) {
				logRecordStore.closeRecordStore();
			}
		} catch (RecordStoreNotOpenException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Implementation of the CommandListener interface.
	 * 
	 * @param cmd
	 *            the command.
	 * @param displayable
	 *            the displayable that generated the command action.
	 */
	public void commandAction(Command cmd, Displayable displayable) {
		AbstractCommand myCmd = (AbstractCommand) cmd;
		myCmd.execute();
	}
        
	/**
	 * The super class for all the Command objects.
	 * 
	 * @author Johan Karlsson
	 */
	protected abstract class AbstractCommand extends Command {

		/**
		 * Create an AbstractCommand.
		 * 
		 * @param label
		 *            the label to use.
		 * @param commandType
		 *            the type of <code>Command</code>.
		 * @param priority
		 *            the priority of the <code>Command</code>.
		 */
		public AbstractCommand(String label, int commandType, int priority) {
			super(label, commandType, priority);
		}

		/**
		 * Create an AbstractCommand.
		 * 
		 * @param shortLabel
		 *            the short label to use.
		 * @param longLabel
		 *            the long label to use.
		 * @param commandType
		 *            the type of <code>Command</code>.
		 * @param priority
		 *            the priority of the <code>Command</code>.
		 */
		public AbstractCommand(String shortLabel, String longLabel,
				int commandType, int priority) {
			super(shortLabel, longLabel, commandType, priority);
		}

		/**
		 * Execute the command.
		 */
		public abstract void execute();
	}

	/**
	 * A command that loads the log.
	 * 
	 * @author Johan Karlsson
	 */
	protected class LoadLogCommand extends AbstractCommand {

		/**
		 * Create a LoadLogCommand object.
		 */
		public LoadLogCommand() {
			super("Load", "Load Log", Command.ITEM, 1);
		}

		/**
		 * Execute the command.
		 * 
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			try {
				logRecordStore = RecordStore.openRecordStore(recordStoreName,
						true);
				int nofLogItems = logRecordStore.getNumRecords();

				logScreen.deleteAll();
				StringItem header = new StringItem("#Log records", String
						.valueOf(nofLogItems)
						+ "\r\n");
				header.setLayout(Item.LAYOUT_NEWLINE_AFTER);
				logScreen.append(header);

				RecordEnumeration recordEnum = logRecordStore.enumerateRecords(
						null, currentComparator, false);
				while (recordEnum.hasNextElement()) {
					byte[] data = recordEnum.nextRecord();
                    
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        DataInputStream is = new DataInputStream(bais);

                        long timestamp = is.readLong();
                        String logString = is.readUTF();
    					logScreen.append(Util.toTimestampString(timestamp)+": "+logString+"\n");

                        is.close();
                        bais.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
				}
                recordEnum.destroy();
			} catch (RecordStoreNotFoundException e) {
				showInfoAlert("Could not find log data in  " + recordStoreName,
						e);
			} catch (RecordStoreException e) {
				showInfoAlert("Could not open log data. ", e);
			} finally {
				
                closeLog();
			}

		}

	}

	/**
	 * A command that clears the log.
	 * 
	 * @author Johan Karlsson
	 */
	protected class ClearLogCommand extends AbstractCommand {

		/**
		 * Create a ClearLogCommand.
		 */
		public ClearLogCommand() {
			super("Clear", "Clear Log", Command.ITEM, 2);
		}

		/**
		 * Execute the command.
		 * 
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			if (logRecordStore != null) {
				try {
					logRecordStore = RecordStore.openRecordStore(
							recordStoreName, true);
					RecordEnumeration enumeration = logRecordStore
							.enumerateRecords(null, null, false);
					while (enumeration.hasNextElement()) {
						int recordId = enumeration.nextRecordId();
						logRecordStore.deleteRecord(recordId);
					}
					logScreen.deleteAll();
					logRecordStore = RecordStore.openRecordStore(
							recordStoreName, true);
					int nofLogItems = logRecordStore.getNumRecords();

					StringItem header = new StringItem("#Log records", String
							.valueOf(nofLogItems)
							+ "\r\n");
					header.setLayout(Item.LAYOUT_NEWLINE_AFTER);
					logScreen.append(header);
				} catch (RecordStoreNotOpenException e) {
					showInfoAlert(
							"RecordStore is not open. " + recordStoreName, e);
				} catch (InvalidRecordIDException e) {
					showInfoAlert("Invalid record id.", e);
				} catch (RecordStoreException e) {
					showInfoAlert("RecordStore not working.", e);
				}
			}

		}
	}

	/**
	 * A command that moves us to the preference screen.
	 * 
	 * @author Johan Karlsson
	 */
	protected class PreferenceCommand extends AbstractCommand {

		/**
		 * Create an PreferenceCommand.
		 */
		public PreferenceCommand() {
			super("Preferences", Command.ITEM, 3);
		}

		/**
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			nameField.setString(recordStoreName);
			display.setCurrent(preferenceScreen);
		}

	}

	/**
	 * A command that exit the MIDlet.
	 * 
	 * @author Johan Karlsson
	 */
	protected class ExitCommand extends AbstractCommand {

		/**
		 * Create an PreferenceCancelCommand.
		 */
		public ExitCommand() {
			super("Exit", Command.EXIT, 1);
		}

		/**
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			try {
				destroyApp(true);
				notifyDestroyed();
			} catch (MIDletStateChangeException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Accept the preferences.
	 * 
	 * @author Johan Karlsson
	 */
	protected class PreferenceOkCommand extends AbstractCommand {

		/**
		 * Create an PreferenceCancelCommand.
		 */
		public PreferenceOkCommand() {
			super("OK", Command.OK, 1);
		}

		/**
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			String newName = nameField.getString();
			if (newName != null && newName.length() > 0) {
				// Try to open the RecordStore to see if the name is correct.

				try {
					RecordStore.openRecordStore(newName, true);
					recordStoreName = newName;
					display.setCurrent(logScreen);
				} catch (RecordStoreFullException e) {
					showInfoAlert("RecordStore is full.", e);
				} catch (RecordStoreNotFoundException e) {
					showInfoAlert("RecordStore does not exists.", e);
				} catch (RecordStoreException e) {
					showInfoAlert("RecordStore is not workings.", e);
				}
			} else {
				showInfoAlert("Please enter a name.", null);
			}
		}

	}

	/**
	 * Cancel the preferences.
	 * 
	 * @author Johan Karlsson
	 */
	protected class PreferenceCancelCommand extends AbstractCommand {

		/**
		 * Create an PreferenceCancelCommand.
		 */
		public PreferenceCancelCommand() {
			super("Cancel", Command.CANCEL, 1);
		}

		/**
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
			display.setCurrent(logScreen);
		}
	}

	/**
	 * Switch between RecordComparators. The 
	 * 
	 * @author Darius Katz
	 */
	protected class SwitchComparatorCommand extends AbstractCommand {

        private AbstractCommand performThisCommand;
        
		/**
		 * Create a SwitchComparatorCommand.
         *
         * @param c
         *            The AbstractCommand to perform after switching
         *            RecordComparator. Ignored if <code>null</code>.
		 */
        public SwitchComparatorCommand(AbstractCommand c) {
			super("Switch sort-order", Command.ITEM, 3);
            performThisCommand = c;
		}

		/**
		 * @see net.sf.microlog.ui.RecordStoreLogViewer.AbstractCommand#execute()
		 */
		public void execute() {
            if(currentComparator instanceof AscendingComparator) {
                currentComparator = descComparator;
            } else {
                currentComparator = ascComparator;
            }
            
            if(performThisCommand != null) {
                performThisCommand.execute();
            }
        }
	}

    
    /**
	 * Show an information alert.
	 * 
	 * @param message
	 *            the message to show.
	 * @param exception
	 *            an optional exception to append to the message. Could be null.
	 */
	protected void showInfoAlert(String message, Throwable exception) {
		StringBuffer buffer = new StringBuffer(message);
		if (exception != null) {
			buffer.append("\r\n");
			buffer.append(exception);
		}

		Alert alert = new Alert("Info", buffer.toString(), null, AlertType.INFO);
		display.setCurrent(alert, display.getCurrent());
	}


	/**
	 * An descending RecordComparator, based on the timestamp.
	 * 
	 * @author Darius Katz
	 */
    protected class DescendingComparator implements RecordComparator {

        /**
         * The compare() implemtation, descending based on the timestamp
         */
        public int compare(byte[] entry1, byte[] entry2) {
            try {
                ByteArrayInputStream bais1 = new ByteArrayInputStream(entry1);
                DataInputStream is1 = new DataInputStream(bais1);
                
                ByteArrayInputStream bais2 = new ByteArrayInputStream(entry2);
                DataInputStream is2 = new DataInputStream(bais2);
                
                // Sort based on the timestamp which is the first long in the data/stream
                long timestamp1 = is1.readLong();
                long timestamp2 = is2.readLong();
                
                if(timestamp1 < timestamp2) {
                    return RecordComparator.PRECEDES;
                } else if(timestamp1 > timestamp2) {
                    return RecordComparator.FOLLOWS;
                } else {
                    return RecordComparator.EQUIVALENT;
                }                
                
            } catch(IOException e) {
                e.printStackTrace();
                return RecordComparator.EQUIVALENT; //Just return something...
            }
        }
    }

	/**
	 * An ascending RecordComparator, based on the timestamp.
	 * 
	 * @author Darius Katz
	 */
    protected class AscendingComparator implements RecordComparator {
        
        /**
         * The compare() implemtation, ascending based on the timestamp
         */
        public int compare(byte[] entry1, byte[] entry2) {
            try {
                ByteArrayInputStream bais1 = new ByteArrayInputStream(entry1);
                DataInputStream is1 = new DataInputStream(bais1);
                
                ByteArrayInputStream bais2 = new ByteArrayInputStream(entry2);
                DataInputStream is2 = new DataInputStream(bais2);
                
                // Sort based on the timestamp which is the first long in the data/stream
                long timestamp1 = is1.readLong();
                long timestamp2 = is2.readLong();
                
                if(timestamp1 > timestamp2) {
                    return RecordComparator.PRECEDES;
                } else if(timestamp1 < timestamp2) {
                    return RecordComparator.FOLLOWS;
                } else {
                    return RecordComparator.EQUIVALENT;
                }                
                
            } catch(IOException e) {
                e.printStackTrace();
                return RecordComparator.EQUIVALENT; //Just return something...
            }
        }
    }

}
