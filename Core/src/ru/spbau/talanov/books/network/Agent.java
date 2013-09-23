package ru.spbau.talanov.books.network;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * An abstract agent with common code for Server and Peer.
 */
public abstract class Agent {
    @NotNull
    public final Logger log;
    @NotNull
    private final File logFile;
    @NotNull
    protected final MessageProcessor messageProcessor;
    @NotNull
    protected final ScheduledThreadPoolExecutor executor;

    public Agent(@NotNull InetSocketAddress agentServerSocketAddress, @NotNull Logger log, @NotNull File logFile) {
        this.log = log;
        this.logFile = logFile;
        this.executor = new ScheduledThreadPoolExecutor(4);
        this.messageProcessor = new MessageProcessor(log, agentServerSocketAddress, executor);
        log.info("ADDRESS: " + agentServerSocketAddress);
    }

    public void run(long secondsBeforeShutdown) {
        try {
            initialize();
            start();
            Thread.sleep(secondsBeforeShutdown * 1000);
        } catch (Throwable e) {
            log.severe(e.getMessage());
        } finally {
            shutdown();
        }
    }

    protected void start() {
        log.info("STARTING");
        messageProcessor.start();
        afterStart();
        log.info("STARTED");
    }

    protected abstract void afterStart();

    protected void initialize() throws IOException {
        log.info("INITIALIZING");
        try {
            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), 1000000, 1, true);
            log.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
            messageProcessor.initialize(messageHandler());
        } catch (IOException e) {
            log.severe("Error while initializing peer. Exiting. " + e.getMessage());
            throw e;
        }
    }

    protected abstract MessageProcessor.MessageHandler messageHandler();

    public void shutdown() {
        log.info("SHUTTING DOWN");
        messageProcessor.shutdown();
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.severe("Error on shutdown " + e.getMessage());
        } finally {
            log.info("SHUT DOWN");
        }
    }
}
