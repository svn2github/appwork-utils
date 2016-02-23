package org.appwork.tools.ide.iconsetmaker.gui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.tools.ide.iconsetmaker.gui.icon8.Style;

public interface IconSetterConfig extends ConfigInterface {
    @DefaultIntValue(0)
    public int getColor();

    public void setColor(int i);

    @DefaultEnumValue("ALL")
    public Style getLastUsedStyle();

    public void setLastUsedStyle(Style style);
}
