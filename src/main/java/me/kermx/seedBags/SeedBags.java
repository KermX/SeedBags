package me.kermx.seedBags;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * The main class for the SeedBags plugin.
 * This class initializes the plugin, sets the command executor,
 * and registers the event listener.
 */
public final class SeedBags extends JavaPlugin {
    private RoseStackerAPI rsAPI;

    /**
     * Called when the plugin is enabled.
     * Sets the command executor for the /getseedbag command and
     * registers the event listener for seed bags.
     */
    @Override
    public void onEnable() {
        // Set the executor for the /getseedbag command
        Objects.requireNonNull(getCommand("getseedbag")).setExecutor(new SeedBagCommandExecutor(this));

        // Check if RoseStacker plugin is enabled and get its API instance
        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            this.rsAPI = RoseStackerAPI.getInstance();
        }

        // Register the event listener for seed bags
        getServer().getPluginManager().registerEvents(new SeedBagListener(this, this.rsAPI), this);
    }
}
