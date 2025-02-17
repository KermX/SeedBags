package me.kermx.seedBags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class SeedBagUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

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
            seedType = normalizeMaterialName(seedType);
        }

        String displayName = "<white><italic:false>Seed Bag - " + seedType + " (" + seedCount + ")";
        meta.displayName(MINI_MESSAGE.deserialize(displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(MINI_MESSAGE.deserialize("<gray><italic:false>Plants crops in a 5x5 area."));
        lore.add(Component.text(" "));
        lore.add(MINI_MESSAGE.deserialize("<gray><italic:false>" + seedCount + "/128,000 "));
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

    public static String normalizeMaterialName(String materialName) {
        // Replace underscores with spaces and capitalize each word
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder normalized = new StringBuilder();
        for (String word : words) {
            normalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return normalized.toString().trim();
    }
}
