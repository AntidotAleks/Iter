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
import java.util.HashMap;

public class GameManager implements Listener {
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


    public static ArrayList<Map> maps = new ArrayList<>();
    HashMap<String, Map> mapNames = new HashMap<>();
    ArrayList<PlayerQueueEvent> queue = new ArrayList<>();

    @EventHandler
    public void onPlayerQueue(PlayerQueueEvent event) {
        queue.add(event);

        sortQueues();
        // TODO: implement queue logic
        MapWithQueues mwq = findRandomMapAndQueues();

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
    private MapWithQueues findRandomMapAndQueues() {
        // Keep all suitable maps
        ArrayList<MapWithQueues> suitableMaps = new ArrayList<>();

        for (Map map : maps) {
            int[] teamCaps = map.getPlayersAmountInTeams().clone();
            // List of queues that fit into the map (have the map in their filters)
            ArrayList<PlayerQueueEvent> fittedQueues = new ArrayList<>();

            for (PlayerQueueEvent queue : queue) {
                if (!queue.getMapFilters().contains(map.displayName()))
                    continue;

                int teamSize = queue.getTeamSize();

                // Filling the map size array with the number of players in each team
                for (int i = 0; i < teamCaps.length; i++) {
                    if (teamCaps[i] >= teamSize) {
                        teamCaps[i] -= teamSize;
                        fittedQueues.add(queue);
                        break;
                    }
                }
            }

            // Якщо бодай одна черга помістилася, вважаємо цю мапу придатною
            if (!fittedQueues.isEmpty()) {
                suitableMaps.add(new MapWithQueues(map, fittedQueues));
            }
        }

        // Якщо жодна мапа не підійшла
        if (suitableMaps.isEmpty()) {
            return null;
        }

        // Обираємо випадкову мапу з придатних
        return suitableMaps.get((int) (Math.random() * suitableMaps.size()));
    }



    public static class MapWithQueues {
        private final Map map;
        private final ArrayList<PlayerQueueEvent> queues;

        public MapWithQueues(Map map, ArrayList<PlayerQueueEvent> queues) {
            this.map = map;
            this.queues = queues;
        }

        public Map getMap() {
            return map;
        }

        public ArrayList<PlayerQueueEvent> getQueues() {
            return queues;
        }
    }

}
