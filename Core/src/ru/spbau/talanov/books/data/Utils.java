package ru.spbau.talanov.books.data;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public final class Utils {

    private Utils() {

    }

    public static void ensureExists(@NotNull File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
