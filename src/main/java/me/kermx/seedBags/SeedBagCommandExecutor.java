package me.kermx.seedBags;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
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

/**
 * Command executor and tab completer for the /getseedbag command.
 */
public class SeedBagCommandExecutor implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final List<Material> plantableSeeds = Arrays.asList(
            Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS,
            Material.CARROT, Material.POTATO, Material.NETHER_WART,
            Material.MELON_SEEDS, Material.PUMPKIN_SEEDS
    );
    private final Map<String, Material> seedSynonyms = new HashMap<>();

    /**
     * Constructor for SeedBagCommandExecutor.
     *
     * @param plugin The main plugin instance.
     */
    public SeedBagCommandExecutor(Plugin plugin) {
        this.plugin = plugin;
        initializeSeedSynonyms();
    }

    /**
     * Initialize seed type synonyms to allow for more flexible input.
     */
    private void initializeSeedSynonyms() {
        seedSynonyms.put("wheat", Material.WHEAT_SEEDS);
        seedSynonyms.put("beetroot", Material.BEETROOT_SEEDS);
        seedSynonyms.put("carrot", Material.CARROT);
        seedSynonyms.put("potato", Material.POTATO);
        seedSynonyms.put("nether_wart", Material.NETHER_WART);
        seedSynonyms.put("melon", Material.MELON_SEEDS);
        seedSynonyms.put("pumpkin", Material.PUMPKIN_SEEDS);
    }

    /**
     * Handle the /getseedbag command to give the player a seed bag.
     *
     * @param sender  The sender of the command.
     * @param command The command being executed.
     * @param label   The alias of the command used.
     * @param args    The arguments passed to the command.
     * @return True if the command was successful, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (!player.hasPermission("seedbags.getseedbag")) {
            return false;
        }

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

        if (!plantableSeeds.contains(seedMaterial)) {
            player.sendMessage("Invalid seed type.");
            return false;
        }

        ItemStack seedBag = createSeedBag(seedMaterial);
        player.getInventory().addItem(seedBag);
        player.sendMessage("You have received a seed bag for " + seedTypeString.replace('_', ' ') + ".");
        return true;
    }

    /**
     * Create a seed bag item with the specified seed material.
     *
     * @param seedMaterial The seed material for the seed bag.
     * @return The created seed bag item.
     */
    private ItemStack createSeedBag(Material seedMaterial) {
        ItemStack seedBag = new ItemStack(Material.PAPER);
        ItemMeta meta = seedBag.getItemMeta();
        if (meta == null) {
            return seedBag;
        }

        meta.getPersistentDataContainer().set(SeedBags.SEED_TYPE_KEY, PersistentDataType.STRING, seedMaterial.toString());
        meta.getPersistentDataContainer().set(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, 0);

        meta.setCustomModelData(getCustomModelDataForSeed(seedMaterial));
        meta.setMaxStackSize(1);
        seedBag.setItemMeta(meta);

        SeedBagUtil.updateSeedBagMeta(seedBag);

        return seedBag;
    }

    /**
     * Map each seed material to its unique custom model data value.
     *
     * @param seedMaterial The seed material.
     * @return The custom model data value.
     */
    private int getCustomModelDataForSeed(Material seedMaterial) {
        return switch (seedMaterial) {
            case WHEAT_SEEDS -> 900;
            case BEETROOT_SEEDS -> 904;
            case CARROT -> 903;
            case POTATO -> 901;
            case NETHER_WART -> 902;
            case MELON_SEEDS -> 899;
            case PUMPKIN_SEEDS -> 898;
            default -> 0;
        };
    }

    /**
     * Handle tab completion for the /getseedbag command to provide seed type suggestions.
     *
     * @param sender  The sender of the command.
     * @param command The command being executed.
     * @param label   The alias of the command used.
     * @param args    The arguments passed to the command.
     * @return A list of suggested seed types based on the input.
     */
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
