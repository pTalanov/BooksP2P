package ru.spbau.talanov.books.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class Chapter {

    @NotNull
    private static final String CHAPTER_FILE_EXTENSION = ".chp";

    @NotNull
    private final ChapterId id;
    @NotNull
    private final Book book;

    @NotNull
    private SoftReference<DataHolder> holder = new SoftReference<>(null);

    private static class DataHolder {
        @NotNull
        private final byte[] data;

        private DataHolder(@NotNull byte[] data) {
            this.data = data;
        }
    }

    @Nullable
    public static Chapter readChapter(@NotNull Book book, @NotNull ChapterId id) {
        File file = getChapterFile(book, id);
        if (!file.exists()) {
            return null;
        }
        if (!file.isFile()) {
            throw new IllegalStateException(file.getAbsolutePath() + " should be a file!");
        }
        return new Chapter(id, book);
    }

    /*package local*/ Chapter(@NotNull ChapterId id, @NotNull Book book) {
        this.id = id;
        this.book = book;
    }

    @Nullable
    private synchronized byte[] readFromDisk() {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(getChapterFile(book, id), "r")) {
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void dumpOnDisk(@NotNull byte[] data) throws IOException {
        File chapterFile = getChapterFile(book, id);
        Utils.ensureExists(chapterFile);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(chapterFile, "rw")) {
            randomAccessFile.write(data);
        }
        holder = new SoftReference<>(new DataHolder(data));
    }

    @NotNull
    private static File getChapterFile(@NotNull Book book, @NotNull ChapterId id) {
        return new File(book.getFolder(), getChapterFileName(id));
    }

    @NotNull
    public static String getChapterFileName(@NotNull ChapterId id) {
        return id.getId() + CHAPTER_FILE_EXTENSION;
    }

    @NotNull
    public ChapterId getId() {
        return id;
    }

    @Nullable
    public synchronized byte[] getData() {
        DataHolder dataHolder = holder.get();
        if (dataHolder == null) {
            byte[] bytes = readFromDisk();
            if (bytes != null) {
                dataHolder = new DataHolder(bytes);
            } else {
                return null;
            }
        }
        return dataHolder.data;
    }
}
