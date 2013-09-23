package ru.spbau.talanov.books.peer;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.Book;
import ru.spbau.talanov.books.data.BookCatalogue;
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
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/*
 * Peer is an agent capable of transfering data to other peers.
 */
public final class Peer extends Agent {

    private static final int CHAPTERS_REQUESTED_AT_A_TIME = 50;
    @NotNull
    private final List<BookId> booksInterestedIn;
    @NotNull
    private List<BookInfo> booksToShare;
    @NotNull
    private final InetSocketAddress peerAddress;
    @NotNull
    private final File catalogueFolder;
    @NotNull
    private BookCatalogue bookCatalogue;
    @NotNull
    private SocketChannel serverChannel;
    @NotNull
    private final Set<ChapterId> chaptersInterestedIn = new CopyOnWriteArraySet<>();
    @NotNull
    private final Set<ChapterId> chaptersAcquired = new CopyOnWriteArraySet<>();
    @NotNull
    private final InetSocketAddress serverAddress;


    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> arguments = Arrays.asList(args);
        if (arguments.size() < 5) {
            System.out.println("Usage: <folder> <peer port> <server port> <seconds to run> <log file> [list of books to download]");
            return;
        }
        try {
            InetSocketAddress peerAddress = new InetSocketAddress("localhost", Integer.parseInt(arguments.get(1)));
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", Integer.parseInt(arguments.get(2)));
            File logFile = new File(arguments.get(4));
            File catalogueFolder = new File(arguments.get(0));
            Peer peer = new Peer(catalogueFolder, peerAddress, serverAddress, logFile, getBooksInterestedIn(arguments));
            peer.run(Integer.parseInt(arguments.get(3)));
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }

    @NotNull
    private static List<BookId> getBooksInterestedIn(@NotNull List<String> arguments) {
        List<String> namesOfBooksToDownload = arguments.subList(5, arguments.size());
        List<BookId> result = new ArrayList<>();
        for (String bookName : namesOfBooksToDownload) {
            result.add(new BookId(bookName));
        }
        return result;
    }


    public Peer(@NotNull File catalogueFolder,
                @NotNull InetSocketAddress peerAddress,
                @NotNull InetSocketAddress serverAddress,
                @NotNull File logFile,
                @NotNull List<BookId> booksInterestedIn
    ) {
        super(peerAddress, Logger.getLogger(Peer.class.getSimpleName()), logFile);
        this.booksInterestedIn = booksInterestedIn;
        this.peerAddress = peerAddress;
        this.catalogueFolder = catalogueFolder;
        this.serverAddress = serverAddress;
    }

