package me.kermx.seedBags;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener class for handling item pickup and player interaction events
 * related to seed bags in the Minecraft plugin.
 */
public class SeedBagListener implements Listener {
    private final Plugin plugin;
    private final RoseStackerAPI rsAPI;

    /**
     * Constructor for SeedBagListener.
     *
     * @param plugin The main plugin instance.
     * @param rsAPI  The RoseStacker API instance.
     */
    public SeedBagListener(Plugin plugin, RoseStackerAPI rsAPI) {
        this.plugin = plugin;
        this.rsAPI = rsAPI;
    }

    /**
     * Handle item pickup events to collect seeds into seed bags.
     *
     * @param event The EntityPickupItemEvent.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        Material material = itemStack.getType();

        if (rsAPI != null) {
            ItemStack seedBag = getSeedBagFromInventory(player.getInventory(), material);

            if (seedBag != null) {
                if (!rsAPI.isItemStacked(item)) {
                    if (isPlantableSeed(material)) {
                        event.setCancelled(true);
                        item.remove();
                        addSeedsToBag(seedBag, itemStack.getAmount());
                    }
                } else {
                    StackedItem stackedItem = rsAPI.getStackedItem(item);
                    if (stackedItem != null) {
                        event.setCancelled(true);
                        item.remove();
                        rsAPI.removeItemStack(stackedItem);
                        addSeedsToBag(seedBag, stackedItem.getStackSize());
                    }
                }
            }
        }
    }

    /**
     * Handle player interactions for planting seeds from seed bags.
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || !SeedBagUtil.isSeedBag(itemInHand, plugin, null)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isSuitableForPlanting(clickedBlock.getType())) {
            return;
        }

        event.setCancelled(true);
        plantSeeds(event.getPlayer(), clickedBlock, itemInHand);
    }

    /**
     * Check if the given material is a plantable seed.
     *
     * @param material The material to check.
     * @return True if the material is a plantable seed, false otherwise.
     */
    private boolean isPlantableSeed(Material material) {
        return switch (material) {
            case WHEAT_SEEDS, BEETROOT_SEEDS, CARROT, POTATO, NETHER_WART, MELON_SEEDS, PUMPKIN_SEEDS -> true;
            default -> false;
        };
    }

    /**
     * Get the seed bag from the player's inventory that matches the given seed type.
     *
     * @param inventory The player's inventory.
     * @param seedType  The type of seed to match.
     * @return The matching seed bag, or null if no match is found.
     */
    private ItemStack getSeedBagFromInventory(PlayerInventory inventory, Material seedType) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && SeedBagUtil.isSeedBag(item, plugin, seedType)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Add the specified amount of seeds to the seed bag.
     *
     * @param seedBag The seed bag to add seeds to.
     * @param amount  The amount of seeds to add.
     */
    private void addSeedsToBag(ItemStack seedBag, int amount) {
        ItemMeta meta = seedBag.getItemMeta();
        if (meta == null) {
            return;
        }
        int currentSeedCount = meta.getPersistentDataContainer()
                .getOrDefault(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, 0);
        currentSeedCount += amount;
        meta.getPersistentDataContainer().set(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, currentSeedCount);
        seedBag.setItemMeta(meta);
        SeedBagUtil.updateSeedBagMeta(seedBag);
    }

    /**
     * Check if the given material is suitable for planting seeds.
     *
     * @param material The material to check.
     * @return True if the material is suitable for planting, false otherwise.
     */
    private boolean isSuitableForPlanting(Material material) {
        return material == Material.FARMLAND || material == Material.SOUL_SAND;
    }

    /**
     * Plant seeds from the seed bag at the specified block.
     *
     * @param player       The player planting the seeds.
     * @param clickedBlock The block to plant the seeds on.
     * @param seedBag      The seed bag containing the seeds.
     */
    private void plantSeeds(Player player, Block clickedBlock, ItemStack seedBag) {
        ItemMeta meta = seedBag.getItemMeta();
        if (meta == null) return;
        String seedTypeString = meta.getPersistentDataContainer().get(SeedBags.SEED_TYPE_KEY, PersistentDataType.STRING);
        int seedCount = meta.getPersistentDataContainer()
                .getOrDefault(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, 0);

        if (seedCount <= 0) {
            player.sendMessage("Your seed bag is empty!");
            return;
        }

        Material seedMaterial = Material.valueOf(seedTypeString);
        int radius = (seedTypeString.equals("PUMPKIN_SEEDS") || seedTypeString.equals("MELON_SEEDS")) ? 0 : 2;
        int seedsPlanted = 0;

        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block block = clickedBlock.getRelative(dx, 0, dz);
                if (canPlantOn(block, seedMaterial)) {
                    Block blockAbove = block.getRelative(0, 1, 0);
                    if (blockAbove.getType() == Material.AIR) {
                        blockAbove.setType(getCropBlock(seedMaterial));
                        if (blockAbove.getBlockData() instanceof Ageable ageable) {
                            ageable.setAge(0);
                            blockAbove.setBlockData(ageable);
                        }
                        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                                blockAbove, block.getState(), block, seedBag, player, true, null
                        );
                        Bukkit.getPluginManager().callEvent(placeEvent);
                        if (!placeEvent.isCancelled()) {
                            seedsPlanted++;
                            seedCount--;
                            if (seedCount <= 0) {
                                break outer;
                            }
                        } else {
                            blockAbove.setType(Material.AIR);
                        }
                    }
                }
            }
        }

        if (seedsPlanted > 0) {
            meta.getPersistentDataContainer().set(SeedBags.SEED_COUNT_KEY, PersistentDataType.INTEGER, seedCount);
            seedBag.setItemMeta(meta);
            SeedBagUtil.updateSeedBagMeta(seedBag);
        } else {
            player.sendMessage("No suitable place to plant seeds!");
        }
    }

    /**
     * Check if the given block is suitable for planting the specified seed.
     *
     * @param block        The block to check.
     * @param seedMaterial The seed material.
     * @return True if the block is suitable for planting, false otherwise.
     */
    private boolean canPlantOn(Block block, Material seedMaterial) {
        Material blockType = block.getType();
        if (seedMaterial == Material.NETHER_WART) {
            return blockType == Material.SOUL_SAND;
        } else {
            return blockType == Material.FARMLAND;
        }
    }

    /**
     * Get the crop block type corresponding to the seed material.
     *
     * @param seedMaterial The seed material.
     * @return The crop block type.
     */
    private Material getCropBlock(Material seedMaterial) {
        return switch (seedMaterial) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            case NETHER_WART -> Material.NETHER_WART;
            case MELON_SEEDS -> Material.MELON_STEM;
            case PUMPKIN_SEEDS -> Material.PUMPKIN_STEM;
            default -> Material.AIR;
        };
    }
}