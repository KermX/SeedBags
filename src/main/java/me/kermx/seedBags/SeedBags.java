package me.kermx.seedBags;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SeedBags extends JavaPlugin {
    private RoseStackerAPI rsAPI;

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("getseedbag")).setExecutor(new SeedBagCommandExecutor(this));

        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            this.rsAPI = RoseStackerAPI.getInstance();
        }

        getServer().getPluginManager().registerEvents(new SeedBagListener(this, this.rsAPI), this);
    }
}
