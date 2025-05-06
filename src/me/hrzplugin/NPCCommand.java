package me.hrzplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class NPCCommand implements CommandExecutor {
    private final WordJsonHandler wordHandler;
    public NPCCommand(Main plugin) { // Pass your plugin here
        this.wordHandler = new WordJsonHandler(plugin); // Correct
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("npcbucket.spawn")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        // Data Saving and Loading
        wordHandler.appendWord("NewWord_" + System.currentTimeMillis());

        // Spawn NPC
        Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
        npc.setCustomName(ChatColor.AQUA + "Custom NPC");
        npc.setCustomNameVisible(true);

        player.sendMessage(ChatColor.GREEN + "You spawned an NPC!");
        return true;
    }
}
