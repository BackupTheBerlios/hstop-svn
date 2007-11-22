/**
 * 
 */
package de.berlios.hstop.jhstop2.common;

/**
 * Type of a Tunnel.
 * @author flx
 */
public final class TunnelType {
		private final String name;
		private final byte num;
		
		/**
		 * private tunnel constructor.
		 * @param name name of tunnel
		 * @param num numerical value
		 */
		private TunnelType(String name, byte num) {
			this.name = name;
			this.num = num;
		}
		
		/**
		 * @see java.lang.Object#toString()
		 * @return name of TunnelType
		 */
		public String toString(){
			return name;
		}
		
		/**
		 * @return numerical TunnelType
		 */
		public byte toInt() {
			return num;
		}
		
		/**
		 * TunnelType: TCP
		 */
		public static final TunnelType TCP_TUNNEL = new TunnelType("tcp", (byte) 0);
		
		/**
		 * TunnelType: UDP
		 */
		public static final TunnelType UDP_TUNNEL = new TunnelType("udp", (byte) 1);
		
}
