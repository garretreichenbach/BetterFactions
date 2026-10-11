package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;

/**
 * The rank editor: {@link FactionRankMatrix} in a window, sent as one roles rewrite on OK. Replaces the
 * engine's per-rank list, which cannot show the mod's rights grouped beside the engine's.
 */
public class FactionRankDialog extends PlayerInput {

	private final FactionRankMatrix matrix;
	private final GUIInputPanel panel;
	private boolean sent;
	private GUIAnchor content;

	public FactionRankDialog(GameClientState state, Faction faction) {
		super(state);
		matrix = new FactionRankMatrix(state, faction) {
			@Override
			public void draw() {
				//follow the window, or the columns never spread past their minimum
				if (content != null) {
					setWidth(content.getWidth());
				}
				super.draw();
			}
		};
		//the matrix needs 236 + 112 per rank across before its columns squeeze; more ranks scroll
		panel = new GUIInputPanel("BF_FACTION_RANKS", state, 920, 520, this, Lng.str("Ranks"), "") {
			@Override
			public void onInit() {
				super.onInit();
				content = ((GUIDialogWindow) background).getMainContentPane().getContent(0);
				GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, content, getState());
				matrix.onInit();
				sc.setContent(matrix);
				sc.onInit();
				content.attach(sc);
			}
		};
	}

	@Override
	protected void apply() {
		if (!sent) {
			getState().getFactionManager().sendFactionRoles(matrix.getRoles());
			sent = true;
		}
	}

	@Override
	public GUIElement getInputPanel() {
		return panel;
	}

	@Override
	public void onDeactivate() {
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
	}
}
