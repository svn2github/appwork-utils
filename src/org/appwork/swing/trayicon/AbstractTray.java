package org.appwork.swing.trayicon;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.appwork.resources.IconRef;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.action.BasicAction;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;

public abstract class AbstractTray implements MouseListener, MouseMotionListener {
    private static final int                            POPUP_INSETS = 5;
    protected TrayIcon                                  trayIcon;
    private org.appwork.swing.trayicon.TrayMouseAdapter ma;
    private TrayIconPopup                               jpopup;
    protected BasicAction[]                             actions;
    private DelayedRunnable                             doubleclickDelayer;
    private Runnable                                    runable;

    public AbstractTray(BasicAction... basicActions) {
        this.actions = basicActions;
    }

    public void closePopup() {
        if (this.jpopup != null) {
            this.jpopup.setVisible(false);
            this.jpopup.dispose();
            this.jpopup = null;
        }
    }

    public void run() throws AWTException {
        this.runTray();
    }

    public void setToolTip(String tt) {
        trayIcon.setToolTip(tt);
    }

    private static final ScheduledExecutorService EXECUTER = Executors.newSingleThreadScheduledExecutor();

    private void runTray() throws AWTException {
        SystemTray systemTray = SystemTray.getSystemTray();
        Image img = this.createTrayImage(TrayIconRef.trayicon);
        this.trayIcon = new TrayIcon(img, null, null);
        this.trayIcon.setImageAutoSize(true);
        this.ma = new TrayMouseAdapter(this, this.trayIcon);
        this.trayIcon.addMouseListener(this.ma);
        this.trayIcon.addMouseMotionListener(this.ma);
        this.doubleclickDelayer = new DelayedRunnable(EXECUTER, 150) {
            @Override
            public void delayedrun() {
                if (AbstractTray.this.runable != null) {
                    AbstractTray.this.runable.run();
                }
            }
        };
        systemTray.add(this.trayIcon);
    }

    /**
     * @param systemTray
     * @return
     */
    protected Image createTrayImage(IconRef id) {
        SystemTray systemTray = SystemTray.getSystemTray();
        Image img = null;
        if (systemTray.getTrayIconSize().getWidth() == 16) {
            img = getIcon(id);
        } else {
            img = IconIO.getScaledInstance(getImage(id), (int) systemTray.getTrayIconSize().getWidth(), (int) systemTray.getTrayIconSize().getHeight());
        }
        return img;
    }

    protected Image getImage(IconRef id) {
        return id.image(-1);
    }

    protected Image getIcon(IconRef id) {
        return id.image(-1);
    }

