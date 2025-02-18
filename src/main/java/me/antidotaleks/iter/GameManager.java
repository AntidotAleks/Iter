package me.antidotaleks.iter;

import me.antidotaleks.iter.events.PlayerCancelQueueEvent;
import me.antidotaleks.iter.events.PlayerQueueEvent;
import me.antidotaleks.iter.maps.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;

public final class GameManager implements Listener {
    public static ArrayList<Game> games = new ArrayList<>();

    public static void startGame(Map map, Player[][] players) {
        Location loc = map.buildMap(); // Build a map and get its location
        Bukkit.getOnlinePlayers().forEach(player->{
            player.teleport(loc.clone().add(0, 5, 0));
            player.sendMessage("Game started");
        }); // Tp players to it TODO remove
        Game thisGame = new Game(playersToTeams(), map); // Create new game instance
        games.add(thisGame);
    }

    private static Player[][] playersToTeams() {
        return new Player[][]{Bukkit.getOnlinePlayers().toArray(new Player[0])};
    }
    public static void stopGame(Game game) {
        game.stop();
        games.remove(game);
    }


    public static final ArrayList<Map> maps = new ArrayList<>();
    public static final ArrayList<String> mapNames = new ArrayList<>();
    private static final ArrayList<PlayerQueueEvent> queue = new ArrayList<>();

    @EventHandler
    public void onPlayerQueue(PlayerQueueEvent event) {
        queue.add(event);

        sortQueues();
        // TODO: implement queue logic
        MapWithQueues mwq = findRandomMapAndQueues();
        if (mwq == null)
            return;
        if (mwq.shortage > 0) {
            System.out.println("Not enough players for map "+mwq.map.displayName()+", awaiting for more. missing "+mwq.shortage);
            return;
        }
        System.out.println("[Iter] Starting game on map "+mwq.map.displayName()+" with: ");
        ArrayList<PlayerQueueEvent >[] queues = mwq.queues;
        for (int teamIndex = 0; teamIndex < queues.length; teamIndex++) {
            System.out.print("       - Team "+teamIndex+": ");
            ArrayList<PlayerQueueEvent> team = queues[teamIndex];
            for (PlayerQueueEvent playerQueueEvent : team) {
                System.out.print(playerQueueEvent.getHost().getName()+" ");
            }
            System.out.println();
        }


    }

    @EventHandler
    public void onPlayerLeaveQueue(PlayerCancelQueueEvent event) {
        queue.removeIf(playerQueueEvent -> playerQueueEvent.getHost().equals(event.getHost()));
    }

    /**
     * Sorts the queue by team size and queue time
     */
    private void sortQueues() {
        // Sort queues by size (bigger first), then by time (older first)
        queue.sort((o1, o2) -> {
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

        for (Map map : maps) {
            int[] teamCaps = map.getPlayersAmountInTeams().clone();
            // List of queues that fit into the map (have the map in their filters)
            //noinspection unchecked
            ArrayList<PlayerQueueEvent>[] fittedQueues = new ArrayList[map.getTeamsAmount()];
            for (int i = 0; i < fittedQueues.length; i++)
                fittedQueues[i] = new ArrayList<>();

            for (PlayerQueueEvent queue : queue) {
                if (!queue.getMapFilters().contains(map.displayName()))
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
