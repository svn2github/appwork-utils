package org.appwork.utils.net.websocket;

import java.io.IOException;
import java.io.InputStream;

import org.appwork.utils.formatter.HexFormatter;

public class WebSocketFrameHeader {
    public static enum OP_CODE {
        CONTINUATION(0x0),
        UTF8_TEXT(0x1),
        BINARY(0x2),
        CLOSE(0x8),
        PING(0x9),
        PONG(0xA);
        private final int opCode;

        final int getOpCode() {
            return this.opCode;
        }

        private OP_CODE(int opCode) {
            this.opCode = opCode;
        }

        static OP_CODE get(int opCode) {
            for (final OP_CODE value : OP_CODE.values()) {
                if (value.getOpCode() == opCode) {
                    return value;
                }
            }
            return null;
        }
    }

    private final boolean fin;

    public boolean isFin() {
        return this.fin;
    }

    public OP_CODE getOpcode() {
        return this.opCode;
    }

    public boolean hasPayLoad() {
        return this.getPayloadLength() > 0;
    }

    public long getPayloadLength() {
        return this.payloadLength;
    }

    public byte[] getMask() {
        return this.mask;
    }

    private final OP_CODE opCode;
    private final long    payloadLength;
    private final byte[]  mask;

    public static WebSocketFrameHeader read(final InputStream is) throws IOException {
        byte[] buf = WebSocketEndPoint.fill(is, new byte[2]);
        final boolean fin = 1 == (buf[0] & 0xff) >>> 7;// fin, finrsv1rsv2rsv3xxxx 7 rightshift
        final int opCode = buf[0] & 15;// opCode, xxxx1111
        final boolean mask = 1 == (buf[1] & 0xff) >>> 7;// mask, fxxxxxxx 7 rightshift
        long payloadLength = buf[1] & 127; // length, x1111111
        if (payloadLength == 126) {
            buf = WebSocketEndPoint.fill(is, new byte[2]);// 16 bit unsigned
            payloadLength = ((buf[0] & 255) << 8) + ((buf[1] & 255) << 0);
        } else if (payloadLength == 127) {
            buf = WebSocketEndPoint.fill(is, new byte[8]);// 64 bit unsigned
            payloadLength = ((long) buf[0] << 56) + ((long) (buf[1] & 255) << 48) + ((long) (buf[2] & 255) << 40) + ((long) (buf[3] & 255) << 32) + ((long) (buf[4] & 255) << 24) + ((buf[5] & 255) << 16) + ((buf[6] & 255) << 8) + ((buf[7] & 255) << 0);
        }
        final OP_CODE op_Code = OP_CODE.get(opCode);
        if (op_Code == null) {
            //
            throw new IOException("Unsupported opCode:" + opCode);
        }
        if (mask) {
            return new WebSocketFrameHeader(fin, op_Code, payloadLength, WebSocketEndPoint.fill(is, new byte[4]));
        } else {
            return new WebSocketFrameHeader(fin, op_Code, payloadLength);
        }
    }

    public byte[] getBytes() {
        int length = 1;// fin and opCode
        if (this.payloadLength <= 125) {
            length += 1;// mask|length;
        } else if (this.payloadLength > 125 && this.payloadLength <= ((1 << 16) - 1)) {
            length += 3;// mask|length + 2 bytes, 16 bit unsigned
        } else {
            length += 9;// mask|length + 8 bytes,64 bit unsigned
        }
        if (this.mask != null) {
            length += 4;// mask, 4 bytes
        }
        int writeIndex = 0;
        final byte[] ret = new byte[length];
        ret[writeIndex++] = (byte) ((this.isFin() ? 1 << 7 : 0) + (this.getOpcode().getOpCode() & 0xFF));
        if (this.payloadLength <= 125) {
            ret[writeIndex++] = (byte) ((this.mask != null ? 1 << 7 : 0) + (Math.max(0, this.payloadLength) & 0xFF));
        } else if (this.payloadLength > 125 && this.payloadLength <= ((1 << 16) - 1)) {
            ret[writeIndex++] = (byte) ((this.mask != null ? 1 << 7 : 0) + 126);
            ret[writeIndex++] = (byte) (this.payloadLength >>> 8 & 0xFF);
            ret[writeIndex++] = (byte) (this.payloadLength >>> 0 & 0xFF);
        } else {
            ret[writeIndex++] = (byte) ((this.mask != null ? 1 << 7 : 0) + 127);
            for (int shift = 56; shift >= 0; shift -= 8) {
                ret[writeIndex++] = (byte) (this.payloadLength >>> shift & 0xFF);
            }
        }
        if (this.mask != null) {
            ret[writeIndex++] = this.mask[0];
            ret[writeIndex++] = this.mask[1];
            ret[writeIndex++] = this.mask[2];
            ret[writeIndex++] = this.mask[3];
        }
        return ret;
    }

    public WebSocketFrameHeader(boolean fin, OP_CODE opCode, long payloadLength, byte[] mask) {
        this.fin = fin;
        this.opCode = opCode;
        this.payloadLength = payloadLength;
        this.mask = mask;
        if (mask != null && mask.length != 4) {
            //
            throw new IllegalArgumentException("mask length must be 4 bytes!");
        }
    }

    public WebSocketFrameHeader(boolean fin, OP_CODE opCode, long payloadLength) {
        this(fin, opCode, payloadLength, null);
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