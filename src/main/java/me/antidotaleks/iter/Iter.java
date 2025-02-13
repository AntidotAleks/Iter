package me.antidotaleks.iter;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;

public final class Iter extends JavaPlugin {

    @Override
    public void onEnable() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        plugin = this;
        pluginFolder = getDataFolder();

        SetupManager.loadFolders(); // Load maps data


        SetupManager.registerCommands();
    }
    public static final World defaultWorld = Bukkit.getWorlds().get(0);
    public static File pluginFolder;

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.get(0));
        }
    }


    public static Iter plugin;
}
