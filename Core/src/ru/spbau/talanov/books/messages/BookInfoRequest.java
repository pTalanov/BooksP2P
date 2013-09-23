package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.BookId;

public final class BookInfoRequest implements PeerToServerMessage {
    @NotNull
    private final BookId bookId;

    public BookInfoRequest(@NotNull BookId bookId) {
        this.bookId = bookId;
    }

    @NotNull
    public BookId getBookId() {
        return bookId;
    }
}
