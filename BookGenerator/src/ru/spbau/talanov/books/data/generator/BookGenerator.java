package ru.spbau.talanov.books.data.generator;

import ru.spbau.talanov.books.data.BookHeader;
import ru.spbau.talanov.books.data.Chapter;
import ru.spbau.talanov.books.data.id.BookId;
import ru.spbau.talanov.books.data.id.BookInfo;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class BookGenerator {

    private static final int CHAPTER_SIZE_MULTIPLIER = 10000;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            return;
        }
        File bookFolder = new File(args[0]);
        if (!bookFolder.exists()) {
            bookFolder.mkdir();
        }
        int chapterCount = Integer.parseInt(args[1]);
        String bookName = bookFolder.getName();
        BookInfo bookInfo = new BookInfo(new BookId(bookName), chapterCount, "Automatically generated book");
        BookHeader.createNewHeader(bookFolder, bookInfo);
        for (ChapterId chapterId : bookInfo.getAllChapterIds()) {
            try (RandomAccessFile file = new RandomAccessFile(new File(bookFolder, Chapter.getChapterFileName(chapterId)), "rw")) {
                file.writeUTF(chapterId.toString());
                for (int i = 0; i < CHAPTER_SIZE_MULTIPLIER; ++i) {
                    file.writeUTF(chapterId.getId() + "\n");
                }
            }
        }

        System.out.println("Created book " + bookName + " in " + bookFolder.getAbsolutePath());
    }
}
