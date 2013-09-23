package ru.spbau.talanov.books.data.id;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BookInfo implements Serializable {

    private final int chapterCount;
    @NotNull
    private final String description;
    @NotNull
    private final BookId bookId;

    public BookInfo(@NotNull BookId id, int chapterCount, @NotNull String description) {
        this.chapterCount = chapterCount;
        this.description = description;
        this.bookId = id;
    }

    @NotNull
    public BookId getBookId() {
        return bookId;
    }

    @NotNull
    public List<ChapterId> getAllChapterIds() {
        List<ChapterId> result = new ArrayList<>();
        for (int i = 1; i <= chapterCount; ++i) {
            result.add(new ChapterId(i, bookId));
        }
        return result;
    }
}
