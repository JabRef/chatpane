package org.jabref.chatpane.skin;

import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;

import org.jabref.chatpane.ChatMessage;

/// The bubble list's [VirtualFlow], which can stay pinned to the end while the newest bubble
/// settles its height.
///
/// A new or grown bubble knows its height only once its text area laid out, a pulse after the
/// list scrolled to it. The flow keeps its scroll position as a fraction, so the grown content
/// slid the newest bubble partly out of view — and an answer growing word by word flickered
/// between shown whole and cut off. While [#follow()] holds, every layout pass that ends short of
/// the end lays out again from the end, in the same pulse, so no frame shows the cut-off state.
///
/// Any position change outside a layout pass is the user's (wheel, scroll bar, keys): it unpins
/// the flow unless it ends at the very end, which pins it again, as chat clients do.
///
/// Unpinned, the position as a fraction would also move the view when messages above it are
/// removed: [#removing(int, int)] keeps the top message where it is instead.
// [impl->dsn~bubble-view~4]
final class BubbleFlow extends VirtualFlow<ListCell<ChatMessage>> {

    /// At or past this position the flow is at the end; positions are fractions of the content.
    private static final double END = 1 - 1e-6;

    /// Layout passes from the end per layout: a grown bubble settles in one; the bound keeps a
    /// height that never settles from hanging the pulse.
    private static final int MAX_REPINS = 3;

    private boolean following = true;
    private boolean inLayout;

    /// The cell to put at the top in the next layout, and its offset there; `-1` for none.
    private int anchorIndex = -1;
    private double anchorOffset;

    BubbleFlow() {
        positionProperty().addListener((_, _, position) -> {
            if (!inLayout) {
                following = position.doubleValue() >= END;
            }
        });
    }

    /// Pins the flow to the end until the user scrolls away.
    void follow() {
        following = true;
        requestLayout();
    }

    /// Unpins the flow: the view must stay where it is.
    void stay() {
        following = false;
    }

    /// Keeps the top message where it is while `count` items from `from` are removed: called before
    /// the removal, it takes effect in the next layout. Pinned to the end, the flow stays there.
    void removing(int from, int count) {
        IndexedCell<?> top = getFirstVisibleCell();
        if (following || top == null) {
            return;
        }
        int index = top.getIndex();
        double offset = top.getLayoutY();
        if (index >= from + count) {
            index -= count;
        } else if (index >= from) {
            // The top message itself goes: its successor takes its place.
            offset = 0;
            index = from;
        }
        anchorIndex = index;
        anchorOffset = offset;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        inLayout = true;
        try {
            super.layoutChildren();
            if (anchorIndex >= 0) {
                if (!following && anchorIndex < getCellCount()) {
                    // scrollToTop piles the cells; scrollPixels moves laid-out ones.
                    scrollToTop(anchorIndex);
                    super.layoutChildren();
                    scrollPixels(-anchorOffset);
                }
                anchorIndex = -1;
            }
            for (int pass = 0; following && getCellCount() > 0 && getPosition() < END && pass < MAX_REPINS; pass++) {
                setPosition(1);
                super.layoutChildren();
            }
        } finally {
            inLayout = false;
        }
    }

    /// The bubble list's skin, with a [BubbleFlow] as its flow.
    static final class Skin extends ListViewSkin<ChatMessage> {

        Skin(ListView<ChatMessage> list) {
            super(list);
        }

        @Override
        protected VirtualFlow<ListCell<ChatMessage>> createVirtualFlow() {
            return new BubbleFlow();
        }

        BubbleFlow flow() {
            return (BubbleFlow) getVirtualFlow();
        }
    }
}
