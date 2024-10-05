package org.bischofftv.logtoolBungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogtoolBungee extends Plugin implements Listener {

    private List<String> blacklist;
    private Configuration config;

    @Override
    public void onEnable() {
        // Lade die Konfiguration und die Blacklist
        loadConfig();

        // Registriere den Plugin-Nachrichtenkanal
        getProxy().registerChannel("logtool:channel");
        getProxy().getPluginManager().registerListener(this, this);
    }

    private void loadConfig() {
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            blacklist = config.getStringList("blacklist");
        } catch (IOException e) {
            getLogger().severe("Failed to load configuration!");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("logtool:channel")) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            String playerName = in.readUTF();
            String data = in.readUTF();

            logActivity(playerName, action, data);

            // Überprüfen auf verbotene Wörter in den Daten
            if (containsBlacklistedWord(data)) {
                executeBlacklistAction(playerName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean containsBlacklistedWord(String text) {
        for (String word : blacklist) {
            if (text.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void executeBlacklistAction(String playerName) {
        String command = config.getString("blacklist-action", "kick %player% Blacklisted word detected").replace("%player%", playerName);
        getLogger().info("Executing command: " + command);
        ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
    }

    private void logActivity(String playerName, String action, String data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        String fileName = playerName + "_" + timestamp + ".log";

        File logFile = new File(getDataFolder(), fileName);
        String logMessage = String.format("[%s] Player %s performed action: %s with data: %s",
                timestamp, playerName, action, data);

        getLogger().info(logMessage);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            getLogger().severe("Failed to write to log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}