package dovtech.starmadeplus.gui.faction.relations;

import dovtech.starmadeplus.BetterFactions;
import dovtech.starmadeplus.faction.diplo.relations.FactionRelations;
import org.schema.game.client.view.gui.faction.FactionRelationEditPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.texture.Texture;
import org.schema.schine.input.InputState;
import java.util.ArrayList;

public class NewFactionRelationEditPanel extends FactionRelationEditPanel {

    private Faction from;
    private Faction to;
    private NewFactionRelationDialog dialog;
    private FactionRelations fromRelations;
    private FactionRelations toRelations;

    private Sprite opinionModButtonSprite;
    private Sprite fromFactionFlag;
    private Sprite toFactionFlag;

    public NewFactionRelationEditPanel(InputState inputState, Faction from, Faction to, NewFactionRelationDialog factionRelationDialog) {
        super(inputState, from, to, factionRelationDialog);
        this.from = from;
        this.to = to;
        this.dialog = factionRelationDialog;
        this.fromRelations = BetterFactions.getInstance().getRelationsTo(from, to);
        this.toRelations = BetterFactions.getInstance().getRelationsTo(to, from);

        this.opinionModButtonSprite = new Sprite(new Texture(0, fromRelations.getDisplay().spriteID, BetterFactions.getInstance().getResourcesPath() + "/gui/faction-buttons"));
    }

    @Override
    public void onInit() {
        super.onInit();

        GUITextOverlay fromOverlay = new GUITextOverlay(15, 10, this.getState());
        GUITextOverlay toOverlay = new GUITextOverlay(15, 10, this.getState());
        GUIAncor opinionAnchor = new GUIAncor(this.getState(), 100, 30);
        GUITextOverlay opinionOverlayFrom = new GUITextOverlay(15, 10, opinionAnchor.getState());
        GUITextOverlay opinionScoreOverlayFrom = new GUITextOverlay(15, 10, opinionAnchor.getState());
        GUITextOverlay opinionOverlayTo = new GUITextOverlay(15, 10, opinionAnchor.getState());
        GUITextOverlay opinionScoreOverlayTo = new GUITextOverlay(15, 10, opinionAnchor.getState());
        GUIOverlay opinionModButtonOverlay = new GUIOverlay(opinionModButtonSprite, opinionAnchor.getState());
        GUIIconButton opinionModButton = new GUIIconButton(opinionAnchor.getState(), 15, 10, opinionModButtonOverlay, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    //Todo:View Opinion Modifiers
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });
        opinionModButton.attach(opinionModButtonOverlay);

        fromOverlay.getPos().x = 15;
        opinionOverlayFrom.getPos().x = 15;
        opinionOverlayFrom.getPos().y = fromOverlay.getPos().y - 10;
        opinionScoreOverlayFrom.getPos().x = 15;
        opinionScoreOverlayFrom.getPos().y = opinionOverlayFrom.getPos().y - 10;

        toOverlay.getPos().x = 60;
        opinionOverlayTo.getPos().x = 60;
        opinionOverlayTo.getPos().y = toOverlay.getPos().y - 10;
        opinionScoreOverlayTo.getPos().y = 60;
        opinionScoreOverlayTo.getPos().y = opinionOverlayTo.getPos().y - 10;

        opinionModButtonOverlay.getPos().x = 30;
        opinionModButtonOverlay.getPos().y = fromOverlay.getPos().y - 10;
        opinionModButton.setPos(opinionModButtonOverlay.getPos());

        opinionAnchor.attach(opinionOverlayFrom);
        opinionAnchor.attach(opinionScoreOverlayFrom);
        opinionAnchor.attach(opinionOverlayTo);
        opinionAnchor.attach(opinionScoreOverlayTo);
        opinionAnchor.attach(opinionModButton);

        this.getContent().attach(opinionAnchor);

        GUITextButton.ColorPalette transparent = GUITextButton.ColorPalette.TRANSPARENT;
        GUITextButton.ColorPalette friendly = GUITextButton.ColorPalette.FRIENDLY;
        GUITextButton.ColorPalette hostile = GUITextButton.ColorPalette.HOSTILE;
        GUITextButton.ColorPalette neutral = GUITextButton.ColorPalette.NEUTRAL;


        GUIAncor changeRelationAnchor = new GUIAncor(this.getState(), 10, 20);
        ArrayList<GUITextButton> diploButtons = new ArrayList<>();

        //Invite to Pact
        if(BetterFactions.getInstance().getFactionPact(from) != null) {
            if(fromRelations.getOpinion() >= 50 && toRelations.getOpinion() >= 50) {
                GUITextButton inviteToPactButton = new GUITextButton(changeRelationAnchor.getState(), 100, 15, friendly, "Invite to Pact", this.dialog);
                inviteToPactButton.getPos().x = 15;
                inviteToPactButton.getPos().y = 15;
                inviteToPactButton.setUserPointer("INVITE_TO_PACT");
                diploButtons.add(inviteToPactButton);
            } else {
                GUITextButton inviteToPactButton = new GUITextButton(changeRelationAnchor.getState(), 100, 15, transparent, "Invite to Pact", this.dialog);
                inviteToPactButton.getPos().x = 15;
                inviteToPactButton.getPos().y = 15;
                inviteToPactButton.setUserPointer("INVALID");
                diploButtons.add(inviteToPactButton);
            }
        }

