package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final Player player;
    private final Game.TeamDetails teamDetails;

    private final TextDisplay infoDisplay;
    private final ItemDisplay cursor;
    private BukkitRunnable cursorUpdater;

    public InfoDisplay(GamePlayer player) {
        this.gamePlayer = player;
        this.player = player.getPlayer();
        teamDetails = gamePlayer.getTeamDetails();

        // Create displays
        infoDisplay = newNicknameInfo(true);

        cursor = newCursor();
        newCursorUpdater();

        updateData();


    }

    private TextDisplay newNicknameInfo(boolean hideFromPlayer) {
        TextDisplay infoDisplay = this.player.getWorld().spawn(gamePlayer.getGame().getMapLocation(), TextDisplay.class);

        infoDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        infoDisplay.setSeeThrough(true);
        infoDisplay.setShadowed(true);
        infoDisplay.setBrightness(new Display.Brightness(15, 15));
        infoDisplay.setBillboard(Display.Billboard.CENTER);
        infoDisplay.setTextOpacity((byte) 128);
        infoDisplay.setBackgroundColor(Color.fromARGB(0,0,0,0));

        // hide info for themselves
        if (hideFromPlayer)
            this.player.hideEntity(Iter.plugin, infoDisplay);

        return infoDisplay;
    }

    private static final ItemStack CURSOR_DATA = new ItemStack(Material.GLASS_PANE, 1);

    private ItemDisplay newCursor() {
        ItemDisplay cursor = this.player.getWorld().spawn(gamePlayer.getGame().getMapLocation(), ItemDisplay.class);

        cursor.setItemStack(CURSOR_DATA);
        cursor.setBrightness(new Display.Brightness(15, 15));
        cursor.setTeleportDuration(1);
        cursor.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                new Vector3f(3f, 3, 0.1f),
                new AxisAngle4f()
        ));

        gamePlayer.getGame().getAllPlayers().forEach(p -> p.hideEntity(Iter.plugin, cursor));
        player.showEntity(Iter.plugin, cursor);

        return cursor;
    }

    private void newCursorUpdater() {

        cursorUpdater = new BukkitRunnable() {
                    Location lastLocation = player.getLocation();
                    @Override
                    public void run() {
                        Location newLocation = player.getLocation();
                        if (newLocation.equals(lastLocation))
                            return;

                        lastLocation = newLocation;

                        Location lookAt = gamePlayer.getLookTileWorldPosition();
                        if (lookAt != null) {
                            cursor.teleport(lookAt);
                            return;
                        }
                        Location underMap = cursor.getLocation();
                        underMap.setY(0);
                        cursor.teleport(underMap);
                    }
                };
    }

    // Display

    public void updateData() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();
        String teamName = "Team " + teamDetails.toString();

        // Update scoreboard
        String infoString = String.format("%s%s: %s%s\n"+ ChatColor.of("#ff5252") +"❤%d/%d "+ChatColor.RESET+"| "+ChatColor.of("#5297ff")+"♦ %d/%d\n",
                        ChatColor.of(teamDetails.color), teamName, ChatColor.of(teamDetails.lightColor), this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
    }

    public void showCursor() {
        player.showEntity(Iter.plugin, cursor);
        cursorUpdater.runTaskTimerAsynchronously(Iter.plugin, 0, 1);
    }

    public void hideCursor() {
        player.hideEntity(Iter.plugin, cursor);
        try {
            cursorUpdater.cancel();
        } catch (Exception ignored) {}
        newCursorUpdater();
    }

    // Utils

    public void remove() {
        infoDisplay.remove();
        try {
            cursorUpdater.cancel();
        } catch (Exception ignored) {}
        cursor.remove();
    }

    public void mount() {
        this.player.addPassenger(infoDisplay);
    }

    public void dismount() {
        this.player.removePassenger(infoDisplay);
    }

    // Getters

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public Player getPlayer() {
        return player;
    }
}