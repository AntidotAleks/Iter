package me.antidotaleks.iter;

import me.antidotaleks.iter.commands.QueueCommand;
import me.antidotaleks.iter.commands.TeamCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SetupManager {

    // Event Listeners

    private static final ArrayList<Listener> listeners = new ArrayList<>(){{
        add(new GameManager());
        add(new Teaming());
    }};

    public static void registerListeners() {
        for (Listener listener : listeners)
        Bukkit.getPluginManager().registerEvents(listener, Iter.plugin);
    }

    // Commands

    private static final HashMap<String, TabExecutor> commands = new HashMap<>(){{
        put("iqueue", new QueueCommand());
        put("iteam", new TeamCommand());
    }};

    public static void registerCommands() {
        for (Map.Entry<String, TabExecutor> pair : commands.entrySet()) {
            Iter.logger.info("       - "+pair.getKey());
            PluginCommand command = Iter.plugin.getCommand(pair.getKey());

            command.setExecutor(pair.getValue());
            command.setTabCompleter(pair.getValue());
        }
    }

    // Maps

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
                Iter.logger.info("       - "+mapName);
                me.antidotaleks.iter.maps.Map current = new me.antidotaleks.iter.maps.Map(mapFile);
                GameManager.maps.add(current);
                GameManager.mapNames.add(mapName);
            } catch (Exception e) {
                Iter.logger.warning("Failed to load map "+mapName+": "+e.getMessage() +"\n"+
                        Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n")));

            }
        }
    }
}
