package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ArenaCommand implements CommandExecutor, TabCompleter {
    private final CombatManager manager;

    public ArenaCommand(CombatManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Component.text("ArenaClash", NamedTextColor.GOLD));
            player.sendMessage(Component.text("/arena join", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/arena leave", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/arena kit <ember|tide|stone>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Left click: combo  |  Right click: block  |  Sneak: dash  |  Swap hands: ultimate", NamedTextColor.GRAY));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "join" -> manager.join(player);
            case "leave" -> manager.leave(player, true);
            case "kit" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Choose ember, tide, or stone.", NamedTextColor.RED));
                } else {
                    manager.selectKit(player, args[1]);
                }
            }
            default -> player.sendMessage(Component.text("Unknown option. Use /arena help.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return partial(args[0], List.of("join", "leave", "kit", "help"));
        if (args.length == 2 && args[0].equalsIgnoreCase("kit")) return partial(args[1], List.of("ember", "tide", "stone"));
        return List.of();
    }

    private List<String> partial(String input, List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.startsWith(input.toLowerCase())) result.add(value);
        return result;
    }
}
