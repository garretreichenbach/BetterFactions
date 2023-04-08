package thederpgamer.betterfactions.gui.factionpanel;

import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerMailInputNew;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.FactionScrollableListRelation;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class BFFactionScrollableListNew extends ScrollableTableList<Faction> implements Observer {

	private final GUIElement panel;

	public BFFactionScrollableListNew(InputState inputState, GUIElement panel) {
		super(inputState, 100.0f, 100.0f, panel);
		this.panel = panel;
		((GameClientState) getState()).getFactionManager().addObserver(this);
	}

	@Override
	public void cleanUp() {
		((GameClientState) getState()).getFactionManager().deleteObserver(this);
		super.cleanUp();
	}

	@Override
	public void initColumns() {
		addColumn(Lng.str("Name"), 7, new Comparator<Faction>() {
			@Override
			public int compare(Faction o1, Faction o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});

		addColumn(Lng.str("Members"), 0, new Comparator<Faction>() {
			@Override
			public int compare(Faction o1, Faction o2) {
				return o1.getMembersUID().size() - o2.getMembersUID().size();
			}
		});

		addColumn(Lng.str("Relation"), 1, new Comparator<Faction>() {
			@Override
			public int compare(Faction o1, Faction o2) {
				FactionRelation.RType relA = ((GameClientState) getState()).getFactionManager().getRelation(((GameClientState) getState()).getPlayer().getName(), ((GameClientState) getState()).getPlayer().getFactionId(), o1.getIdFaction());
				FactionRelation.RType relB = ((GameClientState) getState()).getFactionManager().getRelation(((GameClientState) getState()).getPlayer().getName(), ((GameClientState) getState()).getPlayer().getFactionId(), o2.getIdFaction());
				return relA.sortWeight - relB.sortWeight;
			}
		});

		addDropdownFilter(new GUIListFilterDropdown<Faction, FactionRelation.RType>(FactionRelation.RType.values()) {
			@Override
			public boolean isOk(FactionRelation.RType input, Faction f) {
				final PlayerState player = ((GameClientState) getState()).getPlayer();
				final FactionManager factionManager = ((GameClientState) getState()).getGameState().getFactionManager();
				FactionRelation.RType relation = factionManager.getRelation(player.getName(), player.getFactionId(), f.getIdFaction());
				return relation == input;
			}
		}, new CreateGUIElementInterface<FactionRelation.RType>() {
			@Override
			public GUIElement create(FactionRelation.RType o) {
				GUIAncor c = new GUIAncor(getState(), 10, 24);
				GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
				a.setTextSimple(o.name());
				a.setPos(4, 4, 0);
				c.setUserPointer(o);
				c.attach(a);
				return c;
			}

			@Override
			public GUIElement createNeutral() {
				GUIAncor c = new GUIAncor(getState(), 10, 24);
				GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
				a.setTextSimple(Lng.str("Filter By Relation (off)"));
				a.setPos(4, 4, 0);
				c.attach(a);
				return c;
			}
		}, ControllerElement.FilterRowStyle.LEFT);

		addTextFilter(new GUIListFilterText<Faction>() {

			@Override
			public boolean isOk(String input, Faction listElement) {
				return listElement.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
			}
		}, ControllerElement.FilterRowStyle.RIGHT);
	}

	@Override
	protected Collection<Faction> getElementList() {
		final FactionManager factionManager = ((GameClientState) getState()).getGameState().getFactionManager();
		return factionManager.getFactionCollection();
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<Faction> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		final FactionManager factionManager = ((GameClientState) getState()).getGameState().getFactionManager();
		final PlayerState player = ((GameClientState) getState()).getPlayer();
		for(final Faction faction : collection) {
			GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable sizeText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable relationText = new GUITextOverlayTable(10, 10, getState()) {
				@Override
				public void draw() {
					FactionRelation.RType relation = factionManager.getRelation(player.getName(), player.getFactionId(), faction.getIdFaction());
					setColor(org.schema.game.client.view.gui.shiphud.newhud.ColorPalette.getColorDefault(relation, faction.getIdFaction() == player.getFactionId()));
					super.draw();
				}
			};

			GUIClippedRow nameAnchorP = new GUIClippedRow(getState());
			nameAnchorP.attach(nameText);
			nameText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return faction.getName() + (faction.isOpenToJoin() ? " " + Lng.str("(public)") : "");
				}
			});

			sizeText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return faction.getIdFaction() > 0 ? String.valueOf(faction.getMembersUID().size()) : "-";
				}
			});

			relationText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return faction.getIdFaction() == player.getFactionId() ? Lng.str("OWN") : factionManager.getRelation(player.getName(), player.getFactionId(), faction.getIdFaction()).getName();
				}
			});

			nameText.getPos().y = 4;
			sizeText.getPos().y = 4;
			relationText.getPos().y = 4;

			final FactionRow r = new FactionRow(getState(), faction, nameAnchorP, sizeText, relationText);

			r.expanded = new GUIElementList(getState());
			final GUIAncor c = new GUIAncor(getState(), 100, 100) {

				@Override
				public void draw() {
					this.setWidth(panel.getWidth());
					super.draw();
				}

			};
			final GUIScrollablePanel scrollDescription = new GUIScrollablePanel(80, 80, c, getState()) {

				@Override
				public void draw() {
					super.draw();
				}

			};
			GUITextOverlayTableInnerDescription description = new GUITextOverlayTableInnerDescription(10, 10, getState());
			scrollDescription.setContent(description);

			description.autoWrapOn = new GUIResizableElement(BFFactionScrollableListNew.this.getState()) {

				@Override
				public float getWidth() {
					return scrollDescription.getWidth()-20;
				}

				@Override
				public float getHeight() {
					return scrollDescription.getHeight()-20;
				}

				@Override
				public void cleanUp() {
				}

				@Override
				public void draw() {
				}

				@Override
				public void onInit() {
				}

				@Override
				public void setWidth(float width) {
				}

				@Override
				public void setHeight(float height) {
				}
			};
			description.setTextSimple(new Object() {
				@Override
				public String toString() {
					return faction.getDescription();
				}
			});
			description.setPos(4, 2, 0);

			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 3, 1, c);
			buttonPane.onInit();

			buttonPane.addButton(0, 0, Lng.str("VIEW RELATIONS"), GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						final PlayerGameOkCancelInput c = new PlayerGameOkCancelInput("FactionScrollableListNew_VIEW_REL", (GameClientState) getState(), 540, 400, Lng.str("Relations for %s",  faction.getName()), "") {

							@Override
							public boolean isOccluded() {
								return !(getState().getController().getPlayerInputs().isEmpty() || getState().getController().getPlayerInputs().get(getState().getController().getPlayerInputs().size() - 1) == this);
							}

							@Override
							public void onDeactivate() {

							}
							@Override
							public void pressedOK() {
								deactivate();
							}
						};
						c.getInputPanel().setCancelButton(false);
						c.getInputPanel().onInit();
						FactionScrollableListRelation factionScrollableListRelation = new FactionScrollableListRelation(getState(), c.getInputPanel().getContent(), faction) {
							@Override
							public boolean isActive() {
								return super.isActive() && (getState().getController().getPlayerInputs().isEmpty() || getState().getController().getPlayerInputs().get(getState().getController().getPlayerInputs().size() - 1) == c);
							}
						};
						factionScrollableListRelation.onInit();
						c.getInputPanel().getContent().attach(factionScrollableListRelation);
						c.activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return !isActive();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});


			GUIHorizontalButtonExpandable actionsButton = new GUIHorizontalButtonExpandable((GameClientState) getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, Lng.str("ACTIONS"), buttonPane.activeInterface);
			buttonPane.addButton(actionsButton, 1, 0);

			buttonPane.addButton(2, 0, Lng.str("MAIL"), GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new PlayerMailInputNew((GameClientState) getState(), Lng.str("faction[%s]", faction.getName()), "")).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return !isActive();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});
			c.attach(buttonPane);
		}
	}

	private class FactionRow extends Row {

		public FactionRow(InputState state, Faction f, GUIElement... elements) {
			super(state, f, elements);
			this.highlightSelect = true;
		}
	}

	@Override
	public boolean isFiltered(Faction e) {
		return !e.isShowInHub() || super.isFiltered(e);
	}
}
