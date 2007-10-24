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
		private final int num;
		
		/**
		 * private tunnel constructor.
		 * @param name name of tunnel
		 * @param num numerical value
		 */
		private TunnelType(String name, int num) {
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
		public int toInt() {
			return num;
		}
		
		/**
		 * TunnelType: TCP
		 */
		public static final TunnelType TCP_TUNNEL = new TunnelType("tcp", 0);
		
		/**
		 * TunnelType: UDP
		 */
		public static final TunnelType UDP_TUNNEL = new TunnelType("udp", 1);
		
}
