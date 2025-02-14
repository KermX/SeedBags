package me.kermx.seedBags;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class SeedBagCommandExecutor implements CommandExecutor {
    private final Plugin plugin;

    public SeedBagCommandExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /getseedbag <seedtype>");
            return false;
        }

        String seedTypeString = args[0].toUpperCase();
        Material seedMaterial;
        try {
            seedMaterial = Material.valueOf(seedTypeString);
        } catch (IllegalArgumentException e) {
            player.sendMessage("Invalid seed type.");
            return false;
        }

        if (!isPlantableItem(seedMaterial)) {
            player.sendMessage("Invalid seed type.");
            return false;
        }

        ItemStack seedBag = createSeedBag(seedMaterial);
        player.getInventory().addItem(seedBag);
        player.sendMessage("You have received a seed bag for " + seedTypeString.toLowerCase().replace('_', ' ') + ".");
        return true;
    }

    private boolean isPlantableItem(Material material) {
        switch (material) {
            case WHEAT_SEEDS:
            case BEETROOT_SEEDS:
            case CARROT:
            case POTATO:
            case NETHER_WART:
            case MELON_SEEDS:
            case PUMPKIN_SEEDS:
                return true;
            default:
                return false;
        }
    }

    private ItemStack createSeedBag(Material seedMaterial) {
        ItemStack seedBag = new ItemStack(Material.PAPER);
        ItemMeta meta = seedBag.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        NamespacedKey seedTypeKey = new NamespacedKey(plugin, "seed_type");
        data.set(seedTypeKey, PersistentDataType.STRING, seedMaterial.toString());

        NamespacedKey countKey = new NamespacedKey(plugin, "seed_count");
        data.set(countKey, PersistentDataType.INTEGER, 0);

        meta.setDisplayName(getSeedBagDisplayName(meta));
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
}
