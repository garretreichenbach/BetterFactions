package thederpgamer.betterfactions.data.persistent.faction;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.persistent.PersistentData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class FactionData implements PersistentData {

	private int id;
	private String name;

	public FactionData(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public FactionData(Faction faction) {
		this.id = faction.getIdFaction();
		this.name = faction.getName();
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(PersistentData persistentData) {
		return persistentData instanceof FactionData && persistentData.getId() == getId() && persistentData.getName().equals(getName());
	}
}
