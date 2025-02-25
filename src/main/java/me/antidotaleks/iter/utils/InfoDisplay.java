package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final Player player;
    private final TextDisplay infoDisplay;

    public InfoDisplay(GamePlayer player) {
        this.gamePlayer = player;
        this.player = player.getPlayer();

        // Create text display
        infoDisplay = this.player.getWorld().spawn(player.getGame().getMapLocation(), TextDisplay.class);

        infoDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        infoDisplay.setSeeThrough(true);
        infoDisplay.setShadowed(true);
        infoDisplay.setBrightness(new Display.Brightness(15, 15));
        infoDisplay.setBillboard(Display.Billboard.CENTER);
        // transparent
        infoDisplay.setBackgroundColor(Color.fromARGB(0,0,0,0));

        // hide info for themselves
        this.player.hideEntity(Iter.plugin, infoDisplay);

        updateData();

    }

    public void updateData() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();

        // Update scoreboard
        String infoString = String.format("%s\n"+ ChatColor.of("#ff5252") +"❤%d/%d "+ChatColor.RESET+"| "+ChatColor.of("#5297ff")+"♦ %d/%d\n",
                        this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
    }

    // Utils

    public void remove() {
        infoDisplay.remove();
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