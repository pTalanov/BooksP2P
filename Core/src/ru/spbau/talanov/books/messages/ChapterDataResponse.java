package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.talanov.books.data.id.ChapterId;

public final class ChapterDataResponse implements PeerToPeerMessage {

    @NotNull
    private final ChapterId chapterId;
    @Nullable
    private final byte[] data;

    public ChapterDataResponse(@NotNull ChapterId chapterId, @Nullable byte[] data) {
        this.chapterId = chapterId;
        this.data = data;
    }

    @NotNull
    public ChapterId getChapterId() {
        return chapterId;
    }

    @Nullable
    public byte[] getData() {
        return data;
    }
}
