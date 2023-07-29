package com.triassic.randomcart.listeners;

import com.triassic.randomcart.RandomCart;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class BlockBreakListener implements Listener {
    private final Logger logger;
    private final Random random;
    private final Configuration config;
    private final List<Material> allowedBlocks;
    private final int summonChance;

    public BlockBreakListener(Configuration config, Logger logger, Random random) {
        this.config = config;
        this.logger = logger;
        this.random = random;
        this.allowedBlocks = RandomCart.loadAllowedBlocks(config, logger);
        this.summonChance = config.getInt("randomcart.chance");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!allowedBlocks.contains(block.getType()) || event.isCancelled()) {
            return;
        }

        if (random.nextInt(100) > summonChance) {
            return;
        }

        RandomCart.Minecart minecartData = RandomCart.loadMinecart(config, random);

        if (minecartData == null) {
            return;
        }

        String selectedMinecart = minecartData.getSelectedMinecart();

        Location blockLocation = block.getLocation();
        blockLocation.setY(blockLocation.getY() + 0.75);

        StorageMinecart chestMinecart = (StorageMinecart) block.getWorld().spawnEntity(blockLocation, EntityType.MINECART_CHEST);
        Inventory chestInventory = chestMinecart.getInventory();

        String chestName = ChatColor.translateAlternateColorCodes('&', minecartData.getName());
        chestMinecart.setCustomName(chestName);

        for (String itemSlot : minecartData.getItems()) {
            String materialName = config.getString("minecarts." + selectedMinecart + ".items." + itemSlot + ".material");

            Material material = Material.getMaterial(materialName);
            if (material == null) {
                logger.warning("Invalid item name " + materialName + " in " + selectedMinecart);
                continue;
            }

            String displayName = config.getString("minecarts." + selectedMinecart + ".items." + itemSlot + ".display-name");
            int stackSize = config.getInt("minecarts." + selectedMinecart + ".items." + itemSlot + ".amount");
            List<String> lore = config.getStringList("minecarts." + selectedMinecart + ".items." + itemSlot + ".lore");
            ConfigurationSection enchantmentsSection = config.getConfigurationSection("minecarts." + selectedMinecart + ".items." + itemSlot + ".enchantments");

            if (stackSize == 0) {
                stackSize = 1;
            }

            ItemStack item = new ItemStack(material, stackSize);
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                meta.setLore(coloredLore);
            }

            if (enchantmentsSection != null) {
                for (String ench : enchantmentsSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(ench.toLowerCase()));
                    int level = enchantmentsSection.getInt(ench + ".level");
                    try {
                        meta.addEnchant(enchantment, level, true);
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid enchantment name " + ench + " in " + selectedMinecart);
                    }
                }
            }

            item.setItemMeta(meta);
            chestInventory.setItem(Integer.parseInt(itemSlot), item);
        }
    }
}