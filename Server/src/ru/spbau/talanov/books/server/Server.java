package ru.spbau.talanov.books.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.BookId;
import ru.spbau.talanov.books.data.id.BookInfo;
import ru.spbau.talanov.books.data.id.ChapterId;
import ru.spbau.talanov.books.messages.*;
import ru.spbau.talanov.books.network.Agent;
import ru.spbau.talanov.books.network.MessageProcessor;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;


public final class Server extends Agent {

    @NotNull
    private final BookIndex bookIndex;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> arguments = Arrays.asList(args);
        if (arguments.size() != 3) {
            System.out.println("Usage: <port> <seconds to run> <log file>");
            return;
        }
        try {
            int port = Integer.parseInt(arguments.get(0));
            int secondsBeforeShutdown = Integer.parseInt(arguments.get(1));
            File logFile = new File(arguments.get(2));
            Server server = new Server(new InetSocketAddress("localhost", port), logFile);
            server.run(secondsBeforeShutdown);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }

    private Server(@NotNull InetSocketAddress serverAddress, @NotNull File logFile) {
        super(serverAddress, Logger.getLogger(Server.class.getSimpleName()), logFile);
        this.bookIndex = new BookIndex();
    }

    @Override
    protected void afterStart() {
        //do nothing
    }

    @Override
    protected MessageProcessor.MessageHandler messageHandler() {
        return new MessageProcessor.MessageHandler() {
            @Override
            public void processMessage(@NotNull Message message, @NotNull SocketChannel incomingChannel) throws IOException {
                if (message instanceof RegisterBookRequest) {
                    processRegisterBookRequest((RegisterBookRequest) message);
                } else {
                    if (message instanceof BookInfoRequest) {
                        processBookInfoRequest((BookInfoRequest) message, incomingChannel);
                    } else if (message instanceof ChaptersAcquiredMessage) {
                        processChapterAcquiredMessage((ChaptersAcquiredMessage) message);
                    } else if (message instanceof ChapterInfoRequest) {
                        processChapterInfoRequest((ChapterInfoRequest) message, incomingChannel);
                    } else {
                        log.severe("Message of unexpected type " + message.getClass());
                    }
                }
            }
        };
    }

    private void processChapterInfoRequest(@NotNull ChapterInfoRequest chapterInfoRequest,
                                           @NotNull SocketChannel incomingChannel) {
        ChapterId requestedId = chapterInfoRequest.getId();
        Collection<InetSocketAddress> owners = bookIndex.getChapterOwners(requestedId);
        ChapterInfoResponse response = new ChapterInfoResponse(requestedId, owners);
        messageProcessor.sendMessage(incomingChannel, response);
    }

    private void processChapterAcquiredMessage(@NotNull ChaptersAcquiredMessage chaptersAcquiredMessage) {
        Collection<ChapterId> chapters = chaptersAcquiredMessage.getChapters();
        for (ChapterId chapter : chapters) {
            bookIndex.registerChapterOwner(chapter, chaptersAcquiredMessage.getSenderAddress());
        }
    }

    private void processBookInfoRequest(@NotNull BookInfoRequest bookInfoRequest,
                                        @NotNull SocketChannel incomingChannel) {
        BookId requestedId = bookInfoRequest.getBookId();
        BookInfo info = bookIndex.getInfo(requestedId);
        BookInfoResponse response = new BookInfoResponse(info, requestedId);
        messageProcessor.sendMessage(incomingChannel, response);
    }

    private void processRegisterBookRequest(@NotNull RegisterBookRequest registerBookRequest) {
        bookIndex.addBook(registerBookRequest.getBookInfo());
    }
}
