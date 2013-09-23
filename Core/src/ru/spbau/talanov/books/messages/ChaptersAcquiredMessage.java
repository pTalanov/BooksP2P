package ru.spbau.talanov.books.messages;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.data.id.ChapterId;

import java.net.InetSocketAddress;
import java.util.Collection;

public final class ChaptersAcquiredMessage implements PeerToServerMessage {

    @NotNull
    private final InetSocketAddress senderAddress;
    @NotNull
    private final Collection<ChapterId> chapters;

    public ChaptersAcquiredMessage(@NotNull InetSocketAddress senderAddress, @NotNull Collection<ChapterId> chapters) {
        this.senderAddress = senderAddress;
        this.chapters = chapters;
    }

    @NotNull
    public InetSocketAddress getSenderAddress() {
        return senderAddress;
    }

    @NotNull
    public Collection<ChapterId> getChapters() {
        return chapters;
    }
}
