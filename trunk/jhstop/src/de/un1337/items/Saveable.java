package de.un1337.items;

public interface Saveable {
	public void save();

	public void load();

	public void loadFromString(String s);

	public String saveToString();

	public void setRSstr(String id);

	public String getRSstr();

	public void setRSid(int id);

	public int getRSid();

}
