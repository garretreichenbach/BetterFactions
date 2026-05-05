package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for PlayerPanel to access the private factionPanelNew field
 * without reflection.
 */
@Mixin(PlayerPanel.class)
public interface PlayerPanelAccessor {

	@Accessor("factionPanelNew")
	FactionPanelNew getFactionPanelNew();

	@Accessor("factionPanelNew")
	void setFactionPanelNew(FactionPanelNew panel);
}
