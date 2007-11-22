/**
 * 
 */
package de.berlios.hstop.jhstop2.common;

import java.util.Vector;

/**
 * @author flx
 *
 */
public class TunnelData {
	private Vector<SessionData> sessions;
	
	public TunnelData() {
		sessions = new Vector<SessionData>();
	}
	
	public TunnelData(byte[] data) {
		sessions = new Vector<SessionData>();
		// read in data
		for (int offset = 0; offset < data.length; offset += data[offset]) {
			// handle single session
			// data[of+1..of+7] -> id
			// data[of+8..of+d[of]-1] -> data
			byte[] sessionID = new byte[7];
			for (int i = offset + 1; i < offset + 8; i++)
				sessionID[i - offset - 1] = data[i];
			byte[] sessionData = new byte[data[offset] - 8];
			for (int i = offset + 8; i < offset + data[offset]; i++)
				sessionData[i - offset - 8] = data[i];
			addData(new String(sessionID), sessionData);
		}
	}
	
	public void addData(Session s, byte[] data) {
		sessions.add(new SessionData(s.getID(), data));
	}
	
	public void addData(String s, byte[] data) {
		sessions.add(new SessionData(s, data));
	}
	
	public int getCount() {
		return sessions.size();
	}
	
	/**
	 * build array of sessiondata.
	 * syntax:
	 * 		sessiondata ::= ([(byte) lenofsession][session])*
	 * 		session	::= 	[(byte[7]) id][(byte[]) dump]
	 * @return array of sessiondata
	 */
	public byte[] toArray() {
		byte[] ret;
		int size = 0;
		for (int i = 0; i < sessions.size(); i++) {
			size += sessions.elementAt(i).getData().length + 8;
		}
		ret = new byte[size];
		size = 0;
		for (int i = 0; i < sessions.size(); i++) {
			ret[size] = (byte) (sessions.elementAt(i).getData().length + 8);
			byte[] sessionID = sessions.elementAt(i).getSession().getBytes();
			byte[] sessionData = sessions.elementAt(i).getData();
			for (int j = size + 1; j < size + 8; j++) {
				try {
					ret[j] = sessionID[j - size -1];
				} catch (ArrayIndexOutOfBoundsException e) {
					ret[j] = (byte) 'x';
				}
			}
			for (int j = size +8; j < size + ret[size]; j++) {
				ret[j] = sessionData[j - size - 8];
			}
			size += ret[size];
		}
		return ret;
	}
}
