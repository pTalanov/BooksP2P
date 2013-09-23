package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.net.InetSocketAddress;
import java.util.Collection;

public final class ChapterInfoResponse implements ServerToPeerMessage {

    @NotNull
    private final ChapterId id;
    @NotNull
    private final Collection<InetSocketAddress> peers;

    public ChapterInfoResponse(@NotNull ChapterId id, @NotNull Collection<InetSocketAddress> peers) {
        this.id = id;
        this.peers = peers;
    }

    @NotNull
    public ChapterId getId() {
        return id;
    }

    @NotNull
    public Collection<InetSocketAddress> getPeers() {
        return peers;
    }
}
