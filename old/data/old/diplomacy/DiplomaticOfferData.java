package thederpgamer.betterfactions.data.old.diplomacy;

import thederpgamer.betterfactions.gui.faction.diplomacy.DiplomaticChangeData;

import java.util.HashMap;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class DiplomaticOfferData {

	protected final HashMap<DiplomaticData, DiplomaticChangeData> diploMap = new HashMap<>();

	/*
	public DiplomaticOfferData(PersistentDataMap map) {
		deserialize(map);
	}

	@Override
	public void deserialize(PersistentDataMap map) {
		int size = map.getInt("size");
		for(int i = 0; i < size; i ++) {

		}
	}

	@Override
	public void serialize(PersistentDataMap map) {
		map.update("size", diploMap.size());
		ArrayList<DiplomaticData> keyList = new ArrayList<>();
		ArrayList<DiplomaticChangeData> valueList = new ArrayList<>();
		for(Map.Entry<DiplomaticData, DiplomaticChangeData> entry : diploMap.entrySet()) {
			keyList.add(entry.getKey());
			valueList.add(entry.getValue());
		}
		map.update("diplo_list", keyList);
		map.update("change_list", valueList);
	}
	 */

	public HashMap<DiplomaticData, DiplomaticChangeData> getDiploMap() {
		return diploMap;
	}
}
