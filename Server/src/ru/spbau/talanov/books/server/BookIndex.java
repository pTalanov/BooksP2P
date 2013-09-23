package ru.spbau.talanov.books.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.BookId;
import ru.spbau.talanov.books.data.id.BookInfo;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BookIndex {

    @NotNull
    private final ConcurrentHashMap<ChapterId, Set<InetSocketAddress>> chapterToOwners = new ConcurrentHashMap<>();

    @NotNull
    private final ConcurrentHashMap<BookId, BookInfo> bookToInfo = new ConcurrentHashMap<>();


    public void addBook(@NotNull BookInfo bookInfo) {
        BookId id = bookInfo.getBookId();
        //NOTE: we do not deal with situation when several clients are trying to register different books under same id
        bookToInfo.putIfAbsent(id, bookInfo);
    }

    public void registerChapterOwner(@NotNull ChapterId chapterId, @NotNull InetSocketAddress address) {
        chapterToOwners.putIfAbsent(chapterId, Collections.synchronizedSet(new HashSet<InetSocketAddress>()));
        Set<InetSocketAddress> owners = chapterToOwners.get(chapterId);
        owners.add(address);
    }

    @Nullable
    public BookInfo getInfo(@NotNull BookId id) {
        return bookToInfo.get(id);
    }

    @NotNull
    public Collection<InetSocketAddress> getChapterOwners(@NotNull ChapterId chapterId) {
        return new HashSet<>(chapterToOwners.get(chapterId));
    }
}
