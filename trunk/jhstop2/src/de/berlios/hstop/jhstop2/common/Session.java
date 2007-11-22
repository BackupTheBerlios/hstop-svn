/**
 * 
 */
package de.berlios.hstop.jhstop2.common;


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
	
	public Session(TunnelType type, String host, int port, String id)
	{
		tunneltype = type;
		Host = host;
		Port = port ;
		ID = id;
	}
	
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
