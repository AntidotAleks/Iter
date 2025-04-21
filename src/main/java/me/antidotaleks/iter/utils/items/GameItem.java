package me.antidotaleks.iter.utils.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.maps.Map;
import me.antidotaleks.iter.utils.items.specific.GameItemInterface;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for all game items
 * Override the usable and use method to implement the item's functionality
 */
public abstract class GameItem implements GameItemInterface {

    protected final GamePlayer player;
    protected final Map map;

    /**
     * Create a new game item
     * @param player the player that owns the item
     */
    public GameItem(GamePlayer player) {
        this.player = player;
        this.map = player.getGame().getMap();
    }

    protected Point getCurrentPosition() {
        List<Point> stepPlanning = player.getStepPlanning();
        if (stepPlanning.isEmpty())
            return player.getPosition();

        return stepPlanning.getLast();
    }

    public char getCardSymbol() {
        return '\uE000';
    }

    /**
     * item priority over other items, higher priority means it will be used first, needed so that movement items are used after all other items, so that ally-target items can be used on a given step <br>
     * items with the same priority will are used at the same time and in random order. Items with lower priority will be used after items with higher priority. <br>
     * Default value is 0, movement items generally have priority -1, buffing (heal, damage increase, etc.) ally-target items have priority 1
     * @return priority of the item as an integer
     */
    public int priority() {
        return 0;
    }

    /**
     * time of the animation of the item in ticks, for a cosmetic delay
     * @return time of the animation in ticks
     */
    public int useTime() {
        return 5;
    }

    /**
     * @return dx+dy
     */
    protected static int tilesAwayTaxi(Point coords1, Point coords2) {
        return Math.abs(coords1.x-coords2.x) + Math.abs(coords1.y-coords2.y);
    }

    /**
     * @return dx^2+dy^2
     */
    protected static float tilesAwayEuclidean(Point coords1, Point coords2) {
        return (float) Math.sqrt(Math.pow(coords1.x-coords2.x, 2) + Math.pow(coords1.y-coords2.y, 2));
    }

    public enum TargetSelector {
        SELF(true, false, false, false),
        ALLY(false, true, false, false),
        ALLY_OR_SELF(true, true, false, false),
        ENEMY(false, false, true, false),
        ANYONE(true, true, true, false),
        GROUND(false, false, false, true),
        GROUND_OR_SELF(true, false, false, true),
        GROUND_OR_ALLY(false, true, false, true),
        GROUND_OR_ALLY_OR_SELF(true, true, false, true),
        GROUND_OR_ENEMY(false, false, true, true),
        GROUND_OR_ANYONE(true, true, true, true),
        EMPTY_GROUND(false, false, false, true),;

        private final boolean self;
        private final boolean ally;
        private final boolean enemy;
        private final boolean ground;

        TargetSelector(boolean self, boolean ally, boolean enemy, boolean ground) {
            this.self = self;
            this.ally = ally;
            this.enemy = enemy;
            this.ground = ground;
        }

        public boolean canUseAt(Point coords, GamePlayer playerUsing, int step) {
            if (this == EMPTY_GROUND)
                return isEmptyGround(coords, playerUsing, step);

            if (ground)
                return true;

            GamePlayer target = playerUsing.getGame().getPlayer(coords, step);

            if (target == null)
                return false;

            if (self && target.equals(playerUsing))
                return true;
            else if (ally && Arrays.asList(target.getTeam()).contains(playerUsing))
                return true;
            else return enemy && !Arrays.asList(target.getTeam()).contains(playerUsing);
        }

        private static boolean isEmptyGround(Point coords, GamePlayer player, int step) {
            return GROUND.canUseAt(coords, player, step) && !ANYONE.canUseAt(coords, player, step);
        }
    }
}
