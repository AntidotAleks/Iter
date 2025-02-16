package me.antidotaleks.iter;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class Iter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register custom events

        @SuppressWarnings("unused")
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        plugin = this;
        pluginFolder = getDataFolder();

        SetupManager.loadFolders(); // Load maps data
        Teaming.startRemovingOldInvites(); // Remove invites older than 30 seconds


        SetupManager.registerCommands();
    }
    public static final World defaultWorld = Bukkit.getWorlds().getFirst();
    public static File pluginFolder;

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.getFirst());

        }
    }


    public static Logger logger = Logger.getLogger("Iter");
    public static Iter plugin;
}
