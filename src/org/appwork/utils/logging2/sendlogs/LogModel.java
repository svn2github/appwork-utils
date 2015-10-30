/**
 * 
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany   
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.logging2.sendlogs;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;

public class LogModel extends ExtTableModel<LogFolder> {

    public LogModel(final java.util.List<LogFolder> folders) {
        super("LogModel");
        this.setTableData(folders);
        Collections.sort(folders, new Comparator<LogFolder>() {

            @Override
            public int compare(final LogFolder o1, final LogFolder o2) {
                return new Long(o2.getCreated()).compareTo(new Long(o1.getCreated()));
            }
        });
        for (final LogFolder folder : folders) {
            if (folder.isCurrent()) {
                folders.remove(folder);
                folders.add(0, folder);
                break;
            }
        }
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<LogFolder>(T.T.LogModel_initColumns_x_()) {

            @Override
            protected boolean getBooleanValue(final LogFolder value) {
                return value.isSelected();
            }

            @Override
            public boolean isEditable(final LogFolder obj) {

                return true;
            }

            @Override
            public boolean isSortable(final LogFolder obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(final boolean value, final LogFolder object) {
                object.setSelected(value);
            }
        });

        ExtTextColumn<LogFolder> sort;
        this.addColumn(sort = new ExtTextColumn<LogFolder>(T.T.LogModel_initColumns_time_()) {
            {
                this.setRowSorter(new ExtDefaultRowSorter<LogFolder>() {

                    @Override
                    public int compare(final LogFolder o1, final LogFolder o2) {
                        if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                            return new Long(o1.getCreated()).compareTo(new Long(o2.getCreated()));
                        } else {
                            return new Long(o2.getCreated()).compareTo(new Long(o1.getCreated()));
                        }

                    }

                });
            }

            @Override
            public String getStringValue(final LogFolder value) {

                final String from = DateFormat.getInstance().format(new Date(value.getCreated()));
                final String to = DateFormat.getInstance().format(new Date(value.getLastModified()));
                return T.T.LogModel_getStringValue_between_(from, to);
            }

            @Override
            public boolean isSortable(final LogFolder obj) {
                return false;
            }
        });

    }

}
