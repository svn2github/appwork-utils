package org.appwork.utils.processes;

import java.nio.charset.Charset;

import org.appwork.utils.net.LineParsingOutputStream;

public class ProcessOutputLinereadBuffer extends LineParsingOutputStream {
    private final LineHandler sink;

    public ProcessOutputLinereadBuffer(LineHandler sink, Charset charset) {
        super(charset);
        this.sink = sink;
    }

    @Override
    protected void onNextLine(NEWLINE newLine, StringBuilder sb, int startIndex, int endIndex) {
    }
}