package videogoose.betterfactions.gui;

import org.schema.schine.graphicsengine.forms.gui.GUIColoredRectangle;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;

import javax.vecmath.Vector4f;

/**
 * A plainer mark for the selected row of a single-select table. The engine paints the selected row
 * with the same faint colour as a hovered one, which is easy to lose; this lays a tint and a bright
 * bar down the left edge over the row's background while it is selected.
 *
 * <p>Make one in the row's {@code onInit} after {@code super.onInit()} (the background exists from
 * then), and call {@link #update} at the top of its {@code draw}.
 */
final class SelectionMark {
    private static final Vector4f FILL = new Vector4f(0.3f, 0.6f, 1.0f, 0.22f);
    private static final Vector4f BAR = new Vector4f(0.45f, 0.8f, 1.0f, 1.0f);
    private static final Vector4f NONE = new Vector4f();

    private final ScrollableTableList<?>.Row row;
    private final GUIColoredRectangle fill;
    private final GUIColoredRectangle bar;

    SelectionMark(ScrollableTableList<?>.Row row) {
        this.row = row;
        fill = new GUIColoredRectangle(row.getState(), 0, 0, new Vector4f());
        bar = new GUIColoredRectangle(row.getState(), 3, 0, new Vector4f());
        row.bg.attach(fill);
        row.bg.attach(bar);
    }

    void update(boolean selected) {
        int height = (int) row.bg.getHeight();
        fill.setWidth(row.bg.getWidth());
        fill.setHeight(height);
        bar.setHeight(height);
        fill.getColor().set(selected ? FILL : NONE);
        bar.getColor().set(selected ? BAR : NONE);
    }
}
