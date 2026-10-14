package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.faction.newfaction.FactionMemberScrollableListNew;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.gui.ExpandedRowButtons;

/**
 * The member list's expanded row as a button table across the row, rather than five fixed-width buttons
 * packed at the left. Swapped in just before the row initialises, once vanilla has built it. The buttons
 * are built in {@link ExpandedRowButtons}: a mixin cannot carry anonymous classes of its own.
 */
@Mixin(value = FactionMemberScrollableListNew.class, remap = false)
public abstract class MemberRowButtonsMixin {

	@Shadow
	private Faction faction;

	@Redirect(method = "updateListEntries", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/client/view/gui/faction/newfaction/FactionMemberScrollableListNew$FactionRow;onInit()V"))
	private void betterFactions$tableButtons(@Coerce ScrollableTableList.Row row) {
		ExpandedRowButtons.members((FactionMemberScrollableListNew) (Object) this, faction, row);
		row.onInit();
	}
}
