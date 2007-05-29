package de.berlios.hstop.tools;


public class Waiter {
	private static int initial = 500;
	private static int max = 5000;
	
	
	private int amount;

	public Waiter() {
		amount = initial;
	}

	public void sleep() {
		try {
			Thread.sleep(amount);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		amount *= 2;
		if (amount > max) amount = max;
	}

	public void reduce() {
		amount /= 2;
	}
}
