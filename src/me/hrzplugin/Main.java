package me.hrzplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("✅ NPC Bucket Plugin Enabled!");

        // Register event listeners
        getServer().getPluginManager().registerEvents(new NPCBucketHandler(this), this);

        // Register commands
        if (getCommand("spawnnpc") != null) {
            this.getCommand("spawnnpc").setExecutor(new NPCCommand());
        } else {
            getLogger().warning("⚠ Command 'spawnnpc' is not defined in plugin.yml!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("❌ NPC Bucket Plugin Disabled!");
    }
}
