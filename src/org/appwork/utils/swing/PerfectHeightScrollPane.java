package org.appwork.utils.swing;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseWheelEvent;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class PerfectHeightScrollPane extends JScrollPane {

    public PerfectHeightScrollPane(Component view) {
        super(view);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        setWheelScrollingEnabled(false);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof MouseWheelEvent) {
            getParent().dispatchEvent(e);
        } else {
            super.processEvent(e);
        }

    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        Insets borderInsets = getBorder().getBorderInsets(this);
        Component view = getViewport().getView();
        ret.height = view.getPreferredSize().height + 20 + borderInsets.top + borderInsets.bottom + getColumnHeader().getPreferredSize().height;
        return ret;
    }
}