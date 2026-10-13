package videogoose.betterfactions.emblem;

import videogoose.betterfactions.emblem.FactionEmblemCache;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.input.InputState;
import org.schema.schine.graphicsengine.core.GlUtil;

import java.util.function.Supplier;

/**
 * A faction's emblem, wherever one belongs. Draws nothing at all when the faction has no emblem, when
 * the image has not arrived yet, or when the server never turned uploads on - so a caller attaches one
 * unconditionally and does not have to ask any of those questions.
 *
 * <p>The faction arrives through a supplier rather than a reference because the panels this sits in
 * outlive their selection: the diplomacy header is filled once and then shown a different faction.</p>
 */
public class GUIFactionEmblem extends GUIAnchor {

	private final Supplier<Faction> faction;
	private final int size;

	public GUIFactionEmblem(InputState state, int size, Supplier<Faction> faction) {
		super(state, size, size);
		this.size = size;
		this.faction = faction;
	}

	@Override
	public void draw() {
		Faction f = faction.get();
		if (f == null) {
			return;
		}
		Sprite s = FactionEmblemCache.get((GameClientState) getState(), f);
		if (s == null) {
			return;
		}
		s.setPositionCenter(false);
		//width/height do not resize the quad - it draws at image size times scale. The sprite is shared
		//across every emblem on screen, so the scale is set fresh on each draw
		s.setScale((float) size / Math.max(1, s.getWidth()), (float) size / Math.max(1, s.getHeight()), 1);
		GlUtil.glPushMatrix();
		transform();
		s.draw();
		GlUtil.glPopMatrix();
	}

	@Override
	public float getWidth() {
		return size;
	}

	@Override
	public float getHeight() {
		return size;
	}
}
