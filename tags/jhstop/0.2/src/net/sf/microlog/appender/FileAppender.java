package net.sf.microlog.appender;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.sf.microlog.Level;

/**
 * A class that logs to a file. The class uses the FileConnection API from
 * JSR-75.
 * 
 * @author Johan Karlsson
 * @since 0.1
 */
public class FileAppender extends AbstractAppender {

	public static final String DEFAULT_EMULATOR_ROOT = "/root1";

	private static final int BUFFER_SIZE = 256;

	private static final String FILE_PROTOCOL = "file://";

	private String lineSeparator = "\r\n";

	private String fileSeparator = "/";

	private String rootDir = DEFAULT_EMULATOR_ROOT;

	private String fileName;

	private FileConnection fileConnection;

	private OutputStream outputStream;
	
	private boolean logOpen = true;

	/**
	 * Create a FileAppender.
	 */
	public FileAppender() {
		super();
		fileSeparator = System.getProperty("file.separator");
		fileName = "microlog.txt";
	}

	/**
	 * Create a FileAppender for the specified file name.
	 * 
	 * @param fileName
	 *            the file name.
	 */
	public FileAppender(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#doLog(net.sf.microlog.Level,
	 *      java.lang.Object, java.lang.Throwable)
	 */
	public void doLog(Level level, Object message, Throwable t) {
		if (logOpen && formatter != null) {
			String logString = formatter.format(level, message, t);
			try {
				byte[] stringData = logString.getBytes();
				outputStream.write(stringData);
				outputStream.write(lineSeparator.getBytes());
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#clearLog()
	 */
	public synchronized void clearLog() {
		if (fileConnection != null && fileConnection.isOpen()) {
			try {
				fileConnection.truncate(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#closeLog()
	 */
	public void closeLog() {
		if (fileConnection != null && fileConnection.isOpen()) {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
				fileConnection.close();
				logOpen = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see net.sf.microlog.appender.AbstractAppender#openLog()
	 */
	public void openLog() {
		
		if (fileConnection == null
				|| (fileConnection != null && !fileConnection.isOpen())) {
			try {
				String connectionString = getConnectionString();

				fileConnection = (FileConnection) Connector.open(
						connectionString, Connector.READ_WRITE);
				if (!fileConnection.exists()) {
					fileConnection.create();
				}

				outputStream = fileConnection.openDataOutputStream();
				logOpen = true;
			} catch (IOException e) {
				logOpen = false;
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the size of the log.
	 * 
	 * @return the size of the log.
	 */
	public long getLogSize() {

		long logSize = SIZE_UNDEFINED;

		if (fileConnection != null) {
			try {
				logSize = fileConnection.fileSize();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return logSize;
	}

	/**
	 * Get the connection string to use for opening the FileConnection.
	 * 
	 * @return the connection String.
	 */
	private String getConnectionString() {
		StringBuffer stringBuffer = new StringBuffer(BUFFER_SIZE);

		stringBuffer.append(FILE_PROTOCOL);
		stringBuffer.append(rootDir);
		stringBuffer.append(fileSeparator);
		stringBuffer.append(fileName);

		return stringBuffer.toString();
	}
}
