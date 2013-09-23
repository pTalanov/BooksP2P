package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.BookId;
import ru.spbau.talanov.books.data.id.BookInfo;

public final class BookInfoResponse implements ServerToPeerMessage {

    @Nullable
    private final BookInfo info;

    @NotNull
    private final BookId id;

    public BookInfoResponse(@Nullable BookInfo info, @NotNull BookId id) {
        this.info = info;
        this.id = id;
    }

    @Nullable
    public BookInfo getInfo() {
        return info;
    }

    @NotNull
    public BookId getId() {
        return id;
    }
}
