package me.hrzplugin;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WordJsonHandler {

    private final File file;
    private final Gson gson = new Gson();

    public WordJsonHandler(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "words.json");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
    }

    public void appendWord(String newWord) {
        WordData data = loadAllWords(); // load existing words
        if (data == null) data = new WordData();

        data.words.add(newWord); // append new word

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WordData loadAllWords() {
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, WordData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
