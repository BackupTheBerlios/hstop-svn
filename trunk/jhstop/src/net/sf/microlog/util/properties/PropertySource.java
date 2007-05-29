/*
 * PropertySource.java
 *
 * Created on den 20 oktober 2005, 10:57
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.microlog.util.properties;

import java.util.Hashtable;

/**
 * An interface that every property source must implement.
 *
 * @author Darius Katz
 */
public interface PropertySource {
    
	/**
	 * Insert the values taken from a property source into the Hashtable.
     * Any previous values with the same key should be overridden/overwritten.
     * 
     * @param properties the Hashtable in which the properties are stored
     *
	 */
    void insertProperties(Hashtable properties);
    
}
