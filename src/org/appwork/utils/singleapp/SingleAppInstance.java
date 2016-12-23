/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.singleapp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRunableEvent;
import org.appwork.utils.Application;
import org.appwork.utils.IO;

/**
 * @author daniel
 *
 */
public class SingleAppInstance {
    private static class ShutdownHook implements Runnable {
        private SingleAppInstance instance = null;

        public ShutdownHook(final SingleAppInstance instance) {
            this.instance = instance;
        }

        public void run() {
            if (this.instance != null) {
                this.instance.exit();
            }
        }
    }

    private final String            appID;
    private InstanceMessageListener listener      = null;
    private final File              lockFile;
    private FileLock                fileLock      = null;
    private FileChannel             lockChannel   = null;
    private volatile boolean        daemonRunning = false;
    private boolean                 alreadyUsed   = false;
    private ServerSocket            serverSocket  = null;
    private final String            singleApp     = "SingleAppInstance";
    private Thread                  daemon        = null;
    private static final int        DEFAULTPORT   = 9665;
    private final File              portFile;

    public SingleAppInstance(final String appID) {
        this(appID, new File(Application.getHome()));
    }

    public SingleAppInstance(final String appID, final File directory) {
        this.appID = appID;
        directory.mkdirs();
        this.lockFile = new File(directory, appID + ".lock");
        this.portFile = new File(directory, appID + ".port");
        ShutdownController.getInstance().addShutdownEvent(new ShutdownRunableEvent(new ShutdownHook(this)));
    }

    private synchronized void cannotStart(final String cause) throws UncheckableInstanceException {
        this.alreadyUsed = true;
        closeLock();
        throw new UncheckableInstanceException(cause);
    }

    public synchronized void exit() {
        if (this.fileLock == null) {
            return;
        } else {
            this.daemonRunning = false;
            if (this.daemon != null) {
                this.daemon.interrupt();
            }
            try {
                closeServer();
                closeLock();
            } finally {
                this.lockFile.delete();
                this.portFile.delete();
            }
        }
    }

    private synchronized void closeServer() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private synchronized void closeLock() {
        if (fileLock != null) {
            try {
                fileLock.close();
            } catch (Throwable ignore) {
            } finally {
                this.fileLock = null;
            }
        }
        if (lockChannel != null) {
            try {
                lockChannel.close();
            } catch (Throwable ignore) {
            } finally {
                this.lockChannel = null;
            }
        }
    }

    private synchronized void foundRunningInstance() throws AnotherInstanceRunningException {
        this.alreadyUsed = true;
        closeLock();
        throw new AnotherInstanceRunningException(this.appID);
    }

    private InetAddress getLocalHost() {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (final UnknownHostException e1) {
        }
        if (localhost != null) {
            return localhost;
        }
        try {
            localhost = InetAddress.getByName(null);
        } catch (final UnknownHostException e1) {
        }
        return localhost;
    }

