package me.antidotaleks.iter;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class Iter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register custom events

        plugin = this;
        logger = getLogger();
        overworld = Bukkit.getWorlds().getFirst();
        pluginFolder = getDataFolder();
        protocolManager = ProtocolLibrary.getProtocolManager();

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(plugin);
        } catch (NoPacketAdapterAvailableException e) {
            scoreboardLibrary = new NoopScoreboardLibrary();
            logger.warning("No scoreboard packet adapter available!");
        }
        audiences = BukkitAudiences.create(plugin);

        Iter.logger.info("[Iter] Registering event listeners");
        SetupManager.registerListeners();

        Iter.logger.info("[Iter] Registering commands");
        SetupManager.registerCommands();

        Iter.logger.info("[Iter] Loading maps");
        SetupManager.loadFolders(); // Load maps data

        Iter.logger.info("[Iter] Starting team service");
        Teaming.onEnable(); // Remove invites older than 30 seconds and return players to the team lists
        SetupManager.setupMinecraftTeams();

    }

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.getLast());
        }

        scoreboardLibrary.close();
    }



    public static Iter plugin;
    public static Logger logger;
    public static World overworld;
    public static File pluginFolder;
    public static ProtocolManager protocolManager;
    public static ScoreboardLibrary scoreboardLibrary;
    public static BukkitAudiences audiences;


    public static final BlockData AIR_DATA = Bukkit.createBlockData(Material.AIR);
    public static final BlockData BARRIER_DATA = Bukkit.createBlockData(Material.BARRIER);

    // Utils

    public static TranslatableComponent offset(int offset) {
        if (offset < -8192 || offset > 8192) throw new IllegalArgumentException("Offset out of bounds, must be in [-8192, 8192]");
        return Component.translatable("space."+offset).style(Style.style().font(Key.key("default")));
    }
}
