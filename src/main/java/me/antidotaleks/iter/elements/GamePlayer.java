package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

import java.awt.*;

public class GamePlayer implements Listener {
    private final Player player;
    private final Game game;

    public GamePlayer(Player player, Game game) {
        this.player = player;
        this.game = game;
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player))
            return;

        Point tilePos = getTilePos();
        if (tilePos == null)
            return;

        Iter.logger.info("Tile pos: " + tilePos);
    }

    public Point getTilePos() {
        if (player == null)
            return null;

        if (player.getLocation().getPitch() <= 0) // cancel if looks up
            return null;


        RayTraceResult result = player.rayTraceBlocks(50);
        if (result == null || result.getHitBlock() == null || result.getHitBlock().getLocation().getBlockY() != 0)
            return null;

        Location interactLoc = result.getHitBlock().getLocation();
        interactLoc.subtract(this.game.getMapLocation()).subtract(4, 0, 4).multiply(1d/3);

        Point coords = new Point(interactLoc.getBlockX(), interactLoc.getBlockZ());

        if(game.getMap().isWall(coords.x, coords.y))
            return null;

        return coords;
    }

    public Player getPlayer() {
        return player;
    }

    public Game getGame() {
        return game;
    }
}
