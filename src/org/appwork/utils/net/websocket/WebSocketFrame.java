package org.appwork.utils.net.websocket;

import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.websocket.WebSocketClient.OP_CODE;

public abstract class WebSocketFrame {
    private final WebSocketFrameHeader frameHeader;

    public boolean isFin() {
        return this.frameHeader.isFin();
    }

    public final OP_CODE getOpcode() {
        return this.frameHeader.getOpcode();
    }

    public final long getPayloadLength() {
        return this.frameHeader.getPayloadLength();
    }

    public final byte[] getMask() {
        return this.frameHeader.getMask();
    }

    public final byte[] getPayload() {
        return this.payload;
    }

    public final byte[] getHeader() {
        return this.getFrameHeader().getBytes();
    }

    public WebSocketFrameHeader getFrameHeader() {
        return this.frameHeader;
    }

    public boolean hasPayLoad() {
        return this.frameHeader.hasPayLoad();
    }

    private final byte[] payload;

    public WebSocketFrame(WebSocketFrameHeader frameHeader, byte[] payload) {
        this.frameHeader = frameHeader;
        if (frameHeader.getMask() != null && payload != null && payload.length > 0) {
            final byte[] mask = frameHeader.getMask();
            this.payload = new byte[payload.length];
            System.arraycopy(payload, 0, this.payload, 0, payload.length);
            for (int index = 0; index < payload.length; index++) {
                this.payload[index] ^= mask[index % 4];
            }
        } else {
            this.payload = payload;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Fin:").append(this.isFin());
        sb.append("|OpCode:").append(this.getOpcode());
        if (this.getMask() != null) {
            sb.append("|Mask:").append(HexFormatter.byteArrayToHex(this.getMask()));
        }
        sb.append("|PayLoadLength:" + this.getPayloadLength());
        return sb.toString();
    }
}