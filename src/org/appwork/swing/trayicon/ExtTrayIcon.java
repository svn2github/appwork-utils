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
package org.appwork.swing.trayicon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

/**
 * @author thomas
 * 
 */
public class ExtTrayIcon extends TrayIcon implements MouseListener, MouseMotionListener {

    private static int TOOLTIP_DELAY = 1000;
    private Component dummy;

    private MouseEvent lastEvent;

    private Point max;
    // private TrayIcon trayIcon;
    private Point min;
    private final java.util.List<MouseListener> mouseListeners;
    private Thread mouseLocationObserver;
    private boolean mouseover;
    private Dimension size;
    private final java.util.List<TrayMouseListener> traymouseListeners;

    /**
     * @param icon
     * @param title
     */
    public ExtTrayIcon(final Image icon, final String title) {
        super(icon, title);
        this.setImageAutoSize(true);
        this.mouseListeners = new ArrayList<MouseListener>();
        this.traymouseListeners = new ArrayList<TrayMouseListener>();
        super.addMouseListener(this);
        super.addMouseMotionListener(this);
    }

    
    public void addMouseListener(final MouseListener listener) {
        if (listener == null) { return; }
        synchronized (this.mouseListeners) {
            this.mouseListeners.add(listener);
        }

    }

    public void addTrayMouseListener(final TrayMouseListener listener) {
        if (listener == null) { return; }
        synchronized (this.traymouseListeners) {
            this.traymouseListeners.add(listener);
        }

    }

    public Point getEstimatedTopLeft() {
        final int midx = (this.max.x + this.min.x) / 2;
        final int midy = (this.max.y + this.min.y) / 2;

        return new Point(midx - this.size.width / 2, midy - this.size.height / 2);
    }

    /**
     * Passt die iconsize in die festgestellte geschätzte position ein. und
     * prüft ob point darin ist
     * 
     * @param point
     * @return
     */
    protected boolean isOver(final Point point) {
        final int midx = (this.max.x + this.min.x) / 2;
        final int midy = (this.max.y + this.min.y) / 2;

        final int width = Math.min(this.size.width, this.max.x - this.min.x);
        final int height = Math.min(this.size.height, this.max.y - this.min.y);

        final int minx = midx - width / 2;
        final int miny = midy - height / 2;
        final int maxx = midx + width / 2;
        final int maxy = midy + height / 2;
        // java.awt.Point[x=1274,y=1175] - java.awt.Point[x=1309,y=1185]
        if ((point.x >= minx) && (point.x <= maxx)) {
            if ((point.y >= miny) && (point.y <= maxy)) { return true; }

        }
        return false;
    }

    public void mouseClicked(final MouseEvent e) {
        synchronized (this.mouseListeners) {
            for (final MouseListener l : this.mouseListeners) {
                l.mouseClicked(e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent
     * )
     */
    
    public void mouseDragged(final MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(final MouseEvent e) {
        this.mouseover = true;
        final long enterTime = System.currentTimeMillis();
        this.mouseLocationObserver = new Thread() {
            
            public void run() {
                try {
                    boolean mouseStay = false;
                    while (true) {
                        final Point point = MouseInfo.getPointerInfo().getLocation();
                        if (!ExtTrayIcon.this.isOver(point)) {
                            MouseEvent me;
                            me = new MouseEvent(ExtTrayIcon.this.dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                            me.setSource(ExtTrayIcon.this.lastEvent.getSource());

                            synchronized (ExtTrayIcon.this.mouseListeners) {
                                for (final MouseListener l : ExtTrayIcon.this.mouseListeners) {
                                    l.mouseExited(me);
                                }
                            }
                            return;

                        } else {
                            if (((System.currentTimeMillis() - enterTime) >= ExtTrayIcon.TOOLTIP_DELAY) && !mouseStay) {
                                mouseStay = true;
                                MouseEvent me;
                                me = new MouseEvent(ExtTrayIcon.this.dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                                // me.setSource(MouseAdapter.this);
                                // deligate.mouseStay(me);

                                synchronized (ExtTrayIcon.this.traymouseListeners) {
                                    for (final TrayMouseListener l : ExtTrayIcon.this.traymouseListeners) {
                                        l.mouseMoveOverTray(me);
                                    }
                                }

                            }
                        }

                        Thread.sleep(100);
                    }
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    ExtTrayIcon.this.mouseLocationObserver = null;
                }
            }

        };
        this.mouseLocationObserver.start();

    }

    public void mouseExited(final MouseEvent e) {
        this.mouseover = false;

        this.min = this.max = null;

        synchronized (this.mouseListeners) {
            for (final MouseListener l : this.mouseListeners) {
                l.mouseExited(e);
            }
        }

    }

    public void mouseMoved(final MouseEvent e) {
        this.lastEvent = e;
        /**
         * the more the user moves over the tray, the better we know it's
         * location *
         */

        if (this.min == null) {
            this.min = new Point(e.getPoint().x, e.getPoint().y);
            this.max = new Point(e.getPoint().x, e.getPoint().y);
        } else {
            this.min.x = Math.min(e.getPoint().x, this.min.x);
            this.min.y = Math.min(e.getPoint().y, this.min.y);
            this.max.x = Math.max(e.getPoint().x, this.max.x);
            this.max.y = Math.max(e.getPoint().y, this.max.y);
            // System.out.println(min+" - "+max);
        }

        if (!this.mouseover) {

            synchronized (this.mouseListeners) {
                mouseEntered(e);
                for (final MouseListener l : this.mouseListeners) {
                    l.mouseEntered(e);
                }
            }
        } else {

            // deligate.mouseMoved(e);
        }

    }

    public void mousePressed(final MouseEvent e) {

        synchronized (this.mouseListeners) {
            for (final MouseListener l : this.mouseListeners) {
                l.mousePressed(e);
            }
        }

    }

    public void mouseReleased(final MouseEvent e) {
        synchronized (this.mouseListeners) {
            for (final MouseListener l : this.mouseListeners) {
                l.mouseReleased(e);
            }
        }
    }

    
    public void removeMouseListener(final MouseListener listener) {
        if (listener == null) { return; }
        synchronized (this.mouseListeners) {
            this.mouseListeners.remove(listener);
        }
    }

    public void removeTrayMouseListener(final TrayMouseListener listener) {
        if (listener == null) { return; }
        synchronized (this.traymouseListeners) {
            this.traymouseListeners.remove(listener);
        }
    }

}
