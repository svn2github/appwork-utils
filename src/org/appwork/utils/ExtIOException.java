package org.appwork.utils;

import java.io.IOException;

public class ExtIOException extends IOException {
    public static enum IOExceptionType {
        LOCAL,
        REMOTE;
    }

    private String          streamInfo;
    private String          desc;
    private IOExceptionType type;

    public ExtIOException(Exception e, IOExceptionType type) {
        super(e);
        this.type = type;
    }

    public ExtIOException(Exception e, IOExceptionType type, String message) {
        super(message, e);
        this.type = type;
    }

    public static ExtIOException getInstance(Exception e, IOExceptionType type) {
        if (e instanceof ExtIOException && ((ExtIOException) e).type == type) {
            return (ExtIOException) e;
        }

        return new ExtIOException(e, type);
    }

    public ExtIOException(Exception e, IOExceptionType type, String msg, String streamInfo) {
        super(msg + "( " + streamInfo + ")", e);
        this.type = type;
        this.desc = msg;
        this.streamInfo = streamInfo;
    }

    public ExtIOException(String msg, IOExceptionType type, String streaminfo) {
        super(msg + "( " + streaminfo + ")");
        this.type = type;
        this.desc = msg;
        this.streamInfo = streaminfo;
    }

    public String getDescription() {
        return this.desc;
    }

    public String getStreamInfo() {
        return this.streamInfo;
    }

}
