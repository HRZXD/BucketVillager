package me.hrzplugin;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class NPCJsonHandler {
    private final File file;
    private final Gson gson = new Gson();

    public NPCJsonHandler(JavaPlugin plugin) {
        File pluginFolder = plugin.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        this.file = new File(pluginFolder, "npcs.json");
    }


    public void appendNPC(NPCData newNPC) {
        NPCDataList list = loadNPCList();
        if (list == null) list = new NPCDataList();

        list.npc_data.add(newNPC);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NPCDataList loadNPCList() {
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, NPCDataList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

