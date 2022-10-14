package thederpgamer.betterfactions.gui.factionpanel;

import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionDialogNew;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.FactionIncomingInvitesPlayerInputNew;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea.HButtonType;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

public class BFFactionOptionPersonalContent extends GUIAncor {

	private BFFactionPanelNew panel;

	public BFFactionOptionPersonalContent(InputState state, BFFactionPanelNew panel) {
		super(state);
		this.panel = panel;
	}

	public PlayerState getOwnPlayer() {
		return this.getState().getPlayer();
	}

	public Faction getOwnFaction() {
		return this.getState().getFaction();
	}

	@Override
	public GameClientState getState() {
		return ((GameClientState) super.getState());
	}

	@Override
	public void onInit() {
		{
			GUIHorizontalButtonTablePane p = new GUIHorizontalButtonTablePane(getState(), 2, 2, Lng.str("Personal Options"), this);
			p.onInit();
			p.activeInterface = panel;
			p.addButton(0, 0, Lng.str("Create Faction"), HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						if (getState().getController().getPlayerInputs().isEmpty()) {
							FactionDialogNew d = new FactionDialogNew(getState(), Lng.str("Create Faction"));
							d.activate();
						}
					}
				}				@Override
				public boolean isOccluded() {
					return !isActive();
				}


			}, null);

			p.addButton(1, 0, Lng.str("Leave Faction"), HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						PlayerGameOkCancelInput confirm = new PlayerGameOkCancelInput("CONFIRM", getState(),
								Lng.str("Confirm"),
								Lng.str("Do you really want to leave this faction?\n"
										+ "You'll be unable to access any ship/structure that belongs to this\n"
										+ "faction except for your ships that use the Personal Permission Rank.\n\n"
										+ "If you are the last member, the faction will also automatically disband!")) {

							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								System.err.println("[CLIENT][FactionControlManager] leaving Faction");
								getState().getPlayer().getFactionController().leaveFaction();
								deactivate();
							}
						};
						confirm.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return getOwnFaction() != null;
				}

				@Override
				public boolean isActive(InputState state) {
					return true;
				}
			});

			p.addButton(0, 1, new Object() {
				@Override
				public String toString() {
					return Lng.str("Received Invites (%s)",  getState().getPlayer().getFactionController().getInvitesIncoming().size());
				}

			}, HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						FactionIncomingInvitesPlayerInputNew f = new FactionIncomingInvitesPlayerInputNew(getState());
						f.activate();
					}
				}
			}, null);
			setPos(1, 0, 0);
			attach(p);
		}
	}
}