package ru.spbau.talanov.books.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.BookInfo;

import java.io.*;

public final class BookHeader implements Serializable {
    @NotNull
    private static final String HEADER_FILE_NAME = "header.hdr";

    @NotNull
    private final BookInfo info;

    @NotNull
    private final transient File headerFile;

    private BookHeader(@NotNull BookInfo info, @NotNull File headerFile) {
        this.info = info;
        this.headerFile = headerFile;
    }

    @Nullable
    public static BookHeader readFromDisk(@NotNull File bookFolder) throws IOException {
        File headerFile = getHeaderFile(bookFolder);
        if (!headerFile.exists()) {
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(headerFile);
             ObjectInputStream in = new ObjectInputStream(fileInputStream)
        ) {
            try {
                return (BookHeader) in.readObject();
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error while deserializing book header.", e);
            }
        }
    }

    @NotNull
    public static BookHeader createNewHeader(@NotNull File bookFolder, @NotNull BookInfo info) throws IOException {
        BookHeader bookHeader = new BookHeader(info, getHeaderFile(bookFolder));
        bookHeader.dumpOnDisk();
        return bookHeader;
    }

    @NotNull
    private static File getHeaderFile(@NotNull File bookFolder) {
        return new File(bookFolder, HEADER_FILE_NAME);
    }


    public void dumpOnDisk() throws IOException {
        Utils.ensureExists(headerFile);
        try (FileOutputStream fileOutputStream = new FileOutputStream(headerFile);
             ObjectOutputStream out = new ObjectOutputStream(fileOutputStream)) {
            out.writeObject(this);
        }
    }

    @NotNull
    public BookInfo getBookInfo() {
        return info;
    }
}
