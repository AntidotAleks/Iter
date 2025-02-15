package me.antidotaleks.iter.events;

import me.antidotaleks.iter.Iter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerQueueEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player host;
    private final Player[] team;
    private final ArrayList<String> mapFilters;
    private final long queueStartTime;

    /**
     * PlayerQueueEvent constructor
     * @param host The host of the queue
     * @param team The host's team. Host is first in the array
     * @param mapFilters The map filters. If empty, all maps are allowed
     */
    public PlayerQueueEvent(Player host, Player[] team, ArrayList<String> mapFilters) {

        if (host == null)
            throw new IllegalArgumentException("Host cannot be null");
        if (mapFilters == null || mapFilters.isEmpty())
            throw new IllegalArgumentException("Map filters cannot be null or empty");

        this.host = host;

        // Ensure the host is first in the team array
        if (team == null) {
            Iter.logger.info("Team is null, automatically setting team to host. PlayerQueueEvent: " + this);
            team = new Player[]{host};
        }
        else if (team.length == 0 || !team[0].equals(host)) {
            Iter.logger.info("Host is not first in team array or is missing, automatically setting team to host. PlayerQueueEvent: " + this);
            ArrayList<Player> teamList = new ArrayList<>(Arrays.asList(team));
            teamList.remove(host);
            teamList.addFirst(host);
            team = teamList.toArray(new Player[0]);
        }
        this.team = team;

        this.mapFilters = mapFilters;

        this.queueStartTime = System.currentTimeMillis();

    }

    /**
     * Get the host of the queue
     * @return The host of the queue
     */
    public Player getHost() {
        return host;
    }

    /**
     * Get the team of the queue
     * @return The team of the queue
     */
    public Player[] getTeam() {
        return team;
    }
    public int getTeamSize() {
        return team.length;
    }

    /**
     * Get the map filters
     * @return The map filters
     */
    public ArrayList<String> getMapFilters() {
        return mapFilters;
    }

    /**
     * Get the time the queue was started
     * @return The time the queue was started from {@link System#currentTimeMillis()}
     */
    public long getQueueStartTime() {
        return queueStartTime;
    }



    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
