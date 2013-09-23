package ru.spbau.talanov.books.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.BookInfo;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Book {

    @NotNull
    private final File folder;

    @NotNull
    private final BookHeader header;


    @Nullable
    public static Book readBook(@NotNull File bookFolder) throws IOException {
        BookHeader bookHeader = BookHeader.readFromDisk(bookFolder);
        if (bookHeader == null) {
            return null;
        }
        return new Book(bookFolder, bookHeader);
    }

    @NotNull
    public static Book createBook(@NotNull BookInfo bookInfo,
                                  @NotNull File bookFolder) throws IOException {
        return new Book(bookFolder, BookHeader.createNewHeader(bookFolder, bookInfo));
    }

    private Book(@NotNull File folder, @NotNull BookHeader header) {
        this.folder = folder;
        this.header = header;
    }

    @NotNull
    public Chapter createNewChapter(@NotNull ChapterId id) {
        return new Chapter(id, this);
    }

    @NotNull
    public File getFolder() {
        return folder;
    }

    @NotNull
    public BookInfo getInfo() {
        return header.getBookInfo();
    }


    @NotNull
    public List<Chapter> getAvailableChapters() {
        List<Chapter> result = new ArrayList<>();
        for (ChapterId chapterId : getInfo().getAllChapterIds()) {
            Chapter chapter = Chapter.readChapter(this, chapterId);
            if (chapter != null) {
                result.add(chapter);
            }
        }
        return result;
    }


}