    public void showAbout() {
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        if (!SwingUtilities.isEventDispatchThread()) {
            new Exception().printStackTrace();
        }
        if (CrossSystem.isContextMenuTrigger(e)) {
            onContextClick(e);
        } else if (e.getClickCount() == 1) {
            this.runable = new Runnable() {
                @Override
                public void run() {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            onSingleClick(e);
                        }
                    };
                }
            };
            this.doubleclickDelayer.resetAndStart();
        } else if (e.getClickCount() != 1) {
            this.doubleclickDelayer.stop();
            this.runable = null;
            onDoubleClick(e);
        }
    }

    /**
     * @param e
     */
    protected void onContextClick(MouseEvent e) {
        showMenu(e);
    }

    /**
     * @param e
     */
    protected void onDoubleClick(MouseEvent e) {
        this.showAbout();
    }

    protected void onSingleClick(MouseEvent e) {
        showMenu(e);
    }

    public void showMenu(MouseEvent e) {
        if (AbstractTray.this.jpopup != null && AbstractTray.this.jpopup.isShowing()) {
            AbstractTray.this.jpopup.dispose();
            AbstractTray.this.jpopup = null;
        } else {
            AbstractTray.this.jpopup = AbstractTray.this.createMenu(e);
            Dimension ps = AbstractTray.this.jpopup.getPreferredSize();
            // SwingUtils.getUsableScreenBounds(SwingUtils.getScreenByLocation(e.getX(), e.getY()));
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice[] screens = ge.getScreenDevices();
            Point position = new Point();
            for (final GraphicsDevice screen : screens) {
                final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                if (bounds.contains(e.getPoint())) {
                    final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
                    if (e.getPoint().getX() - bounds.getX() > (bounds.getX() + bounds.getWidth() - e.getPoint().getX())) {
                        // right
                        position.x = bounds.x + bounds.width - ps.width - POPUP_INSETS - insets.right;
                    } else {
                        // left
                        position.x = bounds.x + POPUP_INSETS + insets.left;
                    }
                    if (e.getPoint().getY() - bounds.getY() > (bounds.getY() + bounds.getHeight() - e.getPoint().getY())) {
                        // bottom
                        position.y = bounds.y + bounds.height - ps.height - POPUP_INSETS - insets.bottom;
                    } else {
                        // top
                        position.y = bounds.y + POPUP_INSETS + insets.top;
                    }
                }
            }
            if (position != null) {
                AbstractTray.this.jpopup.show(position.x, position.y);
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    protected TrayIconPopup createMenu(MouseEvent e) {
        TrayIconPopup jpopup = createPopup(e);
        MenuHeaderWrapper header;
        jpopup.add(header = new MenuHeaderWrapper(createMenuHeader(e)));
        header.setOpaque(false);
        header.setBackground(null);
        createMenuNormal(e, jpopup);
        createMenuDebug(e, jpopup);
        return jpopup;
    }

    protected void createMenuDebug(MouseEvent e, final TrayIconPopup jpopup) {
        if (e.isControlDown() && e.isShiftDown()) {
            Component spacer = Box.createGlue();
            spacer.setPreferredSize(new Dimension(10, 5));
            jpopup.add(spacer);
            jpopup.add(new JSeparator());
            jpopup.add(new JSeparator());
            MenuHeader header = createMenuHeader(e);
            header.getLabel().setText(_AWU.T.debug_menu());
            // header.getIcon().setIcon(null);
            jpopup.add(new MenuHeaderWrapper(header));
            jpopup.add(new JSeparator());
            for (BasicAction a : this.actions) {
                if (!Boolean.TRUE.equals(a.getValue("debug"))) {
                    continue;
                }
                addMenuEntry(jpopup, e, a);
            }
        }
    }

    protected void addMenuEntry(final TrayIconPopup jpopup, MouseEvent e, BasicAction a) {
        JMenuItem m = createMenuItem(a);
        jpopup.add(m);
    }

    protected void createMenuNormal(MouseEvent e, TrayIconPopup jpopup) {
        for (BasicAction a : this.actions) {
            if (a == null) {
                jpopup.add(new JSeparator());
                continue;
            }
            if (Boolean.TRUE.equals(a.getValue("debug"))) {
                continue;
            }
            addMenuEntry(jpopup, e, a);
        }
    }

    protected JMenuItem createMenuItem(BasicAction a) {
        if (a.isToggle()) {
            JMenuItem m = new JCheckBoxMenuItem(a);
            m.setPreferredSize(new Dimension(m.getPreferredSize().width, 24));
            return m;
        } else {
            JMenuItem m = new JMenuItem(a);
            m.setPreferredSize(new Dimension(m.getPreferredSize().width, 24));
            return m;
        }
    }

    protected TrayIconPopup createPopup(MouseEvent e) {
        return new TrayIconPopup();
    }

    /**
     * @param e
     * @return
     */
    public abstract MenuHeader createMenuHeader(MouseEvent e);

    @Override
    public void mouseReleased(MouseEvent e) {
        // if (e.isPopupTrigger()) {
        // Dimension ps = jpopup.getPreferredSize();
        //
        // jpopup.setLocation(e.getX(), e.getY()-(int)ps.getHeight());
        // jpopup.setInvoker(jpopup);
        // jpopup.setVisible(true);
        // }
    }

    public void mouseStay(MouseEvent me) {
    }

    public void setName(final String trayTitle) {
        AbstractTray.this.trayIcon.setToolTip(trayTitle);
    }

    /**
     * @param string
     * @param msg
     */
    public void showMessage(String title, String msg) {
        trayIcon.displayMessage(title, msg, MessageType.WARNING);
    }
}
