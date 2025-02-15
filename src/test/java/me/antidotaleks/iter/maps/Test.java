package me.antidotaleks.iter.maps;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;

public class Test {

    /**
     * Test
     */

    public static void main(String[] args) {
        //"3v3classic.yaml" in the same package as this class
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("C:\\Users\\Aleks\\Desktop\\3v3classic.yaml"));
        System.out.println(Arrays.toString(yaml.getString("map").replace(" ", "").split("\n")));
    }

}