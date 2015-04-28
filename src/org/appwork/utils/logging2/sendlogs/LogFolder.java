package org.appwork.utils.logging2.sendlogs;

import java.io.File;

public class LogFolder {

    private final long created;
    private boolean    selected;
    private boolean    current    = false;

    private final long lastModified;

    private final File folder;

    private boolean    needsFlush = false;

    public LogFolder(final File f, final long timestamp) {
        this.created = timestamp;
        this.lastModified = f.lastModified();
        this.folder = f;
    }

    public long getCreated() {
        return this.created;
    }

    public File getFolder() {
        return this.folder;
    }

    public long getLastModified() {
        if (isCurrent() && isNeedsFlush()) {
            return System.currentTimeMillis();
        }
        return this.lastModified;
    }

    /**
     * @return the current
     */
    public boolean isCurrent() {
        return this.current;
    }

    /**
     * @return the needsFlush
     */
    public boolean isNeedsFlush() {
        return this.needsFlush;
    }

    public boolean isSelected() {
        return this.selected;
    }

    /**
     * @param needsFlush
     *            the needsFlush to set
     */
    public void setNeedsFlush(final boolean needsFlush) {
        this.needsFlush = needsFlush;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * @param b
     */
    public void setCurrent(boolean b) {
        current = b;
    }

}
