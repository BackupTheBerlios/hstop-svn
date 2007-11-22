package de.berlios.hstop.jhstop2.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class TunnelDataTest {

	@Test
	public final void testToArray() {
		TunnelData td = new TunnelData();
		Session s = new Session(TunnelType.TCP_TUNNEL, "localhost", 9012, "oOq@g-6");
		byte[] d ={ (byte) 'a', (byte) 'B', (byte) 'c'};
		td.addData(s, d);
		byte[] ret = td.toArray();
		assertEquals(ret[0], 8 + d.length);
		assertEquals(ret[1], (byte) 'o');
		assertEquals(ret[2], (byte) 'O');
		assertEquals(ret[3], (byte) 'q');
		assertEquals(ret[4], (byte) '@');
		assertEquals(ret[5], (byte) 'g');
		assertEquals(ret[6], (byte) '-');
		assertEquals(ret[7], (byte) '6');
		
		for (int i = 0; i < d.length; i++)
			assertEquals(ret[8 + i], d[i]);
		
		System.out.println(new String(ret));
	}

	@Test
	public final void testToArray2() {
		TunnelData td = new TunnelData();
		Session s = new Session(TunnelType.TCP_TUNNEL, "localhost", 9012, "oOq@g-65123");
		byte[] d ={ (byte) 'a', (byte) 'B', (byte) 'c'};
		byte[] d2 ={ (byte) 'a', (byte) 'B', (byte) 'c', (byte) 'A', (byte) 'b', (byte) 'C'};
		td.addData(s, d);
		td.addData(s, d2);
		byte[] ret = td.toArray();
		assertEquals(ret[0], 8 + d.length);
		assertEquals(ret[1], (byte) 'o');
		assertEquals(ret[2], (byte) 'O');
		assertEquals(ret[3], (byte) 'q');
		assertEquals(ret[4], (byte) '@');
		assertEquals(ret[5], (byte) 'g');
		assertEquals(ret[6], (byte) '-');
		assertEquals(ret[7], (byte) '6');
		
		for (int i = 0; i < d.length; i++)
			assertEquals(ret[8 + i], d[i]);
		
		System.out.println(new String(ret));
	}
	
	@Test
	public final void testToArray3() {
		TunnelData td = new TunnelData();
		Session s = new Session(TunnelType.TCP_TUNNEL, "localhost", 9012, "oOq@g-65123");
		byte[] d ={ (byte) 'a', (byte) 'B', (byte) 'c'};
		byte[] d2 ={ (byte) 'a', (byte) 'B', (byte) 'c', (byte) 'A', (byte) 'b', (byte) 'C'};
		td.addData(s, d);
		td.addData(s, d2);
		byte[] ret = td.toArray();
		assertEquals(ret[0], 8 + d.length);
		assertEquals(ret[1], (byte) 'o');
		assertEquals(ret[2], (byte) 'O');
		assertEquals(ret[3], (byte) 'q');
		assertEquals(ret[4], (byte) '@');
		assertEquals(ret[5], (byte) 'g');
		assertEquals(ret[6], (byte) '-');
		assertEquals(ret[7], (byte) '6');
		
		for (int i = 0; i < d.length; i++)
			assertEquals(ret[8 + i], d[i]);
		System.out.println(new String(ret));
		TunnelData td2 = new TunnelData(ret);
		System.out.println(new String(td2.toArray()));
		
		assertEquals(td.getCount(), td2.getCount());
		assertArrayEquals(td.toArray(), td2.toArray());
	}
}
