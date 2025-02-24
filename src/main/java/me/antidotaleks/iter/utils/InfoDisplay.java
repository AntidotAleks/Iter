package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.SetupManager;
import me.antidotaleks.iter.elements.GamePlayer;
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

        // Create scoreboard
        SetupManager.hideNameTeam.addEntry(player.getPlayer().getName());

        infoDisplay = this.player.getWorld().spawn(this.player.getLocation(), TextDisplay.class);

        infoDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        infoDisplay.setSeeThrough(true);
        infoDisplay.setShadowed(true);
        infoDisplay.setBrightness(new Display.Brightness(15, 15));
        infoDisplay.setBillboard(Display.Billboard.CENTER);

        // mount player
        infoDisplay.addPassenger(this.player);
        this.player.hideEntity(Iter.plugin, infoDisplay);

        updateData();

    }

    public void updateData() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();

        // Update scoreboard
        String infoString = String.format("[\"\",\"%s\\\\n\",{\"color\":\"#ff5252\",\"text\":\"❤ %d/%d\"},\" | \",{\"color\":\"#5297ff\",\"text\":\"♦ %d/%d\"}]",
                        this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
    }

    public void remove() {
        infoDisplay.remove();
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public Player getPlayer() {
        return player;
    }
}