package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.net.InetSocketAddress;

public final class ChapterDataRequest implements PeerToPeerMessage {

    @NotNull
    private final ChapterId id;
    @NotNull
    private final InetSocketAddress address;

    public ChapterDataRequest(@NotNull ChapterId id, @NotNull InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    @NotNull
    public ChapterId getId() {
        return id;
    }

    @NotNull
    public InetSocketAddress getAddress() {
        return address;
    }
}
