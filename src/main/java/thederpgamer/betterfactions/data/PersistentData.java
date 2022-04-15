package thederpgamer.betterfactions.data;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public interface PersistentData {

	int getId();
	String getName();
	void setName(String name);
	boolean equals(PersistentData persistentData);
}
