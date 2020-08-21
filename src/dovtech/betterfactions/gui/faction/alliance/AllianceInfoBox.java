package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import org.newdawn.slick.Image;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;

public class AllianceInfoBox extends GUIElement {

    private Image allianceLogo;
    private AllianceMemberList memberList;
    private GUITextOverlay description;
    private AllianceStatsList allianceStats;
    private boolean init;
    private float width;
    private float height;

    public AllianceInfoBox(InputState var1) {
        super(var1);
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }


    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void draw() {
        assert this.init;
        GlUtil.glPushMatrix();



        GlUtil.glPopMatrix();
    }

    @Override
    public void onInit() {
        Alliance alliance = BetterFactions.getInstance().getFactionAlliance(GameClientState.instance.getFaction());
        allianceLogo = BetterFactions.getInstance().defaultLogo;
        if(alliance != null) {
            allianceLogo = BetterFactions.getInstance().getAllianceLogos().get(alliance);
            allianceStats = new AllianceStatsList(getState(), alliance);
            memberList = new AllianceMemberList(getState(), 150, 70, this, alliance);
            memberList.onInit();
        }

        this.init = true;
    }
}
