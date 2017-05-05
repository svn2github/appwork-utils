package org.appwork.utils.net.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.appwork.utils.IO;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.websocket.WebSocketClient.OP_CODE;

public class ReadWebSocketFrame extends WebSocketFrame {
    protected final byte[] maskedPayload;

    public byte[] getMaskedPayload() {
        return this.maskedPayload;
    }

    public ReadWebSocketFrame(WebSocketFrameHeader frameHeader, byte[] payload) {
        super(frameHeader, payload);
        this.maskedPayload = payload;
    }

    public ReadWebSocketFrame(WebSocketFrameHeader frameHeader) {
        this(frameHeader, null);
    }

    public static ReadWebSocketFrame read(InputStream is) throws IOException {
        final WebSocketFrameHeader frameHeader = WebSocketFrameHeader.read(is);
        if (frameHeader != null) {
            if (frameHeader.getPayloadLength() > 0) {
                final byte[] payLoad = IO.readStream(-1, new LimitedInputStream(is, frameHeader.getPayloadLength()) {
                    @Override
                    public void close() throws IOException {
                    }
                });
                return new ReadWebSocketFrame(frameHeader, payLoad);
            } else {
                return new ReadWebSocketFrame(frameHeader);
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReadFrame|Fin:").append(this.isFin());
        sb.append("|OpCode:").append(this.getOpcode());
        if (this.getMask() != null) {
            sb.append("|Mask:").append(HexFormatter.byteArrayToHex(this.getMask()));
        }
        sb.append("|PayLoadLength:" + this.getPayloadLength());
        if (OP_CODE.UTF8_TEXT.equals(this.getOpcode()) && this.hasPayLoad()) {
            sb.append("|UTF8_TEXT:" + new String(this.getPayload(), Charset.forName("UTF-8")));
        }
        return sb.toString();
    }
}
