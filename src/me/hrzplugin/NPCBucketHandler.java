package me.hrzplugin;

import com.google.gson.Gson;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.util.RayTraceResult;

public class NPCBucketHandler implements Listener {
    private final JavaPlugin plugin;
    private final NPCJsonHandler nPCJsonHandler;
    private static final NamespacedKey NPC_KEY = new NamespacedKey("npcbucket", "stored_npc");
    private static final NamespacedKey PROFESSION_KEY = new NamespacedKey("npcbucket", "profession");
    private static final NamespacedKey LEVEL_KEY = new NamespacedKey("npcbucket", "level");
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("npcbucket", "type");

    public NPCBucketHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.nPCJsonHandler = new NPCJsonHandler(plugin);
    }


    @EventHandler
    public void onRightClickNPC(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.BUCKET) {
            event.setCancelled(true);

            Villager npc = (Villager) event.getRightClicked();
            String npcName = (npc.getCustomName() != null) ? npc.getCustomName() : "Captured NPC";
            //New Data NPC
            UUID villagerUUID = npc.getUniqueId();
            Villager.Profession profession = npc.getProfession();
            int level = npc.getVillagerLevel();
            Villager.Type type = npc.getVillagerType();
            double health = npc.getHealth();
            boolean isAdult = npc.isAdult();
            boolean isSleeping = npc.isSleeping();
            // 🟢 Trade data stored in structured list
            List<TradeData> tradeList = new ArrayList<>();
            for (MerchantRecipe recipe : npc.getRecipes()) {
                List<ItemStack> ingredients = recipe.getIngredients();
                ItemStack result = recipe.getResult();
                int maxUses = recipe.getMaxUses();
                int uses = recipe.getUses();
                int xp = recipe.getVillagerExperience();

                // Check if result is an enchanted book
                if (result.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) result.getItemMeta();
                    if (bookMeta != null) {
                        player.sendMessage("  - Result: Enchanted Book");
                        for (Map.Entry<Enchantment, Integer> enchantment : bookMeta.getStoredEnchants().entrySet()) {
                            player.sendMessage("    - Enchantment: " + enchantment.getKey().getKey() + " Level: " + enchantment.getValue());
                        }
                    }
                } else {
                    player.sendMessage("  - Result: " + result.getType());
                }

                // Store trade data in list (same as before)
                tradeList.add(new TradeData(ingredients, result, maxUses, uses, xp));
            }
            // 🟢 Output to player (or you could save to file / database)
            player.sendMessage("§a--- Villager Info ---");
            player.sendMessage("UUID: " + villagerUUID);
            player.sendMessage("Profession: " + profession);
            player.sendMessage("Level: " + level);
            player.sendMessage("Type (Biome): " + type);
            player.sendMessage("Health: " + health);
            player.sendMessage("Age: " + (isAdult ? "Adult" : "Child"));
            player.sendMessage("Sleeping: " + isSleeping);

            int tradeNum = 1;
            for (TradeData trade : tradeList) {
                player.sendMessage("Trade " + tradeNum++ + ":");
                player.sendMessage("  - Result: " + trade.result.getType());
                player.sendMessage("  - Max Uses: " + trade.maxUses);
                player.sendMessage("  - Experience: " + trade.experience);
                player.sendMessage("  - Uses: " + trade.uses);
                player.sendMessage("  - Ingredients:");
                for (ItemStack ing : trade.ingredients) {
                    player.sendMessage("    - " + ing.getType() + " x" + ing.getAmount());
                }
            }
            // Get Villager Data
//            Villager.Profession profession = npc.getProfession();
//            int level = npc.getVillagerLevel();
//            Villager.Type type = npc.getVillagerType();
//            nPCJsonHandler.appendNPC(new NPCData(profession.toString(), level, type.toString()));
            // Remove the NPC
            npc.remove();

            // Create a "Captured NPC" bucket
            ItemStack npcBucket = new ItemStack(Material.WATER_BUCKET);
            ItemMeta meta = npcBucket.getItemMeta();
            if (meta == null) return;

            meta.setDisplayName(ChatColor.GOLD + npcName);

            // Store NPC data inside the bucket
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(NPC_KEY, PersistentDataType.STRING, npcName);
            data.set(PROFESSION_KEY, PersistentDataType.STRING, profession.name());
            data.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
            data.set(TYPE_KEY, PersistentDataType.STRING, type.name());

            npcBucket.setItemMeta(meta);

            // Set the item in player's hand
            player.getInventory().setItemInMainHand(npcBucket);
            player.sendMessage(ChatColor.GREEN + "You captured an NPC in a bucket!");
        }
    }

    @EventHandler
    public void onRightClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Perform ray tracing
        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), 5);
        if (result == null || result.getHitPosition() == null) return;

        Location spawnedLocation = result.getHitPosition().toLocation(player.getWorld());

        if (item.getType() == Material.WATER_BUCKET && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            PersistentDataContainer data = meta.getPersistentDataContainer();
            if (data.has(NPC_KEY, PersistentDataType.STRING)) {
                String npcName = data.getOrDefault(NPC_KEY, PersistentDataType.STRING, "Unnamed NPC");
                String professionString = data.get(PROFESSION_KEY, PersistentDataType.STRING);
                Integer level = data.get(LEVEL_KEY, PersistentDataType.INTEGER);
                String typeString = data.get(TYPE_KEY, PersistentDataType.STRING);

                if (professionString == null || level == null || typeString == null) {
                    player.sendMessage(ChatColor.RED + "Error: Invalid NPC data in bucket!");
                    return;
                }

                Villager.Profession profession = Villager.Profession.valueOf(professionString);
                Villager.Type type = Villager.Type.valueOf(typeString);

                // Spawn the NPC
                Villager npc = player.getWorld().spawn(spawnedLocation, Villager.class);
                npc.addScoreboardTag("npcbucket_spawned");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        applyVillagerData(npc, npcName, profession, level, type, player);
                    }
                }.runTaskLater(plugin, 10L);

                // Replace the bucket with an empty one
                player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                player.sendMessage(ChatColor.YELLOW + "You released " + ChatColor.GOLD + npcName + ChatColor.YELLOW + "!");
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;

        Villager npc = (Villager) event.getEntity();
        if (npc.getScoreboardTags().contains("npcbucket_spawned")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> npc.setCustomNameVisible(true), 10L);
        }
    }

    private void applyVillagerData(Villager villager, String name, Villager.Profession profession, int level, Villager.Type type, Player player) {
        villager.setAI(false); // Temporarily disable AI to prevent reset
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setPersistent(true);

        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (counter >= 100) { // Keep applying data for 5 seconds
                    villager.setAI(true);
                    villager.setSilent(false);
                    cancel();
                    return;
                }

                villager.setCustomName(name);
                villager.setCustomNameVisible(true);
                villager.setVillagerType(type);
                villager.setProfession(profession);
                villager.setVillagerLevel(level);
                counter++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Apply settings every tick for 5 seconds
    }
    // 🔹 Simple structure to store trade data
    static class TradeData {
        List<ItemStack> ingredients;
        ItemStack result;
        int maxUses;
        int uses;
        int experience;

        TradeData(List<ItemStack> ingredients, ItemStack result, int maxUses, int uses, int experience) {
            this.ingredients = ingredients;
            this.result = result;
            this.maxUses = maxUses;
            this.uses = uses;
            this.experience = experience;
        }
    }
}