    private String readLine(final BufferedInputStream in) {
        final ByteArrayOutputStream inbuffer = new ByteArrayOutputStream();
        if (in == null) {
            return "";
        }
        int c;
        try {
            in.mark(1);
            if (in.read() == -1) {
                return null;
            } else {
                in.reset();
            }
            while ((c = in.read()) >= 0) {
                if (c == 0 || c == 10 || c == 13) {
                    break;
                } else {
                    inbuffer.write(c);
                }
            }
            if (c == 13) {
                in.mark(1);
                if (in.read() != 10) {
                    in.reset();
                }
            }
        } catch (final Exception e) {
        }
        try {
            return inbuffer.toString("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return "";
        }
    }

    private int readPortFromPortFile() {
        if (!this.portFile.exists()) {
            return 0;
        }
        try {
            final String port = IO.readFileToTrimmedString(this.portFile);
            return Integer.parseInt(String.valueOf(port).trim());
        } catch (final Exception e) {
            return 0;
        }
    }

    public synchronized boolean sendToRunningInstance(final String[] message) {
        if (this.portFile.exists()) {
            final int port = this.readPortFromPortFile();
            Socket runninginstance = null;
            if (port != 0) {
                try {
                    runninginstance = new Socket();
                    final InetSocketAddress con = new InetSocketAddress(this.getLocalHost(), port);
                    runninginstance.connect(con, 1000);
                    runninginstance.setSoTimeout(2000);/* set Timeout */
                    final BufferedInputStream in = new BufferedInputStream(runninginstance.getInputStream());
                    final OutputStream out = runninginstance.getOutputStream();
                    final String response = this.readLine(in);
                    String ownID = createID(singleApp, appID, Application.getRoot(SingleAppInstance.class));
                    if (response == null || !response.equals(ownID)) {
                        /* invalid server response */
                        return false;
                    }
                    if (message == null || message.length == 0) {
                        this.writeLine(out, "0");
                    } else {
                        this.writeLine(out, message.length + "");
                        for (final String msg : message) {
                            this.writeLine(out, msg);
                        }
                    }
                } catch (final IOException e) {
                    return false;
                } finally {
                    if (runninginstance != null) {
                        try {
                            runninginstance.shutdownInput();
                        } catch (final Throwable e) {
                        }
                        try {
                            runninginstance.shutdownOutput();
                        } catch (final Throwable e) {
                        }
                        try {
                            runninginstance.close();
                        } catch (final Throwable e) {
                        }
                        runninginstance = null;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @param singleApp2
     * @param appID2
     * @param root
     * @return
     */
    protected String createID(String singleApp, String appID, String root) {
        return singleApp + "." + appID + "." + root;
    }

    public synchronized void setInstanceMessageListener(final InstanceMessageListener listener) {
        this.listener = listener;
    }

    public synchronized void start() throws AnotherInstanceRunningException, UncheckableInstanceException {
        if (this.fileLock != null) {
            return;
        }
        if (this.alreadyUsed) {
            this.cannotStart("create new instance!");
        }
        try {
            if (this.sendToRunningInstance(null)) {
                this.foundRunningInstance();
            }
            this.lockChannel = new RandomAccessFile(this.lockFile, "rw").getChannel();
            try {
                this.fileLock = this.lockChannel.tryLock();
                if (this.fileLock == null) {
                    this.foundRunningInstance();
                }
            } catch (final OverlappingFileLockException e) {
                this.foundRunningInstance();
            } catch (final IOException e) {
                this.foundRunningInstance();
            }
            this.portFile.delete();
            this.serverSocket = new ServerSocket();
            SocketAddress socketAddress = null;
            try {
                socketAddress = new InetSocketAddress(this.getLocalHost(), SingleAppInstance.DEFAULTPORT);
                this.serverSocket.bind(socketAddress);
            } catch (final IOException e) {
                e.printStackTrace();
                try {
                    this.serverSocket.close();
                } catch (final Throwable e2) {
                }
                this.serverSocket = new ServerSocket();
                socketAddress = new InetSocketAddress(this.getLocalHost(), 0);
                this.serverSocket.bind(socketAddress);
            }
            FileOutputStream portWriter = null;
            try {
                portWriter = new FileOutputStream(this.portFile);
                portWriter.write((this.serverSocket.getLocalPort() + "").getBytes());
                portWriter.flush();
                this.startDaemon();
                return;
            } catch (final Throwable t) {
                /* network communication not possible */
            } finally {
                if (portWriter != null) {
                    try {
                        portWriter.close();
                    } catch (final Throwable t) {
                    }
                }
            }
            this.cannotStart("could not create instance!");
        } catch (final FileNotFoundException e) {
            this.cannotStart(e.getMessage());
        } catch (final IOException e) {
            try {
                this.serverSocket.close();
            } catch (final Throwable t) {
            }
            this.cannotStart(e.getMessage());
        }
    }

    private synchronized void startDaemon() {
        if (this.daemon != null) {
            return;
        }
        this.daemon = new Thread(new Runnable() {
            public void run() {
                SingleAppInstance.this.daemonRunning = true;
                while (SingleAppInstance.this.daemonRunning) {
                    if (SingleAppInstance.this.daemon.isInterrupted()) {
                        break;
                    }
                    Socket client = null;
                    try {
                        /* accept new request */
                        client = SingleAppInstance.this.serverSocket.accept();
                        client.setSoTimeout(10000);/* set Timeout */
                        final BufferedInputStream in = new BufferedInputStream(client.getInputStream());
                        final OutputStream out = client.getOutputStream();
                        SingleAppInstance.this.writeLine(out, createID(SingleAppInstance.this.singleApp, appID, Application.getRoot(SingleAppInstance.class)));
                        final String line = SingleAppInstance.this.readLine(in);
                        if (line != null && line.length() > 0) {
                            final int lines = Integer.parseInt(line);
                            if (lines != 0) {
                                final String[] message = new String[lines];
                                for (int index = 0; index < lines; index++) {
                                    message[index] = SingleAppInstance.this.readLine(in);
                                }
                                if (SingleAppInstance.this.listener != null) {
                                    try {
                                        SingleAppInstance.this.listener.parseMessage(message);
                                    } catch (final Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (final IOException e) {
                        if (daemonRunning) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                    } finally {
                        if (client != null) {
                            try {
                                client.shutdownInput();
                            } catch (final Throwable e) {
                            }
                            try {
                                client.shutdownOutput();
                            } catch (final Throwable e) {
                            }
                            try {
                                client.close();
                            } catch (final Throwable e) {
                            }
                            client = null;
                        }
                    }
                }
                try {
                    SingleAppInstance.this.serverSocket.close();
                } catch (final Throwable e) {
                    if (daemonRunning) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
            }
        });
        this.daemon.setName("SingleAppInstance: " + this.appID);
        /* set daemonmode so java does not wait for this thread */
        this.daemon.setDaemon(true);
        this.daemon.start();
    }

    private void writeLine(final OutputStream outputStream, final String line) {
        if (outputStream == null || line == null) {
            return;
        }
        try {
            outputStream.write(line.getBytes("UTF-8"));
            outputStream.write("\r\n".getBytes());
            outputStream.flush();
        } catch (final Exception e) {
        }
    }
}
