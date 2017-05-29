/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.statistics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.giulius.annotations.Defaults;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes short strings plus (short!) arrays of longs as UDP packets, in the
 * form [value-1-base36]:[value-2-base36]:stat-string Values are encoded as
 * base-36 (variable length) to reduce the number of bytes required but keep the
 * packet human-readable as ascii. It is best to keep the messages small, though
 * the theoretical max payload size for UDP is 65507 bytes.
 * <p/>
 * / | and : are reserved characters and may not be included.
 *
 * @author Tim Boudreau
 */
@Singleton
@Defaults("stats.udp.dest=224.0.0.1\nstats.udp.port=43271")
public final class UDPBroadcaster {
    private final InetAddress host;
    private final DatagramSocket socket;
    static final Charset ascii = Charset.forName("US-ASCII");
    private final int port;
    @Inject(optional = true)
    private ExecutorService exe = Executors.newFixedThreadPool(1);
    private final TransferQueue<byte[]> queue = new LinkedTransferQueue<>();

    public UDPBroadcaster(@Named("stats.udp.dest") String host, @Named("stats.udp.port") int port, ShutdownHookRegistry reg) throws UnknownHostException, SocketException {
        this.port = port;
        this.host = InetAddress.getByName(host);
        socket = new DatagramSocket();
        socket.setBroadcast(true);
        reg.add(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        });
        start();
    }

    public void shutdown() {
        try {
            if (pubThread != null) {
                pubThread.interrupt();
            }
            exe.shutdownNow();
        } finally {
            socket.close();
        }
    }

    private int port() {
        return port;
    }

    public void publish(UDPMessage msg) {
        if (!exe.isShutdown()) {
            queue.offer(msg.toByteArray());
        }
    }

    void start() {
        exe.submit(new Runnable() {
            @Override
            public void run() {
                pubThread = Thread.currentThread();
                publishLoop();
            }
        });
    }

    private Thread pubThread;
    private void publishLoop() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Thread.currentThread().setName("JMX-AOP UDP Publisher Thread");
        Thread.currentThread().setDaemon(true);
        List<byte[]> bytes = new LinkedList<>();
        ByteBuffer buffer = null;
        while (!exe.isShutdown()) {
            try {
                if (exe.isShutdown()) {
                    break;
                }
                buffer = ByteBuffer.allocate(4096);
                queue.drainTo(bytes, 3);
                buffer.rewind();
                for (byte[] b : bytes) {
                    if (b.length + buffer.position() > buffer.capacity()) {
                        publish(buffer);
                        buffer.rewind();
                    }
                    if (buffer.position() != 0) {
                        buffer.put((byte) '|');
                    }
                    buffer.put(b);
                }
                if (buffer.position() != 0) {
                    publish(buffer);
                    buffer.rewind();
                }
                if (exe.isShutdown()) {
                    break;
                }
            } catch (Exception e) {
                Logger.getLogger(UDPBroadcaster.class.getName()).log(Level.SEVERE, "Exception publishing - cap " + buffer.capacity() + " limit " + buffer.limit() + " pos " + buffer.position(), e);
            }
        }
    }

    private void publish(ByteBuffer buf) {
        if (exe.isShutdown()) {
            return;
        }
        byte[] all = new byte[buf.position()];
        buf.flip();
        buf.get(all);
        DatagramPacket packet = new DatagramPacket(all, 0, all.length, host, port());
        try {
            socket.send(packet);

        } catch (IOException ex) {
            Logger.getLogger(UDPBroadcaster.class.getName()).log(Level.SEVERE, new String(all), ex);
        }
    }
}
