package org.servantframework.web.server;

import org.servantframework.web.HttpRequest;
import org.servantframework.web.HttpResponse;
import org.servantframework.web.codec.HttpDecoder;
import org.servantframework.web.codec.HttpEncoder;
import org.servantframework.web.router.HttpRouter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

public class WebServer {
    private final HttpRouter router;
    private final int port;
    private final CountDownLatch latch;
    private Selector selector;

    public WebServer(HttpRouter router, int port) {
        this.router = router;
        this.port = port;
        this.latch = new CountDownLatch(1);
    }

    public void start() {
        new Thread(() -> {
            try {
                // 创建一个Selector
                selector = Selector.open();

                // 打开一个ServerSocketChannel
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(port));
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("Server started on port " + port + "...");

                while (true) {
                    // 阻塞等待事件
                    selector.select();

                    // 获取所有触发事件的键
                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        if (key.isAcceptable()) {
                            handleAccept(key, selector);
                        } else if (key.isReadable()) {
                            handleRead(key, selector);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                router.shutdown();
                latch.countDown(); // 在服务器停止时释放主线程
            }
        }).start();
    }

    public void awaitTermination() {
        try {
            latch.await(); // 阻塞主线程，直到服务器停止
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped.");
        latch.countDown(); // 在服务器停止时释放主线程
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setKeepAlive(true);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + socketChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            socketChannel.close();
            return;
        }

        buffer.flip();
        String requestString = StandardCharsets.UTF_8.decode(buffer).toString();
        System.out.println("Received request: " + requestString);

        // 解码请求
        HttpRequest httpRequest = HttpDecoder.decode(requestString);
        HttpResponse httpResponse = new HttpResponse("HTTP/1.1", 200, "OK");

        // 路由请求
        router.handle(httpRequest, httpResponse, selector, key);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        if (buffer != null) {
            int write = socketChannel.write(buffer);
            System.out.println("already write : " + write + " byte!");

            if (buffer.hasRemaining()) {
                // 如果没有将所有数据写入完成，重新注册写事件
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            } else {
                key.interestOps(SelectionKey.OP_READ);
                // 清除附件，或者根据需要设置下一个要写的数据
                key.attach(null);
            }
        }
    }
}
