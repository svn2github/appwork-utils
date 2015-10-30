/**
 * 
 * ====================================================================================================================================================
 * 	    "AppWork Utilities" License
 * 	    The "AppWork Utilities" will be called [The Product] from now on.
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
package org.appwork.swing.exttable.columns;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.renderercomponents.RendererProgressBar;

abstract public class ExtProgressColumn<E> extends ExtColumn<E> {
    /**
     * @author Thomas
     * 
     * @param <E>
     */
    public static final class IndeterminatedRenderer<E> extends RendererProgressBar {
        /**
         * 
         */
        private static final long    serialVersionUID = 1L;
        private long                 cleanupTimer     = 0;
        private volatile boolean     indeterminate    = false;
        private volatile long        timer            = 0;
        private ExtProgressColumn<E> column;

        /**
         * @param extProgressColumn
         */
        public IndeterminatedRenderer(ExtProgressColumn<E> column) {
            this.column = column;
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean isDisplayable() {
            return true;
        }

        @Override
        public boolean isIndeterminate() {
            return this.indeterminate;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public void repaint() {
            if (column == null) {
                return;
            }
            if (column.isModifying()) {
                return;
            }
            final ExtTableModel<E> mod = column.getModel();
            if (mod != null && mod.getTable() != null && column.indeterminatedRenderer.isIndeterminate() && mod.getTable().isShowing()) {

                // cleanup map in case we removed a indeterminated value
                if (System.currentTimeMillis() - this.cleanupTimer > 30000) {
                    Entry<E, Long> next;
                    for (final Iterator<Entry<E, Long>> it = column.map.entrySet().iterator(); it.hasNext();) {
                        next = it.next();
                        final long lastUpdate = System.currentTimeMillis() - next.getValue();
                        if (lastUpdate > 5000) {
                            it.remove();
                        }
                    }

                    this.cleanupTimer = System.currentTimeMillis();
                    if (column.map.size() == 0 && column.indeterminatedRenderer.isIndeterminate()) {
                        column.indeterminatedRenderer.setIndeterminate(false);
                        return;
                    }

                }
                if (column.columnIndex >= 0) {
                    if (System.currentTimeMillis() - this.timer > 1000 / column.getFps()) {
                        // mod._fireTableStructureChanged(mod.getTableData(),
                        // false);
                        // System.out.println(getLocation());
                        column.repaint();
                        this.timer = System.currentTimeMillis();
                    }
                }
            }
        }

        @Override
        public void repaint(final Rectangle r) {
            this.repaint();
        }

        @Override
        public void setIndeterminate(final boolean newValue) {
            if (newValue == this.indeterminate) {
                return;
            }
            this.indeterminate = newValue;
            super.setIndeterminate(newValue);
        }
    }

    private static final long serialVersionUID = -2473320164484034664L;

    public static double getPercentString(final long current, final long total) {
        if (total <= 0) {
            return 0.00d;
        }
        return current * 10000 / total / 100.0d;
    }

    protected RendererProgressBar determinatedRenderer;
    protected Border              defaultBorder;
    private RendererProgressBar   indeterminatedRenderer;
    private RendererProgressBar   renderer;
    private HashMap<E, Long>      map;

    private int                   columnIndex = -1;

    /**
     * 
     */
    public ExtProgressColumn(final String title) {
        this(title, null);
    }

    public ExtProgressColumn(final String name, final ExtTableModel<E> extModel) {
        super(name, extModel);
        this.map = new HashMap<E, Long>();
        this.determinatedRenderer = initDeterminatedRenderer();

        this.indeterminatedRenderer = initIndeterminatedRenderer();

        // this.getModel().addTableModelListener(new TableModelListener() {
        //
        // @Override
        // public void tableChanged(final TableModelEvent e) {
        // switch (e.getType()) {
        // case TableModelEvent.DELETE:
        // case TableModelEvent.UPDATE:
        // System.out.println(e);
        // if (ExtProgressColumn.this.map.size() == 0) {
        // ExtProgressColumn.this.indeterminatedRenderer.setIndeterminate(false);
        // }
        // }
        //
        // }
        // });
        this.renderer = this.determinatedRenderer;
        this.defaultBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 2, 1), this.determinatedRenderer.getBorder());

        this.setRowSorter(new ExtDefaultRowSorter<E>() {

            @Override
            public int compare(final E o1, final E o2) {
                final double v1 = (double) ExtProgressColumn.this.getValue(o1) / ExtProgressColumn.this.getMax(o1);
                final double v2 = (double) ExtProgressColumn.this.getValue(o2) / ExtProgressColumn.this.getMax(o2);

                if (v1 == v2) {
                    return 0;
                }
                if (this.getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                    return v1 > v2 ? -1 : 1;
                } else {
                    return v2 > v1 ? -1 : 1;
                }
            }

        });
    }

    protected IndeterminatedRenderer<E> initIndeterminatedRenderer() {
        return new IndeterminatedRenderer<E>(this);
    }

    protected RendererProgressBar initDeterminatedRenderer() {
        return new RendererProgressBar();
    }

    /**
     * 
     */

    @Override
    public void configureEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.prepareGetter(value);
        if (this.renderer == this.determinatedRenderer) {
            // Normalize value and maxvalue to fit in the integer range
            long m = this.getMax(value);
            long v = 0;
            if (m >= 0) {
                v = this.getValue(value);
                final double factor = Math.max(v / (double) Integer.MAX_VALUE, m / (double) Integer.MAX_VALUE);

                if (factor >= 1.0) {
                    v /= factor;
                    m /= factor;
                }
            }
            v = Math.min(m, v);
            // take care to set the maximum before the value!!
            this.renderer.setMaximum((int) m);
            this.renderer.setValue((int) v);
            this.renderer.setMinimum(0);
            this.renderer.setString(this.getString(value, v, m));
        } else {
            this.renderer.setString(this.getString(value, -1, -1));
            if (!this.renderer.isIndeterminate()) {
                this.renderer.setIndeterminate(true);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.rapidshare.rsmanager.gui.components.table.ExtColumn#getCellEditorValue ()
     */
    @Override
    public Object getCellEditorValue() {

        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        return null;
    }

    /**
     * @return
     */
    protected int getFps() {
        return 15;
    }

    protected long getMax(final E value) {
        return 100;
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.columnIndex = column;
        if (this.isIndeterminated(value, isSelected, hasFocus, row, column)) {
            this.renderer = this.indeterminatedRenderer;
            if (this.map.size() == 0) {
                if (!this.indeterminatedRenderer.isIndeterminate()) {
                    this.map.put(value, System.currentTimeMillis());
                    this.indeterminatedRenderer.setIndeterminate(true);
                }
            }
            this.map.put(value, System.currentTimeMillis());
        } else {
            this.renderer = this.determinatedRenderer;
            this.map.remove(value);
            if (this.map.size() == 0) {
                if (this.indeterminatedRenderer.isIndeterminate()) {
                    this.indeterminatedRenderer.setIndeterminate(false);
                }
            }
        }
        return this.renderer;
    }

    abstract protected String getString(E value, long current, long total);

    @Override
    protected String getTooltipText(final E value) {
        long v = this.getValue(value);
        long m = this.getMax(value);
        final double factor = Math.max(v / (double) Integer.MAX_VALUE, m / (double) Integer.MAX_VALUE);

        if (factor >= 1.0) {
            v /= factor;
            m /= factor;
        }
        return this.getString(value, v, m);
    }

    abstract protected long getValue(E value);

    /*
     * (non-Javadoc)
     * 
     * @see com.rapidshare.rsmanager.gui.components.table.ExtColumn#isEditable(java .lang.Object)
     */
    @Override
    public boolean isEditable(final E obj) {

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.rapidshare.rsmanager.gui.components.table.ExtColumn#isEnabled(java .lang.Object)
     */
    @Override
    public boolean isEnabled(final E obj) {

        return true;
    }

    /**
     * @param column
     * @param row
     * @param hasFocus
     * @param isSelected
     * @param value
     * @return
     */
    protected boolean isIndeterminated(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.rapidshare.rsmanager.gui.components.table.ExtColumn#isSortable(java .lang.Object)
     */
    @Override
    public boolean isSortable(final E obj) {

        return true;
    }

    /**
     * @param value
     */
    protected void prepareGetter(final E value) {
    }

    @Override
    public void resetEditor() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetRenderer() {
        this.renderer.setOpaque(false);
        this.renderer.setStringPainted(true);
        this.renderer.setBorder(this.defaultBorder);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.rapidshare.rsmanager.gui.components.table.ExtColumn#setValue(java .lang.Object, java.lang.Object)
     */
    @Override
    public void setValue(final Object value, final E object) {

    }

}
