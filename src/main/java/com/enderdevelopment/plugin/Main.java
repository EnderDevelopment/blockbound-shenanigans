package com.enderdevelopment.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        combatManager = new CombatManager(this);
        Bukkit.getPluginManager().registerEvents(combatManager, this);
        ArenaCommand arenaCommand = new ArenaCommand(combatManager);
        if (getCommand("arena") != null) {
            getCommand("arena").setExecutor(arenaCommand);
            getCommand("arena").setTabCompleter(arenaCommand);
        }
        getLogger().info("ArenaClash enabled with original arena combat systems.");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.shutdown();
        }
    }
}