    @Override
    protected void initialize() throws IOException {
        this.bookCatalogue = BookCatalogue.readCatalogue(catalogueFolder);
        this.booksToShare = bookCatalogue.getPreExistingBooks();
        this.chaptersAcquired.addAll(bookCatalogue.getAllExistingChapterIds());
        super.initialize();
        try {
            serverChannel = openConnectionToMainServer();
            messageProcessor.registerForReading(serverChannel);
        } catch (IOException e) {
            log.severe("Could not establish connection to main server at " + serverAddress + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        log.info("Initialiazing peer at " + peerAddress + " interested in books " + booksInterestedIn);
    }

    private void processChapterInfoResponse(@NotNull SocketChannel serverChannel,
                                            @NotNull ChapterInfoResponse chapterInfoResponse) throws IOException {
        Collection<InetSocketAddress> peers = chapterInfoResponse.getPeers();
        if (peers.isEmpty()) {
            ChapterInfoRequest repeatedRequest = new ChapterInfoRequest(chapterInfoResponse.getId());
            messageProcessor.sendMessageWithDelay(serverChannel, repeatedRequest, 250);
            return;
        }
        InetSocketAddress peer = selectRandomPeerUsingSophisticatedAlgorithm(peers);
        SocketChannel peerChannel = SocketChannel.open(peer);
        ChapterDataRequest chapterDataRequest = new ChapterDataRequest(chapterInfoResponse.getId(), peerAddress);
        messageProcessor.sendMessage(peerChannel, chapterDataRequest);
    }

    private void processBookInfoResponse(@NotNull SocketChannel serverChannel,
                                         @NotNull BookInfoResponse bookInfoResponse) {
        BookInfo bookInfo = bookInfoResponse.getInfo();
        if (bookInfo == null) {
            BookInfoRequest repeatedRequest = new BookInfoRequest(bookInfoResponse.getId());
            messageProcessor.sendMessageWithDelay(serverChannel, repeatedRequest, 1000);
            return;
        }
        Book book = bookCatalogue.createBook(bookInfo);
        if (book == null) {
            log.severe("Could not create book " + bookInfo);
            return;
        }
        chaptersInterestedIn.addAll(getAllAbsentChapters(bookInfo));
    }

    @NotNull
    private Collection<ChapterId> getAllAbsentChapters(@NotNull BookInfo bookInfo) {
        List<ChapterId> allChaptersInBook = bookInfo.getAllChapterIds();
        Collection<ChapterId> existingChapters = bookCatalogue.getAllExistingChapterIds();
        allChaptersInBook.removeAll(existingChapters);
        return allChaptersInBook;
    }

    @NotNull
    private final static Random RANDOM = new Random();

    @NotNull
    private InetSocketAddress selectRandomPeerUsingSophisticatedAlgorithm(@NotNull Collection<InetSocketAddress> peers) {
        List<InetSocketAddress> asList = new ArrayList<>(peers);
        return asList.get(RANDOM.nextInt(asList.size()));
    }

    @Override
    protected void afterStart() {
        for (BookInfo bookToShare : booksToShare) {
            messageProcessor.sendMessage(serverChannel, new RegisterBookRequest(bookToShare));
        }

        for (BookId bookInterestedIn : booksInterestedIn) {
            messageProcessor.sendMessage(serverChannel, new BookInfoRequest(bookInterestedIn));
        }
        scheduleChapterRequestingService();
        scheduleChapterAcquiredService();
    }

    private void scheduleChapterAcquiredService() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Set<ChapterId> processedChapters = new HashSet<>(chaptersAcquired);
                if (processedChapters.isEmpty()) {
                    return;
                }
                ChaptersAcquiredMessage chaptersAcquiredMessage = new ChaptersAcquiredMessage(peerAddress,
                        processedChapters);
                chaptersAcquired.removeAll(processedChapters);
                messageProcessor.sendMessage(serverChannel, chaptersAcquiredMessage);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void scheduleChapterRequestingService() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                List<ChapterId> chaptersToRequest = new ArrayList<>(chaptersInterestedIn);
                //only request a limited number of chapters
                chaptersToRequest = chaptersToRequest.subList(0, Math.min(CHAPTERS_REQUESTED_AT_A_TIME, chaptersToRequest.size()));
                for (ChapterId chapterId : chaptersToRequest) {
                    messageProcessor.sendMessage(serverChannel, new ChapterInfoRequest(chapterId));
                    chaptersInterestedIn.remove(chapterId);
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }


    @NotNull
    private SocketChannel openConnectionToMainServer() throws IOException {
        SocketChannel serverChannel = SocketChannel.open(serverAddress);
        serverChannel.configureBlocking(false);
        return serverChannel;
    }

    @Override
    protected MessageProcessor.MessageHandler messageHandler() {
        return new MessageProcessor.MessageHandler() {
            @Override
            public void processMessage(@NotNull Message message, @NotNull SocketChannel incomingChannel) throws IOException {
                if (message instanceof BookInfoResponse) {
                    processBookInfoResponse(incomingChannel, (BookInfoResponse) message);
                } else if (message instanceof ChapterInfoResponse) {
                    processChapterInfoResponse(incomingChannel, (ChapterInfoResponse) message);
                } else if (message instanceof ChapterDataRequest) {
                    processChapterDataRequest((ChapterDataRequest) message);
                } else if (message instanceof ChapterDataResponse) {
                    processChapterDataResponse((ChapterDataResponse) message);
                } else {
                    log.severe("Message of unexpected type " + message.getClass());
                }
            }
        };
    }

    private void processChapterDataRequest(@NotNull ChapterDataRequest chapterDataRequest) {
        ChapterId chapterId = chapterDataRequest.getId();
        ChapterDataResponse response = new ChapterDataResponse(chapterId, bookCatalogue.getChapterData(chapterId));
        try {
            SocketChannel peer = SocketChannel.open(chapterDataRequest.getAddress());
            messageProcessor.sendMessage(peer, response);
        } catch (IOException e) {
            log.severe("Failed to establish connection to peer " + chapterDataRequest.getAddress());
        }
    }

    private void processChapterDataResponse(@NotNull ChapterDataResponse chapterDataResponse) {
        byte[] data = chapterDataResponse.getData();
        ChapterId id = chapterDataResponse.getChapterId();
        if (data != null) {
            bookCatalogue.addChapterData(id, data);
            chaptersAcquired.add(id);
        } else {
            chaptersInterestedIn.add(id);
        }
    }
}
