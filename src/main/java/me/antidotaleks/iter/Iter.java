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

        // ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        plugin = this;
        pluginFolder = getDataFolder();

        Iter.logger.info("Iter: Registering commands");
        SetupManager.registerCommands();
        Iter.logger.info("Iter: Loading maps");
        SetupManager.loadFolders(); // Load maps data
        Iter.logger.info("Iter: Starting loops");
        Teaming.startRemovingOldInvites(); // Remove invites older than 30 seconds


    }
    public static final World defaultWorld = Bukkit.getWorlds().getFirst();
    public static File pluginFolder;

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.getLast());

        }
    }


    public static Logger logger = Logger.getLogger("Iter");
    public static Iter plugin;
}
