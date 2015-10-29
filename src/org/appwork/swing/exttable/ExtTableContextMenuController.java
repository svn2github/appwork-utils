/**
 * 
 * ====================================================================================================================================================
 * 	    "MyJDownloader Client" License
 * 	    The "MyJDownloader Client" will be called [The Product] from now on.
 * ====================================================================================================================================================
 * 	    Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * 	    Schwabacher Straße 117
 * 	    90763 Fürth
 * 	    Germany   
 * === Preamble ===
 * 	This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * 	The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * 	These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 * 	Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * 	to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 * 	If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's as much as a 
 * 	sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * 	If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * 	Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * 	If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 * 	"GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 * 	If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.swing.exttable;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPopupMenu;

public abstract class ExtTableContextMenuController<T extends ExtTable<?>> implements MouseListener {

    protected T table;

    public ExtTableContextMenuController(final T table) {
        this.table = table;
    }

    protected abstract JPopupMenu getEmptyPopup();

    protected abstract JPopupMenu getPopup();

    public void mouseClicked(final MouseEvent e) {

    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
    }

    public void mouseReleased(final MouseEvent e) {

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            final int row = this.table.rowAtPoint(e.getPoint());
            final Object obj = this.table.getModel().getObjectbyRow(row);
            if (obj == null || row == -1) {
                /* no object under mouse, lets clear the selection */
                this.table.getModel().clearSelection();
                final JPopupMenu pu = this.getEmptyPopup();
                if (pu != null && pu.getComponentCount() > 0) {
                    pu.show(this.table, e.getPoint().x, e.getPoint().y);
                }
                return;
            } else {
                /* check if we need to select object */
                if (!this.table.isRowSelected(row)) {
                    this.table.getModel().clearSelection();
                    this.table.addRowSelectionInterval(row, row);
                }
                final JPopupMenu pu = this.getPopup();
                if (pu != null && pu.getComponentCount() > 0) {
                    pu.show(this.table, e.getPoint().x, e.getPoint().y);
                }
            }
        }
    }
}
