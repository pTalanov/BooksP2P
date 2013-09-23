package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.ChapterId;

public final class ChapterInfoRequest implements PeerToServerMessage {

    @NotNull
    private final ChapterId id;

    public ChapterInfoRequest(@NotNull ChapterId id) {
        this.id = id;
    }

    @NotNull
    public ChapterId getId() {
        return id;
    }
}
