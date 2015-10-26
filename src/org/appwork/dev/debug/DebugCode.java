package org.appwork.dev.debug;

import org.appwork.utils.Application;


public abstract class DebugCode<T> {
    private T ret;

    public DebugCode() {
        if (Application.isJared(DebugCode.class)) throw new Error("Debug code left!");
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new Exception("Run Debug Code. Remove me!"));
        ret = run();
    }

    /**
     * @param b
     */
    public DebugCode(T b) {
        if (Application.isJared(DebugCode.class)) throw new Error("Debug code left!");
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new Exception("Run Debug Code. Remove me!"));
        ret=b;
    }

    protected abstract T run();
    public T get() {
        return ret;
    }
}
