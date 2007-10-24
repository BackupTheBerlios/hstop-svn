/**
 * 
 */
package de.berlios.hstop.jhstop2.server;

import de.berlios.hstop.jhstop2.common.TunnelType;

/**
 * This is one Tunnelsession.
 * @author flx
 *
 */
public class Session {
	private TunnelType tunneltype;
	private int Port;
	private String Host;
	private String ID;
	
	/**
	 * @return type of Session
	 */
	public TunnelType getTunnelType() {
		return tunneltype;
	}
	
	/**
	 * @return Port to connect to
	 */
	public int getPort() {
		return Port;
	}
	
	/**
	 * @return Host to connect to
	 */
	public String getHost() {
		return Host;
	}
	
	/**
	 * @return id of Session
	 */
	public String getID() {
		return ID;
	}
}
