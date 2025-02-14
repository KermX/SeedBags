package me.kermx.seedBags;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeedBagCommandExecutor implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final List<Material> plantableSeeds = Arrays.asList(
            Material.WHEAT_SEEDS,
            Material.BEETROOT_SEEDS,
            Material.CARROT,
            Material.POTATO,
            Material.NETHER_WART,
            Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS
    );
    private final Map<String, Material> seedSynonyms = new HashMap<>();

    public SeedBagCommandExecutor(Plugin plugin) {
        this.plugin = plugin;
        initializeSeedSynonyms();
    }

    private void initializeSeedSynonyms() {
        seedSynonyms.put("wheat", Material.WHEAT_SEEDS);
        seedSynonyms.put("beetroot", Material.BEETROOT_SEEDS);
        seedSynonyms.put("carrot", Material.CARROT);
        seedSynonyms.put("potato", Material.POTATO);
        seedSynonyms.put("nether_wart", Material.NETHER_WART);
        seedSynonyms.put("melon", Material.MELON_SEEDS);
        seedSynonyms.put("pumpkin", Material.PUMPKIN_SEEDS);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /getseedbag <seedtype>");
            return false;
        }

        String seedTypeString = args[0].toLowerCase();
        Material seedMaterial = seedSynonyms.get(seedTypeString);

        if (seedMaterial == null) {
            player.sendMessage("Invalid seed type.");
            return false;
        }

        if (!isPlantableItem(seedMaterial)) {
            player.sendMessage("Invalid seed type.");
            return false;
        }

        ItemStack seedBag = createSeedBag(seedMaterial);
        player.getInventory().addItem(seedBag);
        player.sendMessage("You have received a seed bag for " + seedTypeString.replace('_', ' ') + ".");
        return true;
    }

    private boolean isPlantableItem(Material material) {
        return plantableSeeds.contains(material);
    }

    private ItemStack createSeedBag(Material seedMaterial) {
        ItemStack seedBag = new ItemStack(Material.PAPER);
        ItemMeta meta = seedBag.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        NamespacedKey seedTypeKey = new NamespacedKey(plugin, "seed_type");
        data.set(seedTypeKey, PersistentDataType.STRING, seedMaterial.toString());

        NamespacedKey countKey = new NamespacedKey(plugin, "seed_count");
        data.set(countKey, PersistentDataType.INTEGER, 0);

        meta.displayName(Component.text(getSeedBagDisplayName(meta)));
        meta.setMaxStackSize(1);
        seedBag.setItemMeta(meta);

        return seedBag;
    }

    private String getSeedBagDisplayName(ItemMeta meta) {
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey seedTypeKey = new NamespacedKey(plugin, "seed_type");
        NamespacedKey countKey = new NamespacedKey(plugin, "seed_count");
        String seedType = data.get(seedTypeKey, PersistentDataType.STRING);
        int count = data.getOrDefault(countKey, PersistentDataType.INTEGER, 0);

        if (seedType == null) {
            seedType = "Unknown Seed";
        }

        String seedName = seedType.replace('_', ' ').toLowerCase();
        seedName = Character.toUpperCase(seedName.charAt(0)) + seedName.substring(1);

        return seedName + " Seed Bag [" + count + "]";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            for (String synonym : seedSynonyms.keySet()) {
                if (synonym.startsWith(input)) {
                    suggestions.add(synonym);
                }
            }
            return suggestions;
        }
        return null;
    }
}
