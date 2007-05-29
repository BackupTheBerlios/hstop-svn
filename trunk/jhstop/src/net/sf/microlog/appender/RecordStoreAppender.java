package net.sf.microlog.appender;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import net.sf.microlog.Level;
import net.sf.microlog.rms.DescendingComparator;
import net.sf.microlog.util.GlobalProperties;


/**
 * An Appender that appends the logging to the record store.
 * 
 * @author Johan Karlsson
 * @author Darius Katz
 * @since 0.1
 */
public class RecordStoreAppender extends AbstractAppender {

	private boolean logOpen = true;

	private RecordStore logRecordStore;

	private String recordStoreName;

    // variables used by the limited record entries functionality
    private int maxRecordEntries;
    private int currentOldestEntry = 0;
    private int[] limitedRecordIDs;
    

	/**
	 * Create a RecordStoreAppender with the default name, i.e "LogRecordStore".
	 */
	public RecordStoreAppender() {
		recordStoreName = "LogRecordStore";
        initLimitedEntries();
	}

	/**
	 * Create a RecordStoreAppender with the specified name. The name is used
	 * when creating the <code>RecordStore</code>.
	 * 
	 * @param name
	 *            the name of the <code>RecordStore</code>.
	 */
	public RecordStoreAppender(String name) {
		this.recordStoreName = name;
        initLimitedEntries();
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
	public synchronized void doLog(Level level, Object message, Throwable t) {
		
		if (logOpen && formatter != null) {
            long timestamp = System.currentTimeMillis();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream os = new DataOutputStream(baos);
            byte[] data = null;
            
            try {
                os.writeLong(timestamp);
                os.writeUTF(formatter.format(level, message, t));
                data = baos.toByteArray();
                os.close();
                baos.close();                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            try {                
                //Delete the oldest log entry
                if (limitedRecordIDs[currentOldestEntry] != -1) {
                    logRecordStore.deleteRecord(limitedRecordIDs[currentOldestEntry]);
                }
                
                //Add the new entry
                int newRecId = logRecordStore.addRecord(data, 0, data.length);
    			
                //Save the recordId for later
                limitedRecordIDs[currentOldestEntry] = newRecId;
                
                //Move pointer to the now oldest entry
                currentOldestEntry = (currentOldestEntry+1) % maxRecordEntries;
                
			} catch (RecordStoreNotOpenException e) {
				e.printStackTrace();
			} catch (RecordStoreFullException e) {
				e.printStackTrace();
			} catch (RecordStoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Clear the underlying RecordStore from data. Note if logging is done when
	 * executing this method, these new logging events are not cleared.
	 * 
	 * @see net.sf.microlog.appender.AbstractAppender#clearLog()
	 */
	public synchronized void clearLog() {

		try {
			RecordEnumeration enumeration = logRecordStore.enumerateRecords(
					null, null, false);
			while (enumeration.hasNextElement()) {
				int recordId = enumeration.nextRecordId();
				logRecordStore.deleteRecord(recordId);
			}
		} catch (RecordStoreNotOpenException e) {
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#closeLog()
	 */
	public synchronized void closeLog() {
		if (!logOpen) {
			try {
				logRecordStore.closeRecordStore();
				logOpen = false;
			} catch (RecordStoreNotOpenException e) {
				e.printStackTrace();
			} catch (RecordStoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#openLog()
	 */
	public synchronized void openLog() {
		try {
			logRecordStore = RecordStore.openRecordStore(recordStoreName, true,
					RecordStore.AUTHMODE_ANY, true);
			logOpen = true;
		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the size of the log. Always returns SIZE_UNDEFINED, since it is not
	 * applicable to the ConsoleAppender.
	 * 
	 * @return the size of the log.
	 */
	public long getLogSize() {
		long logSize = SIZE_UNDEFINED;

		if (logRecordStore != null) {
			try {
				int numRecords = logRecordStore.getNumRecords();
				if (numRecords != 0) {
					logSize = logRecordStore.getSize();
				} else if (numRecords == 0) {
					logSize = 0;
				}
			} catch (RecordStoreNotOpenException e) {
				e.printStackTrace();
			}
		}

		return logSize;
	}

	/**
     * Initialise the limited entries functionality
	 * 
	 */
    private void initLimitedEntries() {        
        
        // Set the maximum number of record/log entries from Properties
        try {
           maxRecordEntries  = Integer.parseInt((GlobalProperties.getInstance().
                   getString("microlog.appender.RecordStoreAppender.maxLogEntries")));
        } catch (NumberFormatException e) {
           maxRecordEntries = Integer.parseInt((String)(GlobalProperties.getInstance().
                   getDefaultValue("microlog.appender.RecordStoreAppender.maxLogEntries")));
        }
        //maxRecordEntries = 10;
                
        limitedRecordIDs = new int[maxRecordEntries];
        
        for(int i=0; i<maxRecordEntries; i++) limitedRecordIDs[i] = -1;
        
        // Enumerate through all records. Copy/save timestamps and recordIDs
        // of the newest n records into the array(s) that keep track of limited
        // entries and delete the rest of the records from the RecordStore.
        // (n = max no of log-enties)
        
        try {
            int arrayPointer = maxRecordEntries-1;
            RecordStore logRecordStore = RecordStore.openRecordStore(recordStoreName, true);
            RecordEnumeration recordEnum = logRecordStore.enumerateRecords(
                    null, new DescendingComparator(), false);
            
            while (recordEnum.hasNextElement()) {
                int recId = recordEnum.nextRecordId();                
                if(arrayPointer >= 0) {
                    //save recId
                    limitedRecordIDs[arrayPointer] = recId;                        
                    arrayPointer--;
                } else {
                    //too old, just delete
                    logRecordStore.deleteRecord(recId);
                }
            }
            recordEnum.destroy();
        } catch (RecordStoreNotFoundException e) {
            e.printStackTrace();
            //showInfoAlert("Could not find log data in  " + recordStoreName, e);
        } catch (RecordStoreException e) {
            e.printStackTrace();
            //showInfoAlert("Could not open log data. ", e);
        } finally {
            closeLog();
        }

    }

}
