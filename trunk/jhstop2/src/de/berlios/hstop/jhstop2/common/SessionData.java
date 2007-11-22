package de.berlios.hstop.jhstop2.common;

public class SessionData {
	private String s;
	private byte[] d;
	
	public SessionData(String sessionID, byte[] data) {
		s = sessionID;
		d = data;
	}
	
	public String getSession() {
		return s;
	}
	
	public byte[] getData() {
		return d;
	}

}
