package org.appwork.tools.ide.iconsetmaker.gui.icon8;

import org.appwork.swing.exttable.ExtTable;
import org.appwork.tools.ide.iconsetmaker.IconSetMaker;
import org.appwork.tools.ide.iconsetmaker.gui.Icon8Resource;

public class Icon8Table extends ExtTable<Icon8Resource> {

    private IconSetMaker owner;
    private Icon8Dialog  icon8Dialog;

    public Icon8Table(IconSetMaker owner, Icon8Dialog icon8Dialog, Icon8TableModel setTableModel) {
        super(setTableModel);
        setRowHeight(36);
        this.icon8Dialog = icon8Dialog;
        this.owner = owner;
    }

}
