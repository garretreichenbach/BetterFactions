package dovtech.betterfactions.gui.faction.alliance;

import api.common.GameClient;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.util.DataUtil;
import org.newdawn.slick.Image;
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
    private Alliance alliance;

    public AllianceInfoBox(InputState inputState, float width, float height) {
        super(inputState);
        this.width = width;
        this.height = height;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void draw() {
        if(!init) this.onInit();

        if(alliance != null) {
            GlUtil.glPushMatrix();
            memberList.draw();
            memberList.drawAttached();
            description.draw();
            description.drawAttached();
            //allianceLogo.draw();
            GlUtil.glPopMatrix();

        }
    }

    @Override
    public void onInit() {
        alliance = DataUtil.getBetterFaction(GameClient.getClientState().getFaction()).getAlliance();
        //allianceLogo = BetterFactions.getInstance().defaultLogo;
        if(alliance != null) {
            //allianceLogo = BetterFactions.getInstance().getAllianceLogos().get(alliance);
            allianceStats = new AllianceStatsList(getState(), alliance);
            allianceStats.onInit();
            memberList = new AllianceMemberList(getState(), 150, 70, this, alliance);
            memberList.onInit();
            description = new GUITextOverlay(15, 15, getState());
            description.onInit();
            description.setTextSimple(alliance.getDescription());
        } else {
            description = new GUITextOverlay(15, 15, getState());
            description.onInit();
            description.setTextSimple("No Alliance");
        }

        this.init = true;
    }
}
