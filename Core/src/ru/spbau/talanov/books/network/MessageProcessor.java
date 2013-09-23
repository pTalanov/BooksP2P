package ru.spbau.talanov.books.network;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.messages.Message;
import ru.spbau.talanov.books.messages.serialization.Serializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;

public final class MessageProcessor {

    public interface MessageHandler {
        void processMessage(@NotNull Message message, @NotNull SocketChannel incomingChannel) throws IOException;
    }

    @NotNull
    private ServerSocketChannel serverSocket;
    @NotNull
    private Selector selector;
    @NotNull
    private final ScheduledThreadPoolExecutor executor;
    @NotNull
    private final Logger log;
    @NotNull
    private final InetSocketAddress serverAddress;
    //should be set to not null in initialize() method
    private MessageHandler messageHandler;

    private boolean shutdown = false;

    public MessageProcessor(@NotNull Logger log, @NotNull InetSocketAddress serverAddress, @NotNull ScheduledThreadPoolExecutor threadPoolExecutor) {
        this.executor = threadPoolExecutor;
        this.log = log;
        this.serverAddress = serverAddress;
    }

    public void initialize(@NotNull MessageHandler messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.serverSocket = ServerSocketChannel.open().bind(serverAddress);
        this.serverSocket.configureBlocking(false);

        this.selector = Selector.open();
        this.serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() {
        assert messageHandler != null : "Initialize first!";
        executor.submit(new Runnable() {
            @Override
            public void run() {
                messageProcessingLoop();
            }
        });
    }

    private void messageProcessingLoop() {
        try {
            while (!shutdown) {
                int channelsSelected;
                try {
                    channelsSelected = selector.select();
                } catch (IOException e) {
                    //NOTE: not sure if one can recover from this.
                    log.severe("Error during selection operation. " + e.getMessage());
                    continue;
                }

                if (channelsSelected == 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    processKey(key);
                }
                selectionKeys.clear();
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {

        try {
            serverSocket.close();
        } catch (IOException e) {
            log.severe("Error on shutdown " + e.getMessage());
        }
        try {
            selector.close();
        } catch (IOException e) {
            log.severe("Error on shutdown " + e.getMessage());
        }
    }

    private void processKey(@NotNull SelectionKey key) {
        if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
            acceptConnection();
        } else if ((key.readyOps() & OP_READ) == OP_READ) {
            processIncomingMessage(key);
        }
    }

    private void processIncomingMessage(@NotNull SelectionKey key) {
        try {
            final SocketChannel socketChannel = (SocketChannel) key.channel();
            final Message message = readMessage(socketChannel);
            log.info("Received message " + message.getClass().getSimpleName() + " from " + socketChannel.getRemoteAddress());
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String messageName = message.getClass().getSimpleName();
                        SocketAddress socketAddress = socketChannel.getRemoteAddress();
                        messageHandler.processMessage(message, socketChannel);
                        log.info("Proccessed " + messageName
                                + " from " + socketAddress);
                    } catch (Throwable e) {
                        log.severe("Exception in message handler for "
                                + message.getClass() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            key.cancel();
            try {
                key.channel().close();
            } catch (IOException ee) {
                log.severe("Could not close the channel: " + e.getMessage());
            }
            log.severe("Error while trying to process message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void acceptConnection() {
        try {
            SocketChannel incomingConnection = serverSocket.accept();
            incomingConnection.configureBlocking(false);
            incomingConnection.register(selector, OP_READ);
            log.info("Incoming connection from " + incomingConnection.getRemoteAddress());
        } catch (IOException e) {
            log.severe("Could not accept incoming connection:" + e.getMessage());
        }
    }

    public void registerForReading(@NotNull SocketChannel channel) throws ClosedChannelException {
        channel.register(selector, OP_READ);
    }

    public void shutdown() {
        shutdown = true;
        // could be null if exception is thrown while initilizing
        //noinspection ConstantConditions
        if (selector != null) {
            selector.wakeup();
        }
    }

    public void sendMessageWithDelay(@NotNull SocketChannel channel, @NotNull Message message, long delayInMillis) {
        executor.schedule(sendMessageRunnable(channel, message), delayInMillis, TimeUnit.MILLISECONDS);
    }

    public void sendMessage(@NotNull SocketChannel channel, @NotNull Message message) {
        executor.submit(sendMessageRunnable(channel, message));
    }

    @NotNull
    private Runnable sendMessageRunnable(@NotNull final SocketChannel channel, @NotNull final Message message) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("Sending message " + message.getClass().getSimpleName() + " to " + channel.getRemoteAddress());
                    ByteBuffer buffer = messageToBuffer(message);
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                } catch (IOException e) {
                    log.severe("Error serializing message: " + e.getMessage());
                }
            }
        };
    }

    @NotNull
    private static Message readMessage(@NotNull SocketChannel socketChannel) throws IOException {
        int messageSize = readMessageSize(socketChannel);
        ByteBuffer messageBuffer = ByteBuffer.allocate(messageSize);
        while (messageBuffer.hasRemaining()) {
            socketChannel.read(messageBuffer);
        }
        return Serializer.deserialize(messageBuffer);
    }

    private static int readMessageSize(@NotNull SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        socketChannel.read(buffer);
        buffer.flip();
        return buffer.getInt();
    }

    @NotNull
    private static ByteBuffer messageToBuffer(@NotNull Message message) throws IOException {
        byte[] responseBytes = Serializer.serialize(message);
        ByteBuffer buffer = ByteBuffer.allocate(responseBytes.length + 4);
        buffer.putInt(responseBytes.length);
        buffer.put(responseBytes);
        buffer.flip();
        return buffer;
    }

}
