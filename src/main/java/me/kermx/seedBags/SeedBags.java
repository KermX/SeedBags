package me.kermx.seedBags;

import org.bukkit.plugin.java.JavaPlugin;

public final class SeedBags extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SeedBagListener(this), this);
        getCommand("getseedbag").setExecutor(new SeedBagCommandExecutor(this));
    }

    @Override
    public void onDisable() {
    }
}
