package ru.spbau.talanov.books.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.BookId;
import ru.spbau.talanov.books.data.id.BookInfo;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class BookCatalogue {

    @NotNull
    private static final Logger LOG = Logger.getLogger(BookCatalogue.class.getSimpleName());
    @NotNull
    private final Map<BookId, Book> idToBook = new ConcurrentHashMap<>();
    @NotNull
    private final ConcurrentHashMap<ChapterId, Chapter> idToChapter = new ConcurrentHashMap<>();
    @NotNull
    private final List<Book> preExistingBooks;
    @NotNull
    private final File folder;


    private BookCatalogue(@NotNull List<Book> books, @NotNull File folder) {
        this.preExistingBooks = books;
        this.folder = folder;
        for (Book book : books) {
            idToBook.put(book.getInfo().getBookId(), book);
            for (Chapter chapter : book.getAvailableChapters()) {
                idToChapter.put(chapter.getId(), chapter);
            }
        }
    }

    @NotNull
    public static BookCatalogue readCatalogue(@NotNull File folder) {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException(folder.getAbsolutePath() + " should be a directory.");
        }
        List<Book> books = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("IO error has occured while reading " + folder.getAbsoluteFile());
        }
        for (File subFolder : files) {
            if (!subFolder.isDirectory()) {
                throw new IllegalArgumentException(folder.getAbsolutePath() + " should contain book directories\n" +
                        subFolder.getAbsoluteFile() + " is not a directory.");
            }
            try {
                Book book = Book.readBook(subFolder);
                if (book == null) {
                    LOG.severe("Book folder with no header file");
                } else {
                    books.add(book);
                }
            } catch (IOException e) {
                LOG.severe("IO error while reading book: " + e.getMessage());
            }
        }
        return new BookCatalogue(books, folder);
    }

    @Nullable
    public byte[] getChapterData(@NotNull ChapterId chapterId) {
        Chapter chapter = idToChapter.get(chapterId);
        if (chapter == null) {
            return null;
        }
        return chapter.getData();
    }

    @Nullable
    private Book getBook(@NotNull BookId bookId) {
        return idToBook.get(bookId);
    }

    @Nullable
    public synchronized Book createBook(@NotNull BookInfo bookInfo) {
        Book book = getBook(bookInfo.getBookId());
        if (book != null) {
            return book;
        }
        try {
            File bookFolder = new File(folder, bookInfo.getBookId().getName());
            boolean success = bookFolder.mkdir();
            if (!success) {
                LOG.severe("Could not create folder " + bookFolder.getAbsolutePath());
                return null;
            }
            book = Book.createBook(bookInfo, bookFolder);
            idToBook.put(book.getInfo().getBookId(), book);
            return book;
        } catch (IOException e) {
            LOG.severe("Error writing book header for " + bookInfo);
            return null;
        }
    }

    public void addChapterData(@NotNull ChapterId chapterId, @NotNull byte[] data) {
        Book book = getBook(chapterId.getBookId());
        if (book == null) {
            LOG.severe("Error writing chapter " + chapterId);
            return;
        }
        Chapter newChapter;
        synchronized (book) {
            if (idToChapter.contains(chapterId)) {
                return;
            }
            newChapter = book.createNewChapter(chapterId);
            idToChapter.put(chapterId, newChapter);
        }
        try {
            newChapter.dumpOnDisk(data);
        } catch (IOException e) {
            LOG.severe("Error writing chapter do disk: " + e.getMessage());
        }
    }

    @NotNull
    public List<BookInfo> getPreExistingBooks() {
        List<BookInfo> result = new ArrayList<>();
        for (Book preExistingBook : preExistingBooks) {
            result.add(preExistingBook.getInfo());
        }
        return result;
    }

    @NotNull
    public Collection<ChapterId> getAllExistingChapterIds() {
        return new HashSet<>(idToChapter.keySet());
    }
}
