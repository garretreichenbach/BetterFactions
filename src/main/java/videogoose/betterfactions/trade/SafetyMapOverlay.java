package videogoose.betterfactions.trade;

import api.event.render.GameMapDrawListener;
import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.effects.ConstantIndication;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.debug.DebugLineRenderer;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUICheckBox;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;

/**
 * The galaxy map shows how safe trade is around the system being looked at: each system nearby is outlined from
 * red (raided, empty, far from anyone reputable) to green, and the system itself is labelled with its safety.
 */
public final class SafetyMapOverlay implements GameMapDrawListener {

	/** Systems around the selected one that are outlined. */
	private static final int REACH = 1;

	/** The map tools panel's "Trade Safety" checkbox. */
	public static boolean shown = true;

	/** The "Trade Safety" checkbox, in the map tools panel's free slot above "Warp Gates". */
	public static void addToggle(GUIAnchor panel) {
		GUICheckBox box = new GUICheckBox(panel.getState()) {
			@Override
			protected boolean isActivated() {
				return shown;
			}

			@Override
			protected void activate() {
				shown = true;
			}

			@Override
			protected void deactivate() {
				shown = false;
			}
		};
		box.setScale(0.5f, 0.5f, 0.5f);
		box.setPos(601, 80, 0);
		panel.attach(box);
		GUITextOverlay text = new GUITextOverlay(120, 20, panel.getState());
		text.setTextSimple(Lng.str("Trade Safety"));
		text.setPos(617, 80, 0);
		panel.attach(text);
	}

	@Override
	public void galaxy_DrawQuads(GameMapDrawer drawer) {
		if (!shown) {
			return;
		}
		Vector3i at = drawer.getGameMapPosition().getCurrentSysPos();
		RouteSafety rs = RouteSafety.client();
		float h = GameMapDrawer.halfsize * 0.97f;
		DebugLineRenderer dlr = DebugLineRenderer.get();
		dlr.begin();
		Vector3i s = new Vector3i();
		for (int x = -REACH; x <= REACH; x++) {
			for (int y = -REACH; y <= REACH; y++) {
				for (int z = -REACH; z <= REACH; z++) {
					s.set(at.x + x, at.y + y, at.z + z);
					double safe = rs.systemSafety(s);
					float a = x == 0 && y == 0 && z == 0 ? 0.8f : 0.35f;
					cube(dlr, s.x * GameMapDrawer.size, s.y * GameMapDrawer.size, s.z * GameMapDrawer.size, h,
							(float) Math.min(1, 2 * (1 - safe)), (float) Math.min(1, 2 * safe), 0.1f, a);
				}
			}
		}
		dlr.end();

		Transform t = new Transform();
		t.setIdentity();
		t.origin.set(at.x * GameMapDrawer.size, at.y * GameMapDrawer.size - GameMapDrawer.halfsize * 0.8f, at.z * GameMapDrawer.size);
		HudIndicatorOverlay.toDrawMapTexts.add(new ConstantIndication(t, Lng.str("Trade safety %s", RouteSafety.percent(rs.systemSafety(at)))));
	}

	private static void cube(DebugLineRenderer d, float cx, float cy, float cz, float h, float r, float g, float b, float a) {
		float x0 = cx - h, x1 = cx + h, y0 = cy - h, y1 = cy + h, z0 = cz - h, z1 = cz + h;
		d.line(x0, y0, z0, x1, y0, z0, r, g, b, a);
		d.line(x1, y0, z0, x1, y0, z1, r, g, b, a);
		d.line(x1, y0, z1, x0, y0, z1, r, g, b, a);
		d.line(x0, y0, z1, x0, y0, z0, r, g, b, a);
		d.line(x0, y1, z0, x1, y1, z0, r, g, b, a);
		d.line(x1, y1, z0, x1, y1, z1, r, g, b, a);
		d.line(x1, y1, z1, x0, y1, z1, r, g, b, a);
		d.line(x0, y1, z1, x0, y1, z0, r, g, b, a);
		d.line(x0, y0, z0, x0, y1, z0, r, g, b, a);
		d.line(x1, y0, z0, x1, y1, z0, r, g, b, a);
		d.line(x1, y0, z1, x1, y1, z1, r, g, b, a);
		d.line(x0, y0, z1, x0, y1, z1, r, g, b, a);
	}
}
