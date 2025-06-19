package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.Game;
import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.elements.GameTeam;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Processes item usage in two phases:
 * 1) Build a per-round priority queue via getNextItemPriority()
 * 2) Consume each priority group (useNextItem()), then repeat until no items remain
 */
public class RoundCompletionProcessor {
    private static final int
            BETWEEN_STEP_DELAY = 5,
            BETWEEN_GROUP_DELAY = 0;

    private final GameTeam team;
    private final Game game;
    private final Runnable onRoundEnd;
    private Deque<List<GamePlayer>> currentStepQueue;

    public RoundCompletionProcessor(GameTeam team, Runnable onRoundEnd) {
        this.team = team;
        this.game = team.getGame();
        this.onRoundEnd = onRoundEnd;
    }

    /**
     * Kick off the first display update and round scheduling.
     */
    public void start() {
        game.getTeams().forEach(GameTeam::updateTeamDisplay);
        game.getAllGamePlayers().forEach(GamePlayer::updateInfo);
        scheduleNextStep(0);
    }

    /**
     * Builds the next round's priority queue, or ends the round if empty.
     */
    private void scheduleNextStep(long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
            }
        }.runTaskLater(Iter.plugin, delay);

        Bukkit.getScheduler().runTaskLater(Iter.plugin, () -> {
            currentStepQueue = buildPriorityQueueForNextStep();
            if (currentStepQueue.isEmpty())
                onRoundEnd.run();
            else
                scheduleNextGroup(0); // Start consuming the first group immediately
        }, delay);
    }

    /**
     * Groups all players by getNextItemPriority(), descending.
     */
    private Deque<List<GamePlayer>> buildPriorityQueueForNextStep() {
        // Create a map of players grouped by their next item priority, highest to lowest
        TreeMap<Integer, List<GamePlayer>> byPriority = new TreeMap<>(Comparator.reverseOrder());
        for (GamePlayer player : team.getPlayers()) {
            int priority = player.getNextItemPriority();
            if (priority == Integer.MIN_VALUE) continue;
            byPriority.computeIfAbsent(priority, k -> new ArrayList<>()).add(player);
        }
        Iter.logger.info("Priority queue has levels: " + byPriority.keySet());
        // Turn the map into a queue of lists
        return new ArrayDeque<>(byPriority.values());
    }

    /**
     * Consumes one priority group (useNextItem for each player), updates UI,
     * then schedules the next group or next round.
     */
    private void scheduleNextGroup(long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentStepQueue.isEmpty()) {
                    // Finished all groups: start new step
                    scheduleNextStep(BETWEEN_STEP_DELAY);
                    return;
                }
                game.gameDisplay.decrementPaths(); // Update path indexes to change hue according to the current step
                List<GamePlayer> group = currentStepQueue.pollFirst();
                int nextDelay = group.stream()
                        .mapToInt(GamePlayer::useNextItem)
                        .max()
                        .orElse(0) + BETWEEN_GROUP_DELAY;

                // Refresh UI after each group's items are used
                game.getTeams().forEach(GameTeam::updateTeamDisplay);
                game.getAllGamePlayers().forEach(GamePlayer::updateInfo);

                // Continue with the next group after its animation delay
                scheduleNextGroup(nextDelay);
            }
        }.runTaskLater(Iter.plugin, delay);
    }
}
