package br.com.devpaulo.legendchat.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import br.com.devpaulo.legendchat.api.Legendchat;
import br.com.devpaulo.legendchat.messages.MessageManager;

public class Updater {
    private static final Pattern CURSEFORGE_NAME_PATTERN = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final String version;
    private final Plugin plugin = Bukkit.getPluginManager().getPlugin("Legendchat");

    private boolean updConfig = false;
    private boolean updLang = false;

    public Updater(String v) {
        this.version = v == null ? "" : v;
    }

    public Updater() {
        this.version = "";
    }

    public String CheckNewVersion() throws Exception {
        String response;
        URLConnection conn = URI.create("https://api.curseforge.com/servermods/files?projectIds=74494").toURL().openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.addRequestProperty("User-Agent", "Legendchat (by PauloABR)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            response = reader.readLine();
        }

        String v = extractLastVersionName(response);
        if (v == null) {
            return null;
        }

        boolean updateAvailable = false;
        if (!version.equals(v)) {
            String[] vObtained = v.split("\\.");
            String[] vHere = version.split("\\.");

            boolean draw = true;
            for (int i = 0; i < Math.min(vObtained.length, vHere.length); i++) {
                int nObtained = Integer.parseInt(vObtained[i]);
                int nHere = Integer.parseInt(vHere[i]);

                if (nObtained > nHere) {
                    updateAvailable = true;
                    break;
                }
                if (nObtained < nHere) {
                    draw = false;
                    break;
                }
            }

            if (draw && vObtained.length > vHere.length) {
                updateAvailable = true;
            }
        }

        return updateAvailable ? v : null;
    }

    private String extractLastVersionName(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String latestName = null;
        Matcher matcher = CURSEFORGE_NAME_PATTERN.matcher(response);
        while (matcher.find()) {
            latestName = matcher.group(1);
        }

        if (latestName == null || !latestName.contains("(") || !latestName.contains(")")) {
            return null;
        }

        return latestName.split("\\(")[1].split("\\)")[0].replace("V", "");
    }

    public boolean updateConfig() {
        InputStream is = plugin.getResource("config_template.yml");
        if (is == null) {
            return false;
        }

        YamlConfiguration c = loadYaml(is);
        for (String n : c.getConfigurationSection("").getKeys(true)) {
            if (!has(n)) {
                set(n, c.get(n));
            }
        }

        if (updConfig) {
            plugin.saveConfig();
        }
        return updConfig;
    }

    private YamlConfiguration loadYaml(InputStream is) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private boolean has(String s) {
        return plugin.getConfig().contains(s);
    }

    private void set(String s, Object obj) {
        plugin.getConfig().set(s, obj);
        updConfig = true;
    }

    public boolean updateAndLoadLanguage(String language) {
        File f = new File(plugin.getDataFolder(), "language" + File.separator + "language_" + language + ".yml");
        MessageManager m = Legendchat.getMessageManager();
        m.registerLanguageFile(f);
        m.loadMessages(f);

        InputStream is = plugin.getResource(("language" + File.separator + "language_" + language + ".yml").replace('\\', '/'));
        if (is == null) {
            is = plugin.getResource(("language" + File.separator + "language_en.yml").replace('\\', '/'));
        }
        if (is == null) {
            return false;
        }

        YamlConfiguration c = loadYaml(is);
        for (String n : c.getConfigurationSection("").getKeys(false)) {
            if (!m.hasMessage(n)) {
                addMessage(m, n, c.getString(n));
            }
        }

        if (updLang) {
            m.loadMessages(f);
        }
        return updLang;
    }

    private void addMessage(MessageManager m, String name, String msg) {
        m.addMessageToFile(name, msg);
        updLang = true;
    }

    public boolean updateChannels() {
        boolean upd = false;
        File channelsDir = new File(plugin.getDataFolder(), "channels");
        File[] channels = channelsDir.listFiles();
        if (channels == null) {
            return false;
        }

        for (File channel : channels) {
            YamlConfiguration channel2 = YamlConfiguration.loadConfiguration(channel);
            if (!channel2.contains("needFocus")) {
                channel2.set("needFocus", false);
                upd = true;
            }
            try {
                channel2.save(channel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return upd;
    }
}
