package org.appwork.app.gui;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.appwork.shutdown.ShutdownController;
import org.appwork.swing.ExtJFrame;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.trayicon.AWTrayIcon;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.LockPanel;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dimensor.RememberLastDimensor;
import org.appwork.utils.swing.locator.RememberAbsoluteLocator;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;

public abstract class BasicGui {

    public static void main(final String[] args) {
        try {
            for (int i = 2; i >= 0; i--) {
                Thread.sleep(1000);
                System.out.println(i);
            }
        } catch (final InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                final BasicGui bg = new BasicGui("Test") {

                    @Override
                    protected void layoutPanel() {

                        final ExtButton bt;
                        getFrame().add(bt = new ExtButton(new BasicAction(" button") {

                            /**
                             * 
                             */
                            private static final long serialVersionUID = -4007724735998967065L;

                            @Override
                            public void actionPerformed(final ActionEvent e) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            for (int i = 2; i >= 0; i--) {
                                                Thread.sleep(1000);
                                                System.out.println(i);
                                            }

                                            getFrame().setAlwaysOnTop(false);
                                            WindowManager.getInstance().setZState(getFrame(), FrameState.TO_FRONT_FOCUSED);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().setZState(getFrame(),
                                            // FrameState.TO_FRONT_FOCUSED);
                                            //
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // Thread.sleep(200);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame(),
                                            // FrameState.FOCUS);
                                            // Thread.sleep(200);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame(),
                                            // FrameState.FOCUS);
                                            // Thread.sleep(200);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame(),
                                            // FrameState.FOCUS);
                                            // Thread.sleep(200);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame(),
                                            // FrameState.FOCUS);
                                            // Thread.sleep(200);
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame());
                                            // WindowManager.getInstance().toFront(getFrame(),
                                            // FrameState.FOCUS);
                                        } catch (final InterruptedException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                    }

                                }.start();

                            }
                        }));

                        getFrame().addWindowFocusListener(new WindowFocusListener() {

                            @Override
                            public void windowGainedFocus(final WindowEvent windowevent) {
                                bt.setText("JIPPIE! I Got Focused");

                            }

                            @Override
                            public void windowLostFocus(final WindowEvent windowevent) {
                                bt.setText(" :( No Focus para mi");

                            }
                        });
                    }

                    @Override
                    protected void requestExit() {
                        ShutdownController.getInstance().requestShutdown();

                    }
                };

            }
        };

        try {
            Thread.sleep(1000000);
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * The Mainframe
     */
    private final JFrame            frame;

    private LockPanel               lockPanel;

    private AWTrayIcon              ti;

    private RememberAbsoluteLocator locator;

    private RememberLastDimensor    dimensor;

    protected BasicGui(final String title) {

        frame = new ExtJFrame(title) {
            /**
             * 
             */
            private static final long serialVersionUID = -8325715174242107194L;
            private final boolean     notToFront       = false;                 ;

            @Override
            public void setVisible(final boolean b) {
                // if we hide a frame which is locked by an active modal dialog,
                // we get in problems. avoid this!
                if (!b) {
                    for (final Window w : getOwnedWindows()) {
                        if (w instanceof JDialog && ((JDialog) w).isModal() && w.isActive()) {

                            Toolkit.getDefaultToolkit().beep();
                            throw new ActiveDialogException((JDialog) w);
                        }

                    }

                    locator.onClose(frame);
                    dimensor.onClose(frame);
                }

                super.setVisible(b);

            }

            @Override
            public void toFront() {
                // if (notToFront) { return; }
                super.toFront();
            }
        };

        // dilaog init

        locator = new RememberAbsoluteLocator(BasicGui.this.getClass().getName());
        dimensor = new RememberLastDimensor(BasicGui.this.getClass().getName());

        AbstractDialog.setDefaultRoot(frame);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent arg0) {

                locator.onClose(frame);
                dimensor.onClose(frame);
                if (!CrossSystem.isMac()) {
                    new Thread("Closer") {
                        @Override
                        public void run() {
                            BasicGui.this.requestExit();

                        }

                    }.start();
                } else {
                    if (BasicGui.this.getFrame().isVisible()) {

                        WindowManager.getInstance().setVisible(BasicGui.this.getFrame(), false);
                    }
                }
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon

        try {
            lockPanel = LockPanel.create(frame);
        } catch (final AWTException e1) {

            org.appwork.utils.logging.Log.exception(e1);
        }
        frame.setIconImages(getAppIconList());
        // Set Application dimensions and locations

        Dimension dim = dimensor.getDimension(frame);
        // restore size
        if (dim != null) {
            frame.setSize(dim);
            frame.setPreferredSize(dim);
        }

        frame.setMinimumSize(new Dimension(100, 100));
        //

        layoutPanel();
        // setGlasPane();

        frame.pack();
        Point loc = locator.getLocationOnScreen(frame);
        if (loc != null) {
            frame.setLocation(loc);
        }

        WindowManager.getInstance().setVisible(frame, true);
        WindowManager.getInstance().show(frame, FrameState.TO_FRONT_FOCUSED);

    }

    public void dispose() {

        if (ti != null) {
            ti.dispose();
        }
        WindowManager.getInstance().setVisible(frame, false);
        frame.dispose();
    }

    /**
     * @return
     */
    protected List<? extends Image> getAppIconList() {
        final java.util.List<Image> list = new ArrayList<Image>();

        return list;
    }

    public JFrame getFrame() {
        return frame;
    }

    /**
     * @return the {@link GUI#lockPanel}
     * @see GUI#lockPanel
     */
    protected LockPanel getLockPanel() {
        return lockPanel;
    }

    /**
     * Creates the whole mainframework panel
     * 
     * @throws IOException
     */
    protected abstract void layoutPanel();

    protected abstract void requestExit();
}
