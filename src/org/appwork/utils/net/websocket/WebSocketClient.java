package org.appwork.utils.net.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WebSocketClient {
    // https://tools.ietf.org/html/rfc6455
    // http://www.websocket.org/echo.html
    // https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
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

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected static byte[] fill(final InputStream is, final byte[] buffer) throws IOException {
        final int length = buffer.length;
        int done = 0;
        int read = 0;
        while (done < length && (read = is.read(buffer, done, length - done)) != -1) {
            done += read;
        }
        if (done != length) {
            throw new EOFException(done + "!=" + length);
        }
        return buffer;
    }

    protected byte[] nextMask() {
        final byte[] ret = new byte[4];
        new Random().nextBytes(ret);
        return ret;
    }

    /**
     * //https://tools.ietf.org/html/rfc6455#section-5.5.1
     *
     * @return
     */
    public WriteWebSocketFrame buildCloseFrame() {
        return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.CLOSE, 0, this.nextMask()));
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.5.2
     */
    public WriteWebSocketFrame buildPingFrame() {
        return this.buildPingFrame(null);
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.5.2
     */
    public WriteWebSocketFrame buildPingFrame(byte[] payLoad) {
        if (payLoad != null && payLoad.length > 0) {
            if (payLoad.length > 125) {
                throw new IllegalArgumentException("Payload length must be <=125!");
            }
            return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.PING, payLoad.length, this.nextMask()), payLoad);
        } else {
            return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.PING, 0, null));
        }
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.6
     *
     * @param text
     * @return
     */
    public WriteWebSocketFrame buildUTF8TextFrame(final String text) {
        final byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
        return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.UTF8_TEXT, bytes.length, this.nextMask()), bytes);
    }

    public void writeFrame(WriteWebSocketFrame webSocketFrame) throws IOException {
        this.log(webSocketFrame);
        final OutputStream os = this.getOutputStream();
        os.write(webSocketFrame.getHeader());
        if (webSocketFrame.hasPayLoad()) {
            os.write(webSocketFrame.getPayload());
        }
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.5.1
     *
     * @param ping
     * @return
     */
    protected void onOpCode_Close(ReadWebSocketFrame close) throws IOException {
    }

    protected void onOpCode_Pong(ReadWebSocketFrame pong) throws IOException {
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.5.3
     *
     * @param ping
     * @return
     */
    public WriteWebSocketFrame buildPongFrame(ReadWebSocketFrame ping) {
        if (OP_CODE.PING.equals(ping.getOpcode())) {
            if (ping.hasPayLoad()) {
                return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.PONG, ping.getPayloadLength(), ping.getMask()), ping.getPayload());
            } else {
                return new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.PONG, 0), null);
            }
        } else {
            throw new IllegalArgumentException("Parameter must be valid PING!");
        }
    }

    /**
     * https://tools.ietf.org/html/rfc6455#section-5.5.2
     *
     * @param ping
     * @return
     */
    protected void onOpCode_Ping(ReadWebSocketFrame ping) throws IOException {
    }

    protected abstract void log(WebSocketFrame webSocketFrame);

    public ReadWebSocketFrame readNextFrame() throws IOException {
        final ReadWebSocketFrame webSocketFrame = ReadWebSocketFrame.read(this.getInputStream());
        if (webSocketFrame != null) {
            this.log(webSocketFrame);
            switch (webSocketFrame.getOpcode()) {
            case PING:
                this.onOpCode_Ping(webSocketFrame);
                break;
            case PONG:
                this.onOpCode_Pong(webSocketFrame);
                break;
            case CLOSE:
                this.onOpCode_Close(webSocketFrame);
                break;
            default:
                break;
            }
            return webSocketFrame;
        } else {
            return null;
        }
    }

    protected abstract InputStream getInputStream() throws IOException;

    protected abstract OutputStream getOutputStream() throws IOException;
}
