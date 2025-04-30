package me.antidotaleks.iter;

import me.antidotaleks.iter.elements.Game;
import me.antidotaleks.iter.events.PlayerCancelQueueEvent;
import me.antidotaleks.iter.events.PlayerQueueEvent;
import me.antidotaleks.iter.maps.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;

import static me.antidotaleks.iter.Iter.tryCatch;

public final class GameManager implements Listener {
    public static ArrayList<Game> games = new ArrayList<>();

    public static void startGame(Map map, ArrayList<PlayerQueueEvent>[] playerQueues) {
        Player[][] players = new Player[playerQueues.length][];
        for (int i = 0; i < playerQueues.length; i++) {
            if (playerQueues[i] == null)
                continue;
            players[i] =
                    playerQueues[i].stream()
                            .flatMap(e -> Arrays.stream(e.getTeam()))
                            .toArray(Player[]::new);
        }
        startGame(map, players);
    }

    public static void startGame(Map map, Player[][] players) {
        Game thisGame = new Game(players, map); // Create new game instance
        Bukkit.getPluginManager().registerEvents(thisGame, Iter.plugin);
        new BukkitRunnable() { @Override public void run() {
            thisGame.startGame();
        }}.runTaskLater(Iter.plugin, 20);

        games.add(thisGame);
    }

    public static void stopGame(Game game) {
        if (game == null)
            return;

        tryCatch(game::stopGame);
        HandlerList.unregisterAll(game);

        games.remove(game);
    }


    public static final ArrayList<Map> MAPS = new ArrayList<>();
    public static final ArrayList<String> MAP_NAMES = new ArrayList<>();
    private static final ArrayList<PlayerQueueEvent> QUEUES = new ArrayList<>();

    @EventHandler
    public void onPlayerQueue(PlayerQueueEvent event) {
        QUEUES.add(event);

        sortQueues();

        MapWithQueues mwq = findRandomMapAndQueues();

        if (mwq == null)
            return;
        if (mwq.shortage > 0) {
            Iter.logger.info("Awaiting for more players for map "+mwq.map.getDisplayName()+". Shortage: "+mwq.shortage + " players");
            return;
        }

        Iter.logger.info("[Iter] Starting game on map "+mwq.map.getDisplayName()+" with:");

        ArrayList<PlayerQueueEvent >[] queues = mwq.queues;
        for (int teamIndex = 0; teamIndex < queues.length; teamIndex++) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("       - Team %d: ", teamIndex));
            ArrayList<PlayerQueueEvent> team = queues[teamIndex];
            team.stream()
                    .flatMap(e -> Arrays.stream(e.getTeam()))
                    .forEach(p -> sb.append(p.getName()).append(" "));

            Iter.logger.info(sb.toString());
        }

        for (ArrayList<PlayerQueueEvent> queueList : queues) {
            for (PlayerQueueEvent playerQueueEvent : queueList) {
                QUEUES.remove(playerQueueEvent);
            }
        }
        startGame(mwq.map, mwq.queues);


    }

    @EventHandler
    public void onPlayerLeaveQueue(PlayerCancelQueueEvent event) {
        QUEUES.removeIf(playerQueueEvent -> playerQueueEvent.getHost().equals(event.getHost()));
    }

    /**
     * Sorts the queue by team size and queue time
     */
    private void sortQueues() {
        // Sort queues by size (bigger first), then by time (older first)
        QUEUES.sort((o1, o2) -> {
            if (o1.getTeamSize() != o2.getTeamSize()) {
                return o2.getTeamSize() - o1.getTeamSize();
            }
            return (int) (o1.getQueueStartTime() - o2.getQueueStartTime());
        });

    }

    /**
     * Finds a random map that fits the queues
     * @return MapWithQueues object that contains the map and the queues that fit into it
     */
    private static MapWithQueues findRandomMapAndQueues() {
        // Keep all suitable maps
        ArrayList<MapWithQueues> suitableMaps = new ArrayList<>();

        for (Map map : MAPS) {
            int[] teamCaps = map.getPlayersAmountInTeams().clone();
            // List of queues that fit into the map (have the map in their filters)
            //noinspection unchecked
            ArrayList<PlayerQueueEvent>[] fittedQueues = new ArrayList[map.getTeamsAmount()];
            for (int i = 0; i < fittedQueues.length; i++)
                fittedQueues[i] = new ArrayList<>();

            for (PlayerQueueEvent queue : QUEUES) {
                if (!queue.getMapFilters().contains(map.getDisplayName()))
                    continue;

                int teamSize = queue.getTeamSize();

                // Filling the map size array with the number of players in each team
                for (int i = 0; i < teamCaps.length; i++) {
                    if (teamCaps[i] >= teamSize) {
                        teamCaps[i] -= teamSize;
                        fittedQueues[i].add(queue);
                        break;
                    }
                }
            }

            // If at least 1 queue fits into the map, add it to the suitable maps list with the player shortage amount
            if (Arrays.stream(fittedQueues).anyMatch(queues -> !queues.isEmpty())) {
                int shortage =
                        Arrays.stream(map.getPlayersAmountInTeams()).sum()
                           - Arrays.stream(fittedQueues).flatMapToInt(list ->
                                list.stream().mapToInt(PlayerQueueEvent::getTeamSize)).sum();
                suitableMaps.add(new MapWithQueues(map, fittedQueues, shortage));
            }
        }

        // Return null if no suitable maps
        if (suitableMaps.isEmpty())
            return null;

        // Get smallest shortage value
        int minShortage = suitableMaps.stream().mapToInt(MapWithQueues::shortage).min().getAsInt();

        // Remove all maps with bigger shortage
        suitableMaps.removeIf(mapWithQueues -> mapWithQueues.shortage() > minShortage);

        // Return random map from suitable ones
        return suitableMaps.get((int) (Math.random() * suitableMaps.size()));
    }


    private record MapWithQueues(Map map, ArrayList<PlayerQueueEvent>[] queues, int shortage) {}

}
