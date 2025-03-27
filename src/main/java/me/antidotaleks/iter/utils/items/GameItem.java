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

    protected static int tilesAway(Point coords1, Point coords2) {
        return Math.abs(coords1.x-coords2.x) + Math.abs(coords1.y-coords2.y);
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
        GROUND_OR_ANYONE(true, true, true, true);

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

        public boolean isSelf() {
            return self;
        }

        public boolean isAlly() {
            return ally;
        }

        public boolean isEnemy() {
            return enemy;
        }

        public boolean isGround() {
            return ground;
        }

        public boolean isAcceptable(Point coords, GamePlayer playerUsing) {
            if (ground)
                return true;

            GamePlayer target = playerUsing.getGame().getPlayer(coords);

            if (target == null)
                return false;

            if (self && target.equals(playerUsing))
                return true;
            else if (ally && Arrays.asList(target.getTeam()).contains(playerUsing))
                return true;
            else return enemy && !Arrays.asList(target.getTeam()).contains(playerUsing);
        }

        public static boolean isEmptyGround(Point coords, GamePlayer player) {
            return GROUND.isAcceptable(coords, player) && player.getGame().getPlayer(coords) == null;
        }
    }
}
