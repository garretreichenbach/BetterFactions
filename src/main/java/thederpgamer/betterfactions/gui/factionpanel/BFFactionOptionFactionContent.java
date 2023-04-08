package thederpgamer.betterfactions.gui.factionpanel;

import org.schema.game.client.controller.PlayerCreateFactionNewsInputNew;
import org.schema.game.client.controller.PlayerFactionPointDialogNew;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionOfferDialog;
import org.schema.game.client.controller.manager.ingame.faction.FactionPersonalEnemyDialog;
import org.schema.game.client.controller.manager.ingame.faction.FactionRolesDialogNew;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.FactionOutgoingInvitesPlayerInputNew;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class BFFactionOptionFactionContent extends GUIAncor {
	private final BFFactionPanelNew panel;

	public BFFactionOptionFactionContent(InputState state, BFFactionPanelNew panel) {
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

			GUIElementList mainList = new GUIElementList(getState()) {

				/* (non-Javadoc)
				 * @see org.schema.schine.graphicsengine.forms.gui.GUIElementList#draw()
				 */
				@Override
				public void draw() {
					if (getOwnFaction() != null) {
						super.draw();
					}
				}

			};
			GUIHorizontalButtonTablePane p = new GUIHorizontalButtonTablePane(getState(), 2, 5, Lng.str("Faction Control Panel"), this);
			p.onInit();
			p.activeInterface = panel;
			p.addButton(0, 0, Lng.str("Invite Player"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						getState().getGlobalGameControlManager()
								.getIngameControlManager().getPlayerGameControlManager().getFactionControlManager().openInvitePlayerDialog();
					}
				}				@Override
				public boolean isOccluded() {
					return !isActive();
				}


			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasInvitePermission();
				}
			});

			p.addButton(1, 0, new Object() {
				@Override
				public String toString() {
					return Lng.str("Sent Pending Invites (%s)",  getState().getPlayer().getFactionController().getInvitesOutgoing().size());
				}

			}, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						FactionOutgoingInvitesPlayerInputNew f = new FactionOutgoingInvitesPlayerInputNew(getState());
						f.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasInvitePermission();
				}
			});

			p.addButton(0, 1, Lng.str("Received Diplomacy Offers"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						FactionOfferDialog o = new FactionOfferDialog(getState());
						o.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasRelationshipPermission();
				}
			});

			p.addButton(1, 1, Lng.str("Post News"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						PlayerCreateFactionNewsInputNew n = new PlayerCreateFactionNewsInputNew(getState());
						n.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasDescriptionAndNewsPostPermission();
				}
			});

			p.addButton(0, 2, new Object() {
				@Override
				public String toString() {
					return Lng.str("Faction Points (%s)", (int) getState().getPlayer().getFactionController().getFactionPoints());
				}

			}, GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						PlayerFactionPointDialogNew p = new PlayerFactionPointDialogNew(getState());
						p.activate();
					}
				}
			}, null);

			p.addButton(1, 2, Lng.str("Edit Ranks"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						FactionRolesDialogNew d = new FactionRolesDialogNew(getState(), getOwnFaction());
						d.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasPermissionEditPermission();
				}
			});

			p.addButton(0, 3, Lng.str("Edit Faction Description"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						Faction ownFaction = getOwnFaction();
						PlayerTextAreaInput t = new PlayerTextAreaInput("FactionOptionFactionContent_EDIT_FACTION_DESC", getState(), 140, 5, Lng.str("Edit Faction Description"),
								"",
								ownFaction != null ? ownFaction.getDescription() : Lng.str("ERROR: NO FACTION")) {
							@Override
							public String[] getCommandPrefixes() {
								return null;
							}

							@Override
							public String handleAutoComplete(String s, TextCallback callback,
							                                 String prefix) throws PrefixNotFoundException {
								return null;
							}

							@Override
							public void onFailedTextCheck(String msg) {
							}

							@Override
							public boolean isOccluded() {
								return false;
							}

							@Override
							public void onDeactivate() {
							}

							@Override
							public boolean onInput(String entry) {
								return getState().getPlayer().getFactionController().editDescriptionClient(entry);
							}
						};
						t.activate();
						t.setInputChecker(new InputChecker() {
							@Override
							public boolean check(String entry, TextCallback callback) {
								return true;
							}
						});
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasDescriptionAndNewsPostPermission();
				}
			});

			p.addButton(1, 3, Lng.str("Manage Individual Enemies"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {
						FactionPersonalEnemyDialog f = new FactionPersonalEnemyDialog(getState(), getOwnFaction());
						f.activate();
					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasRelationshipPermission();
				}
			});

			p.addButton(0, 4, Lng.str("Reset Homebase"), GUIHorizontalArea.HButtonType.BUTTON_RED_MEDIUM, new GUICallback() {
				@Override
				public boolean isOccluded() {
					return !isActive();
				}

				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if (event.pressedLeftMouse()) {

						PlayerGameOkCancelInput check = new PlayerGameOkCancelInput("CONFIRM", getState(),
								Lng.str("Confirm reset Homebase"), Lng.str("Do you really want to do this?")) {

							@Override
							public boolean isOccluded() {
								return false;
							}

							@Override
							public void onDeactivate() {
							}

							@Override
							public void pressedOK() {
								deactivate();
								getState().getFactionManager().sendClientHomeBaseChange(getState().getPlayer().getName(), getState().getPlayer().getFactionId(), "");


							}
						};
						check.activate();

					}
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState state) {
					return true;
				}

				@Override
				public boolean isActive(InputState state) {
					return getOwnPlayer().getFactionController().hasHomebaseEditPermission()
							&& getOwnPlayer().getFactionController().hasHomebase();
				}


			});

			int dist = 208;
			GUICheckBoxTextPair pairAttOnHostile = new GUICheckBoxTextPair(getState(), Lng.str("Declare war on hostile action:"), dist, 30) {

				@Override
				public void activate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestAutoDeclareWar(getOwnPlayer(), true);
					}
				}				@Override
				public boolean isActivated() {
					return getOwnFaction() != null && getOwnFaction().isAutoDeclareWar();
				}

				@Override
				public void deactivate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestAutoDeclareWar(getOwnPlayer(), false);
					}
				}


			};
			GUICheckBoxTextPair pairNeutral = new GUICheckBoxTextPair(getState(), Lng.str("Consider neutrals enemy: "), dist, 20) {

				@Override
				public boolean isActivated() {
					return getOwnFaction() != null && getOwnFaction().isAttackNeutral();
				}

				@Override
				public void deactivate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestAttackNeutral(getOwnPlayer(), false);
					}
				}

				@Override
				public void activate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestAttackNeutral(getOwnPlayer(), true);
					}
				}
			};
			GUICheckBoxTextPair publicFaction = new GUICheckBoxTextPair(getState(), Lng.str("Public Faction (join w/o invite): "), dist, 20) {

				@Override
				public boolean isActivated() {
					return getOwnFaction() != null && getOwnFaction().isOpenToJoin();
				}

				@Override
				public void deactivate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestOpenFaction(getOwnPlayer(), false);
					}
				}

				@Override
				public void activate() {
					if (BFFactionOptionFactionContent.this.isActive() && getOwnFaction() != null) {
						getOwnFaction().clientRequestOpenFaction(getOwnPlayer(), true);
					}
				}
			};

			pairAttOnHostile.setPos(4, 10, 0);
			pairNeutral.setPos(4, 0, 0);
			publicFaction.setPos(4, 0, 0);
			mainList.add(new GUIListElement(p, p, getState()));
			mainList.add(new GUIListElement(pairAttOnHostile, pairAttOnHostile, getState()));
			mainList.add(new GUIListElement(pairNeutral, pairNeutral, getState()));
			mainList.add(new GUIListElement(publicFaction, publicFaction, getState()));

			mainList.setPos(1, 0, 0);
			attach(mainList);
		}

	}
}
