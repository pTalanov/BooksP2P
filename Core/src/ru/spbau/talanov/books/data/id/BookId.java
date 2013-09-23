package ru.spbau.talanov.books.data.id;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public final class BookId implements Serializable {

    @NotNull
    private final String name;

    public BookId(@NotNull String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookId bookId = (BookId) o;

        return name.equals(bookId.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BookId{" +
                "name='" + name + '\'' +
                '}';
    }
}
