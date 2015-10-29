/**
 * 
 * ====================================================================================================================================================
 * "AppWork Utilities" License
 * ====================================================================================================================================================
 * Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * Schwabacher Straße 117
 * 90763 Fürth
 * Germany
 * 
 * === Preamble ===
 * This license establishes the terms under which the AppWork Utilities Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * These terms apply to all files that have the "AppWork Utilities" License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 
 * === 3rd Party Licences ===
 * Some parts of the AppWork Utilities use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 * 
 * === Definition: Commercial Usage ===
 * If anybody or any organization is generating income (directly or indirectly) by using "AppWork Utilities" or if there's as much as a
 * sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * If you want to use AppWork Utilities in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * If there is no commercial usage (see definition above), you may use AppWork Utilities under the terms of the
 * "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 
 * If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.swing.exttable;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

public class ExtTransferHandler<T> extends TransferHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -6250155503485735869L;

    public ExtTransferHandler() {

    }

    private ExtTable<T> table;

    public ExtTable<T> getTable() {
        return table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(final TransferSupport support) {
        if(!canImport(support)) {
            return false;
        }
        final JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();       
        if (dl.isInsertRow()) {
            final int dropRow = dl.getRow();
            try {
                return table.getModel().move((java.util.List<T>) support.getTransferable().getTransferData(table.getDataFlavor()), dropRow);

            } catch (final UnsupportedFlavorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public boolean canImport(final TransferSupport support) {
   
        if (support.isDrop()) {
            return support.isDataFlavorSupported(table.getDataFlavor());

        }
        return false;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return TransferHandler.MOVE;
    }

    public void setTable(final ExtTable<T> table) {
        this.table = table;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        return new ExtTransferable(table.getDataFlavor(), table.getModel().getSelectedObjects());

    }
}
