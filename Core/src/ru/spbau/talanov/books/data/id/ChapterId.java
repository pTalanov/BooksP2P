package ru.spbau.talanov.books.data.id;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public final class ChapterId implements Serializable {

    private final int id;

    @NotNull
    private final BookId bookId;

    public ChapterId(int id, @NotNull BookId bookId) {
        this.id = id;
        this.bookId = bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChapterId chapterId = (ChapterId) o;

        return id == chapterId.id && bookId.equals(chapterId.bookId);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + bookId.hashCode();
        return result;
    }

    public long getId() {
        return id;
    }

    @NotNull
    public BookId getBookId() {
        return bookId;
    }

    @Override
    public String toString() {
        return "ChapterId{" +
                "id=" + id +
                ", bookId=" + bookId +
                '}';
    }
}
