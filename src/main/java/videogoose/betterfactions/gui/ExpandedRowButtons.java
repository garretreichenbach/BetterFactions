package videogoose.betterfactions.gui;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.schine.graphicsengine.forms.AbstractSceneNode;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIListElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Vanilla expanded rows rebuilt with a button table across the row, in place of fixed-width buttons
 * packed at the left. Vanilla's buttons are kept for their labels and callbacks, so the confirmations and
 * permission popups behind them are unchanged; what vanilla hid from a player who may not use a button
 * is greyed out here instead, by the same rules.
 */
public final class ExpandedRowButtons {

	private static final GUIHorizontalArea.HButtonType BLUE = GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM;
	private static final GUIHorizontalArea.HButtonType RED = GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM;

	private ExpandedRowButtons() {
	}

	/** One button per vanilla button, in vanilla's order; a null rule means always usable. */
	private record Spec(GUIHorizontalArea.HButtonType type, BooleanSupplier allowed) {
	}

	/** The member list: mail, nav, promote, demote, kick. */
	public static void members(ScrollableTableList<?> list, Faction faction, ScrollableTableList.Row row) {
		GameClientState state = (GameClientState) list.getState();
		FactionPermission f = (FactionPermission) row.f;
		rebuild(list, row,
				new Spec(BLUE, null),
				new Spec(BLUE, null),
				new Spec(BLUE, () -> canPromote(state, faction, f)),
				new Spec(RED, () -> canDemote(state, faction, f)),
				new Spec(RED, () -> canKick(state, faction, f)));
	}

	/** The news list: delete. */
	public static void news(ScrollableTableList<?> list, ScrollableTableList.Row row) {
		GameClientState state = (GameClientState) list.getState();
		rebuild(list, row, new Spec(RED, () -> state.getPlayer().getFactionController().hasDescriptionAndNewsPostPermission()));
	}

	private static void rebuild(ScrollableTableList<?> list, ScrollableTableList.Row row, Spec... specs) {
		GUIListElement le = row.expanded.get(0);
		GUIElement oldContent = le.getContent();
		List<GUITextButton> old = new ArrayList<>();
		List<GUIElement> rest = new ArrayList<>();
		for (AbstractSceneNode n : oldContent.getChilds()) {
			if (n instanceof GUITextButton b) {
				old.add(b);
			} else if (n instanceof GUIElement e) {
				rest.add(e);
			}
		}
		//vanilla changed under us; leave its row as it is
		if (old.size() != specs.length) {
			return;
		}
		float buttonsY = old.get(0).getPos().y;

		GUIAnchor c = new GUIAnchor(list.getState(), 100, Math.max(oldContent.getHeight(), buttonsY + 26)) {
			@Override
			public void draw() {
				setWidth(list.getWidth() - 28);
				super.draw();
			}
		};
		for (GUIElement e : rest) {
			c.attach(e);
		}
		GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(list.getState(), specs.length, 1, c);
		buttons.onInit();
		buttons.setPos(0, buttonsY, 0);
		for (int i = 0; i < specs.length; i++) {
			add(buttons, i, old.get(i), specs[i]);
		}
		c.attach(buttons);

		le.setContent(c);
		le.setSelectContent(c);
	}

	private static void add(GUIHorizontalButtonTablePane buttons, int column, GUITextButton from, Spec spec) {
		BooleanSupplier allowed = spec.allowed();
		//the label overlay only exists after onInit, which vanilla may not have run yet (news rows); it no-ops when already done
		from.onInit();
		buttons.addButton(column, 0, from.getTextList().get(0), spec.type(), from.getCallback(), allowed == null ? null : new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState state) {
				return true;
			}

			@Override
			public boolean isActive(InputState state) {
				return allowed.getAsBoolean();
			}
		});
	}

	private static FactionPermission own(GameClientState state, Faction faction) {
		return faction.getMembersUID().get(state.getPlayer().getName());
	}

	private static boolean canPromote(GameClientState state, Faction faction, FactionPermission f) {
		FactionPermission own = own(state, faction);
		return own != null && own.hasPermissionEditPermission(faction) && own.role > f.role
				&& f.role < faction.getRoles().getRoles().length - 1;
	}

	private static boolean canDemote(GameClientState state, Faction faction, FactionPermission f) {
		FactionPermission own = own(state, faction);
		int top = faction.getRoles().getRoles().length - 1;
		return own != null && own.hasPermissionEditPermission(faction)
				&& (f.role >= top || own.role > f.role)
				&& f.role > faction.getRoles().getLowestIndex()
				//the top rank may only step itself down, and only while still active
				&& (f.role < top || (own == f && !f.isOverInactiveLimit(state)));
	}

	private static boolean canKick(GameClientState state, Faction faction, FactionPermission f) {
		FactionPermission own = own(state, faction);
		return own != null && own.hasKickPermission(faction) && own.role > f.role && !f.isFounder(faction);
	}
}
