package me.hrzplugin;

import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
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
    private static final NamespacedKey NPC_KEY = new NamespacedKey("npcbucket", "stored_npc");
    private static final NamespacedKey PROFESSION_KEY = new NamespacedKey("npcbucket", "profession");
    private static final NamespacedKey LEVEL_KEY = new NamespacedKey("npcbucket", "level");
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("npcbucket", "type");

    public NPCBucketHandler(JavaPlugin plugin) {
        this.plugin = plugin;
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

            // Get Villager Data
            Villager.Profession profession = npc.getProfession();
            int level = npc.getVillagerLevel();
            Villager.Type type = npc.getVillagerType();

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
}
