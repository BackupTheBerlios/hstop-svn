/*
 * AppProperty.java
 *
 * Created on den 24 oktober 2005, 10:59
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.microlog.util.properties;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.midlet.MIDlet;

/**
 *
 * @author Darius Katz
 */
public class AppProperty implements PropertySource{
    
    private MIDlet midlet;
    
    /** 
     * Creates a new instance of AppProperty 
     *
     * @param m the MIDlet from which the application properties are read.
     */
    public AppProperty(MIDlet m) {
        midlet = m;
    }
    
	/**
	 * Insert the values taken from a property source into the Hashtable.
     * Any previous values with the same key should be overridden/overwritten.
     * 
     * @param properties the Hashtable in which the properties are stored
	 */
    public void insertProperties(Hashtable properties) {
        Enumeration keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = midlet.getAppProperty(key);
            if (value != null) {
                properties.put(key, value);
            }
        }
    }

}
