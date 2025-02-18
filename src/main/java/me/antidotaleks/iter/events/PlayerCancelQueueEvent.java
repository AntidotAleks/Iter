package me.antidotaleks.iter.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerCancelQueueEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player host;

    /**
     * PlayerCancelQueueEvent constructor
     * @param host The host of the queue
     */
    public PlayerCancelQueueEvent(Player host) {
        this.host = host;
    }

    /**
     * Get the host, who cancelled the queue
     * @return The host of the queue
     */
    public Player getHost() {
        return host;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
