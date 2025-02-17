package me.antidotaleks.iter;

import me.antidotaleks.iter.commands.StartGameCommand;
import me.antidotaleks.iter.commands.TeamCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SetupManager {
    private static final HashMap<String, TabExecutor> commands = new HashMap<>(){{
        put("game", new StartGameCommand());
        put("gteam", new TeamCommand());
    }};

    public static void registerCommands() {
        for (Map.Entry<String, TabExecutor> pair : commands.entrySet()) {
            Iter.logger.info("> "+pair.getKey());
            PluginCommand command = Iter.plugin.getCommand(pair.getKey());

            command.setExecutor(pair.getValue());
            command.setTabCompleter(pair.getValue());
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
        File[] mapFiles = mapsFolder.listFiles((dir, name) -> name.endsWith(".yaml"));
        for (File mapFile : mapFiles) {
            String mapName = mapFile.getName().replace(".yaml", "");
            try {
                Iter.logger.info("Loading map "+mapName);
                me.antidotaleks.iter.maps.Map current = new me.antidotaleks.iter.maps.Map(mapFile);
                GameManager.maps.add(current);
            } catch (Exception e) {
                Iter.logger.warning("Failed to load map "+mapName+": "+e.getMessage());
            }

        }
    }
}
