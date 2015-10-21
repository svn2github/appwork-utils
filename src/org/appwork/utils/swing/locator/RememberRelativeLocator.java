package org.appwork.utils.swing.locator;

import java.awt.Container;
import java.awt.Point;
import java.awt.Window;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.swing.dialog.LocationStorage;

public class RememberRelativeLocator extends AbstractLocator {

    private final String    id;
    private final Window    parent;
    private AbstractLocator fallbackLocator;

    /**
     * @param jFrame
     * @param string
     */
    public RememberRelativeLocator(final String id, final Window jFrame) {
        this.id = id;
        if (id == null) {
            throw new IllegalArgumentException("id ==null");
        }
        parent = jFrame;
        fallbackLocator = new CenterOfScreenLocator();
    }

    /**
     * @param frame
     * @return
     */
    private LocationStorage createConfig(final Window frame) {
        String storageID = RememberRelativeLocator.class.getSimpleName() + "-" + this.getID(frame);
        if (storageID.length() > 128) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < storageID.length(); i++) {
                char c = storageID.charAt(i);
                switch (c) {
                case 'U':
                case 'E':
                case 'I':
                case 'O':
                case 'A':
                case 'J':
                case 'u':
                case 'e':
                case 'i':
                case 'o':
                case 'a':
                case 'j':
                    continue;
                default:
                    sb.append(c);

                }
            }
            storageID = sb.toString();

        }
        if (storageID.length() > 128) {
            storageID = RememberRelativeLocator.class.getSimpleName() + "-" + Hash.getMD5(storageID);
        }

        return JsonConfig.create(Application.getResource("cfg/" + storageID), LocationStorage.class);
    }

    /**
     * @return
     */
    protected AbstractLocator getFallbackLocator() {
        return fallbackLocator;
    }

    /**
     * @param frame
     * @return
     */
    protected String getID(final Window frame) {
        return id;
    }

    @Override
    public Point getLocationOnScreen(final Window frame) {
        try {
            final LocationStorage cfg = createConfig(frame);
            if (cfg.isValid()) {
                if ("absolute".equalsIgnoreCase(cfg.getType())) {
                    return AbstractLocator.validate(new Point(cfg.getX(), cfg.getY()), frame);
                }
                // Do a "is on screen check" here
                Window parent = getParent();
                Container actualParent = frame.getParent();
                Point pLoc = null;

                if (parent != null) {
                    pLoc = parent.getLocationOnScreen();

                }
                if (pLoc == null && actualParent != null) {
                    pLoc = actualParent.getLocationOnScreen();
                }
                if (pLoc == null) {
                    return getFallbackLocator().getLocationOnScreen(frame);
                }
                return AbstractLocator.validate(new Point(cfg.getX() + pLoc.x, cfg.getY() + pLoc.y), frame);

            }
        } catch (final Throwable e) {

            // frame.getParent() might be null or invisble
            // e.printStackTrace();
        }
        return getFallbackLocator().getLocationOnScreen(frame);
    }

    /**
     * @return
     */
    protected Window getParent() {
        // TODO Auto-generated method stub
        return parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.frame.Locator#onClose(org.appwork.utils.swing .frame.frame)
     */
    @Override
    public void onClose(final Window frame) {
        try {
            if (frame.isShowing()) {
                final Point loc = frame.getLocationOnScreen();
                Window parent = getParent();
                Container actualParent = frame.getParent();
                Point pLoc = null;

                if (parent != null) {
                    pLoc = parent.getLocationOnScreen();

                }
                if (pLoc == null && actualParent != null) {
                    pLoc = actualParent.getLocationOnScreen();
                }
                if (pLoc == null) {
                    // no parent. save absolute
                    final LocationStorage cfg = createConfig(frame);
                    cfg.setValid(true);
                    cfg.setType("absolute");
                    cfg.setX(loc.x);
                    cfg.setY(loc.y);
                    cfg._getStorageHandler().write();
                } else {
                    final LocationStorage cfg = createConfig(frame);
                    cfg.setValid(true);
                    cfg.setX(loc.x - pLoc.x);
                    cfg.setY(loc.y - pLoc.y);
                    cfg._getStorageHandler().write();
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            // nothing.... frame.getParent or parent might be invisible
        }

    }

    public void setFallbackLocator(final AbstractLocator fallbackLocator) {
        this.fallbackLocator = fallbackLocator;
    }

}
