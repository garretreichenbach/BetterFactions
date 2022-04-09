package thederpgamer.betterfactions.data.persistent;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public interface PersistentData {

	int getId();
	String getName();
	boolean equals(PersistentData persistentData);
}
