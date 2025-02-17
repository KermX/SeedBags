package me.kermx.seedBags;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class SeedBagUtil {

    public static void updateSeedBagMeta(ItemStack seedBag) {
        if (seedBag == null) {
            return;
        }
        ItemMeta meta = seedBag.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String seedType = data.get(SeedBags.SEED_TYPE_KEY, PersistentDataType.STRING);
        int seedCount = data.getOrDefault(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, 0);

        if (seedType == null) {
            seedType = "Unknown Seed";
        } else {
            seedType = seedType.replace('_', ' ').toLowerCase();
            seedType = Character.toUpperCase(seedType.charAt(0)) + seedType.substring(1);
        }

        String displayName = seedType + " Seed Bag [" + seedCount + "]";
        meta.displayName(Component.text(displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Seeds: " + seedCount));
        meta.lore(lore);

        seedBag.setItemMeta(meta);
    }

    public static boolean isSeedBag(ItemStack item, Plugin plugin, Material seedType) {
        if (item.getType() != Material.PAPER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (!meta.getPersistentDataContainer().has(SeedBags.SEED_TYPE_KEY, PersistentDataType.STRING)) {
            return false;
        }
        String storedSeedType = meta.getPersistentDataContainer().get(SeedBags.SEED_TYPE_KEY, PersistentDataType.STRING);
        return seedType == null || storedSeedType.equals(seedType.toString());
    }
}
