package me.kermx.seedBags;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

public class SeedBagListener implements Listener {
    private final Plugin plugin;

    public SeedBagListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // Handle item pickup events
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        ItemStack pickedUpItem = event.getItem().getItemStack();
        Material material = pickedUpItem.getType();

        if (isPlantableItem(material)) {
            // Check for corresponding seed bag
            ItemStack seedBag = getSeedBagFromInventory(player.getInventory(), material);
            if (seedBag != null) {
                event.setCancelled(true);
                event.getItem().remove();
                addSeedsToBag(seedBag, pickedUpItem.getAmount());
            }
        }
    }

    // Handle player interactions for planting
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isSeedBag(item, null)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isSuitableForPlanting(clickedBlock.getType())) {
            return;
        }

        event.setCancelled(true);
        plantSeeds(event.getPlayer(), clickedBlock, item);
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

    private ItemStack getSeedBagFromInventory(PlayerInventory inventory, Material seedType) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && isSeedBag(item, seedType)) {
                return item;
            }
        }
        return null;
    }

    private boolean isSeedBag(ItemStack item, Material seedType) {
        if (item.getType() != Material.PAPER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "seed_type");
        if (!data.has(key, PersistentDataType.STRING)) {
            return false;
        }
        String storedSeedType = data.get(key, PersistentDataType.STRING);
        return seedType == null || storedSeedType.equals(seedType.toString());
    }

    private void addSeedsToBag(ItemStack seedBag, int amount) {
        ItemMeta meta = seedBag.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey countKey = new NamespacedKey(plugin, "seed_count");
        int currentCount = data.getOrDefault(countKey, PersistentDataType.INTEGER, 0);
        currentCount += amount;
        data.set(countKey, PersistentDataType.INTEGER, currentCount);
        seedBag.setItemMeta(meta);
        meta.setDisplayName(getSeedBagDisplayName(seedBag));
        seedBag.setItemMeta(meta);
    }

    private String getSeedBagDisplayName(ItemStack seedBag) {
        ItemMeta meta = seedBag.getItemMeta();
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

    private boolean isSuitableForPlanting(Material material) {
        return material == Material.FARMLAND || material == Material.SOUL_SAND;
    }

    private void plantSeeds(Player player, Block clickedBlock, ItemStack seedBag) {
        ItemMeta meta = seedBag.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey seedTypeKey = new NamespacedKey(plugin, "seed_type");
        NamespacedKey countKey = new NamespacedKey(plugin, "seed_count");
        String seedTypeString = data.get(seedTypeKey, PersistentDataType.STRING);
        int seedCount = data.getOrDefault(countKey, PersistentDataType.INTEGER, 0);

        if (seedCount <= 0) {
            player.sendMessage("Your seed bag is empty!");
            return;
        }

        Material seedMaterial = Material.valueOf(seedTypeString);
        int radius = 1; // 3x3 area
        int seedsPlanted = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block block = clickedBlock.getRelative(dx, 0, dz);
                if (canPlantOn(block, seedMaterial)) {
                    Block blockAbove = block.getRelative(0, 1, 0);
                    if (blockAbove.getType() == Material.AIR) {
                        blockAbove.setType(getCropBlock(seedMaterial));
                        if (blockAbove.getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) blockAbove.getBlockData();
                            ageable.setAge(0);
                            blockAbove.setBlockData(ageable);
                        }
                        seedsPlanted++;
                        seedCount--;
                        if (seedCount <= 0) {
                            break;
                        }
                    }
                }
            }
            if (seedCount <= 0) {
                break;
            }
        }

        if (seedsPlanted > 0) {
            data.set(countKey, PersistentDataType.INTEGER, seedCount);
            meta.setDisplayName(getSeedBagDisplayName(seedBag));
            seedBag.setItemMeta(meta);
        } else {
            player.sendMessage("No suitable place to plant seeds!");
        }
    }

    private boolean canPlantOn(Block block, Material seedMaterial) {
        Material blockType = block.getType();
        if (seedMaterial == Material.NETHER_WART) {
            return blockType == Material.SOUL_SAND;
        } else {
            return blockType == Material.FARMLAND;
        }
    }

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
