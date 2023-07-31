package com.triassic.randomcart;

import com.triassic.randomcart.listeners.BlockBreakListener;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public final class RandomCart extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        Logger logger = getLogger();
        Configuration config = getConfig();
        Random random = new Random();

        this.saveDefaultConfig();

        // Initialize bStats metrics
        final boolean metricsEnabled = config.getBoolean("toggle-metrics", true);

        if (metricsEnabled) {
            new Metrics(this, 19329);
        }

        getServer().getPluginManager().registerEvents(new BlockBreakListener(config, logger, random), this);
    }

    public static class Minecart {
        private final String name;
        private final List<String> items;
        private final String selectedMinecart;

        public Minecart(String name, List<String> items, String selectedMinecart) {
            this.name = name;
            this.items = items;
            this.selectedMinecart = selectedMinecart;
        }

        public String getName() {
            return name;
        }

        public List<String> getItems() {
            return items;
        }

        public String getSelectedMinecart() {
            return selectedMinecart;
        }
    }

    public static Minecart loadMinecart(Configuration config, Random random) {
        List<String> minecartKeys = null;

        try {
            minecartKeys = new ArrayList<>(config.getConfigurationSection("minecarts").getKeys(false));
        } catch (NullPointerException ignored) {}

        if (minecartKeys != null) {
            String selectedMinecart = minecartKeys.get(random.nextInt(minecartKeys.size()));

            String name = config.getString("minecarts." + selectedMinecart + ".name");

            ConfigurationSection itemsConfig = config.getConfigurationSection("minecarts." + selectedMinecart + ".items");
            List<String> items = new ArrayList<>(itemsConfig.getKeys(false));

            return new Minecart(name, items, selectedMinecart);
        }

        return null;
    }

    public static List<Material> loadAllowedBlocks(Configuration config, Logger logger) {
        List<String> allowedBlockNames = config.getStringList("randomcart.allowed-blocks");
        List<Material> allowedBlocks = new ArrayList<>();

        for (String blockName : allowedBlockNames) {
            Material blockMaterial = Material.getMaterial(blockName);
            if (blockMaterial == null) {
                logger.warning("Invalid block name: " + blockName + " in allowed blocks list");
                continue;
            }

            allowedBlocks.add(blockMaterial);
        }

        return allowedBlocks;
    }
}