        //Request To Join Pact
        if(BetterFactions.getInstance().getFactionPact(to) != null) {
            if(toRelations.getOpinion() >= 50 && fromRelations.getOpinion() >= 50) {
                GUITextButton joinPactButton = new GUITextButton(changeRelationAnchor.getState(), 100, 15, friendly, "Ask to Join Pact", this.dialog);
                joinPactButton.getPos().x = 15;
                joinPactButton.getPos().y = 30;
                joinPactButton.setUserPointer("ASK_TO_JOIN_PACT");
                diploButtons.add(joinPactButton);
            } else {
                GUITextButton joinPactButton = new GUITextButton(changeRelationAnchor.getState(), 100, 15, transparent, "Ask to Join Pact", this.dialog);
                joinPactButton.getPos().x = 15;
                joinPactButton.getPos().y = 30;
                joinPactButton.setUserPointer("INVALID");
                diploButtons.add(joinPactButton);
            }
        }


        for(GUITextButton textButton : diploButtons) {
            changeRelationAnchor.attach(textButton);
        }
        this.getContent().attach(changeRelationAnchor);



        /*
        GUITextOverlay var1 = new GUITextOverlay(100, 10, this.getState());
        FactionRelation.RType var2 = ((GameClientState)this.getState()).getFactionManager().getRelation(this.from.getIdFaction(), this.to.getIdFaction());
        GUITextButton var3 = new GUITextButton(this.getState(), 100, 25, GUITextButton.ColorPalette.HOSTILE, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_1, this.dialog);
        GUITextButton var4 = new GUITextButton(this.getState(), 100, 25, GUITextButton.ColorPalette.NEUTRAL, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_2, this.dialog);
        GUITextButton var5 = new GUITextButton(this.getState(), 100, 25, GUITextButton.ColorPalette.FRIENDLY, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_3, this.dialog);
        var4.getPos().y = 15.0F;
        var3.getPos().y = 45.0F;
        var5.getPos().y = 75.0F;
        GUITextButton var6 = new GUITextButton(this.getState(), 150, 25, GUITextButton.ColorPalette.HOSTILE, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_4, this.dialog);
        GUITextButton var7 = new GUITextButton(this.getState(), 150, 25, GUITextButton.ColorPalette.NEUTRAL, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_5, this.dialog);
        GUITextButton var8 = new GUITextButton(this.getState(), 150, 25, GUITextButton.ColorPalette.NEUTRAL, Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_6, this.dialog);
        var6.getPos().x = 100.0F;
        var7.getPos().x = 100.0F;
        var6.getPos().y = 45.0F;
        var7.getPos().y = 75.0F;
        var8.getPos().y = 75.0F;
        FactionRelationOffer var9;
        switch(var2) {
            case ENEMY:
                var1.setTextSimple(StringTools.format(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_7, new Object[]{this.to.getName()}));
                (var9 = new FactionRelationOffer()).a = this.from.getIdFaction();
                var9.b = this.to.getIdFaction();
                var9.rel = FactionRelation.RType.NEUTRAL.code;
                if (((GameClientState)this.getState()).getPlayer().getFactionController().getRelationshipOutOffers().contains(var9)) {
                    this.getContent().attach(var6);
                    this.getContent().attach(var5);
                } else {
                    this.getContent().attach(var4);
                    this.getContent().attach(var5);
                }
                break;
            case FRIEND:
                var1.setTextSimple(StringTools.format(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_8, new Object[]{this.to.getName()}));
                (var9 = new FactionRelationOffer()).a = this.from.getIdFaction();
                var9.b = this.to.getIdFaction();
                var9.rel = FactionRelation.RType.FRIEND.code;
                if (((GameClientState)this.getState()).getPlayer().getFactionController().getRelationshipOutOffers().contains(var9)) {
                    this.getContent().attach(var3);
                    this.getContent().attach(var8);
                } else {
                    this.getContent().attach(var3);
                    this.getContent().attach(var8);
                }
                break;
            case NEUTRAL:
                var1.setTextSimple(StringTools.format(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_FACTIONRELATIONEDITPANEL_9, new Object[]{this.to.getName()}));
                (var9 = new FactionRelationOffer()).a = this.from.getIdFaction();
                var9.b = this.to.getIdFaction();
                var9.rel = FactionRelation.RType.FRIEND.code;
                if (((GameClientState)this.getState()).getPlayer().getFactionController().getRelationshipOutOffers().contains(var9)) {
                    this.getContent().attach(var3);
                    this.getContent().attach(var7);
                } else {
                    this.getContent().attach(var3);
                    this.getContent().attach(var5);
                }
                break;
            default:
                assert false;
        }

        this.getContent().attach(var1);
        var3.setUserPointer("WAR");
        var4.setUserPointer("PEACE");
        var5.setUserPointer("ALLY");
        var8.setUserPointer("ALLY_REVOKE");
        var7.setUserPointer("ALLY_OFFER_REVOKE");
        var6.setUserPointer("PEACE_OFFER_REVOKE");
         */
    }
}
