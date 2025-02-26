package me.antidotaleks.iter.events;

import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerFinishTurnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer player;

    /**
     * PlayerCancelQueueEvent constructor
     * @param player The host of the queue
     */
    public PlayerFinishTurnEvent(GamePlayer player) {
        this.player = player;
    }

    /**
     * Get the host, who cancelled the queue
     * @return The host of the queue
     */
    public GamePlayer getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
