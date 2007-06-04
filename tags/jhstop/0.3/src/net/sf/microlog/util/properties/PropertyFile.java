/*
 * PropertyFile.java
 *
 * Created on den 20 oktober 2005, 11:13
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.microlog.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * A class to handle the properties in a property file (a textfile).
 *
 * @author Darius Katz
 */
public class PropertyFile implements PropertySource {
    
    private String fileName;
	
    private static final int BUFFER_SIZE = 256;
	private byte[] buffer = new byte[BUFFER_SIZE];
	private StringBuffer stringBuffer = new StringBuffer(2 * BUFFER_SIZE);
    
    /** Creates a new instance of PropertyFile */
    public PropertyFile(String f) {
        fileName = f;
    }

	/**
	 * Insert the values taken from a property source into the Hashtable.
     * Any previous values with the same key should be overridden/overwritten.
     * 
     * @param properties the Hashtable in which the properties are stored
	 */
    public void insertProperties(Hashtable properties) {
        //get an InputStream to the property file; is
        InputStream is = this.getClass().getResourceAsStream(
				fileName);
		if (is != null) {
			System.out.println("Property file for Logger found");
            //is is used below
		} else {
			System.out.println("Property file not found");
            return;
		}

        //get a string with the contents of the file; configString
        String configString = null;
        try {
			int readBytes = is.read(buffer);
			while (readBytes > 0) {
				String string = new String(buffer, 0, readBytes, "UTF-8");
				stringBuffer.append(string);
				readBytes = is.read(buffer);
			}

			if (stringBuffer.length() > 0) {
				configString = stringBuffer.toString();
                //configString is used below
            }
		} catch (IOException e) {
			e.printStackTrace();
            return;
        } finally {
			try {
				is.close();
			} catch (IOException e) { }
		}

		// parse the string and put keys/values into the properties hashtable
        int separatorIndex = configString.indexOf("=");
		int currentIndex = 0;
		int newLineIndex = 0;

		while (separatorIndex > -1) {
			String propertyKey = configString.substring(currentIndex,
					separatorIndex++);
			newLineIndex = configString.indexOf("\r\n", separatorIndex);
			if (newLineIndex != -1) {
				String propertyValue = configString.substring(
						separatorIndex, newLineIndex);
				separatorIndex = newLineIndex + 2;
				currentIndex = separatorIndex;
				//Put the propertyKey and the propertyValue
                //into the properies hashtable
                properties.put(propertyKey, propertyValue);                
			}
			separatorIndex = configString.indexOf("=", separatorIndex);
		}        
    }
}
