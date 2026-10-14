package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.faction.newfaction.FactionNewsScrollableListNew;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.gui.ExpandedRowButtons;

/**
 * The news list's expanded row with its delete button across the row, as {@link MemberRowButtonsMixin}
 * does for members.
 */
@Mixin(value = FactionNewsScrollableListNew.class, remap = false)
public abstract class NewsRowButtonsMixin {

	@Redirect(method = "updateListEntries", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/client/view/gui/faction/newfaction/FactionNewsScrollableListNew$FactionRow;onInit()V"))
	private void betterFactions$tableButtons(@Coerce ScrollableTableList.Row row) {
		ExpandedRowButtons.news((FactionNewsScrollableListNew) (Object) this, row);
		row.onInit();
	}
}
