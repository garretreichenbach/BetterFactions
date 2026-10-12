package videogoose.betterfactions.gui;

import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.input.InputState;

/**
 * The header of the admin profiler's BATTLES tab: totals, the queue and recent results, from the
 * server's last report. The list below it is {@link BattleAdminList}.
 */
public class BattlesProfilerText extends GUITextOverlayTable {

	public BattlesProfilerText(InputState state) {
		super(10, 10, state);
		autoHeight = true;
	}

	@Override
	public void draw() {
		BattleAdminList.requestReport();
		String report = BattleAdminList.header;
		setTextFromArray((report.isEmpty() ? "Waiting for the server..." : report).split("\n"));
		super.draw();
	}
}
