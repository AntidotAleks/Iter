package me.antidotaleks.iter;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class Iter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register custom events

        plugin = this;
        logger = Logger.getLogger("Iter");
        overworld = Bukkit.getWorlds().getFirst();
        pluginFolder = getDataFolder();
        // ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        Iter.logger.info("[Iter] Registering event listeners");
        SetupManager.registerListeners();

        Iter.logger.info("[Iter] Registering commands");
        SetupManager.registerCommands();

        Iter.logger.info("[Iter] Loading maps");
        SetupManager.loadFolders(); // Load maps data

        Iter.logger.info("[Iter] Starting team service");
        Teaming.onEnable(); // Remove invites older than 30 seconds and return players to the team lists


    }

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.getLast());

        }
    }


    public static Iter plugin;
    public static Logger logger;
    public static World overworld;
    public static File pluginFolder;
}
