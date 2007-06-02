/*
 * DefaultValues.java
 *
 * Created on den 20 oktober 2005, 13:18
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.microlog.util.properties;

import java.util.Hashtable;

/**
 * A property source that contains the default values. This is the lowest level
 * from which properties originate.
 *
 * @author Darius Katz
 */
public class DefaultValues implements PropertySource {
    
    String defaultValues[][] = {
        {"microlog.level", "INFO"},
        
        {"microlog.appender", "net.sf.microlog.appender.ConsoleAppender"}, 
        
        // The maximum number of log entries that are kept in the recordstore.
        {"microlog.appender.RecordStoreAppender.maxLogEntries","10"},
        
        {"microlog.formatter", "net.sf.microlog.format.SimpleFormatter"},
        
        {"test.defaultvalue","from.default.values"},        
        {"test.propertyfile","from.default.values"},
        {"test.app.property","from.default.values"},
        {"test.numapp.property","from.default.values"}
    
    };

    
    /** Creates a new instance of DefaultValues */
    public DefaultValues() {
    }
    

	/**
	 * Insert the values taken from a property source into the Hashtable.
     * This is the lowest level from which properties originate.
     * 
     * @param properties the Hashtable in which the properties are stored
     *
	 */
    public void insertProperties(Hashtable properties) {
        for (int i=0; i<defaultValues.length; i++) {
            properties.put(defaultValues[i][0], defaultValues[i][1]);
        }
    }
    
	/**
	 * Returns the Object to which the specified key is mapped, directly from
     * the source of the default values (that is not from the Hashtable).
     *
     * @param key the key associated to the stored Object
     *
	 * @return the Object to which the key is mapped; null if the key is not
     * mapped to any Object
	 */
    public Object get(String key) {
        boolean notFound = true;
        for (int i=0; i<defaultValues.length && notFound; i++) {
            if (key.compareTo(defaultValues[i][0]) == 0) {
                return defaultValues[i][1];
            }
        }
        
        return null;
    }
}
