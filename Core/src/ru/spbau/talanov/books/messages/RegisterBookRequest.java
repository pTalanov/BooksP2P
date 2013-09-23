package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.BookInfo;

public final class RegisterBookRequest implements PeerToServerMessage {
    @NotNull
    private final BookInfo bookInfo;

    public RegisterBookRequest(@NotNull BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }

    @NotNull
    public BookInfo getBookInfo() {
        return bookInfo;
    }

}
