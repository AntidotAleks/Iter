package me.antidotaleks.iter;

import me.antidotaleks.iter.commands.StartGameCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SetupManager {
    private static final HashMap<String, TabExecutor> commands = new HashMap<String, TabExecutor>(){{
        put("game", new StartGameCommand());
    }};
    public static void registerCommands() {
        for (Map.Entry<String, TabExecutor> pair : commands.entrySet()) {
            PluginCommand command = Iter.plugin.getCommand(pair.getKey());
            assert command != null;

            command.setExecutor(commands.get(pair.getValue()));
            command.setTabCompleter(commands.get(pair.getValue()));
        }
    }


    public static File mapsFolder;
    public static void loadFolders() {
        Iter.pluginFolder.mkdirs();
        String path = Iter.pluginFolder.getPath();
        char separator = File.separatorChar;

        // Open maps folder
        mapsFolder = new File(path+separator+"maps"); // Open maps folder
        mapsFolder.mkdirs();

        // Read maps data files
        File[] mapFiles = mapsFolder.listFiles((dir, name) -> name.endsWith(".mapdata"));
        for (File mapFile : mapFiles) {
            me.antidotaleks.iter.maps.Map current = new me.antidotaleks.iter.maps.Map(mapFile);

            GameManager.maps.add(current);
        }
    }
}
