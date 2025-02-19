package net.coalcube.bansystem.spigot;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.listener.AsyncPlayerChatListener;
import net.coalcube.bansystem.spigot.listener.PlayerCommandPreprocessListener;
import net.coalcube.bansystem.spigot.listener.PlayerConnectionListener;
import net.coalcube.bansystem.spigot.util.SpigotConfig;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class BanSystemSpigot extends JavaPlugin implements BanSystem {

    private static Plugin instance;
    private static BanManager banManager;
    private static IDManager idManager;
    private static URLUtil urlUtil;

    private Database sql;
    private MySQL mysql;
    private ServerSocket serversocket;
    private TimeFormatUtil timeFormatUtil;
    private Config config, messages, blacklist;
    private static String Banscreen;
    private static List<String> blockedCommands, ads, blockedWords;
    private File sqlitedatabase;
    private String hostname, database, user, pw;
    private int port;
    private CommandSender console;
    public static String prefix = "§8§l┃ §cBanSystem §8» §7";

    @Override
    public void onEnable() {
        super.onEnable();

        BanSystem.setInstance(this);

        instance = this;
        PluginManager pluginmanager = Bukkit.getPluginManager();
        console = Bukkit.getConsoleSender();
        UpdateChecker updatechecker = new UpdateChecker(65863);

        console.sendMessage("§c  ____                    ____                  _                      ");
        console.sendMessage("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  ");
        console.sendMessage("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ ");
        console.sendMessage("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |");
        console.sendMessage("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|");
        console.sendMessage("§c                                  |___/                           §7v" + this.getVersion());

        createConfig();
        loadConfig();

        timeFormatUtil = new TimeFormatUtil(messages);

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            sql = mysql;
            banManager = new BanManagerMySQL(mysql);
            try {
                mysql.connect();
                console.sendMessage(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                console.sendMessage(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                console.sendMessage(prefix + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
                console.sendMessage(prefix + "§cDebug Message: §e" + e.getMessage());
            }
            try {
                if(mysql.isConnected()) {
                    if(mysql.isOldDatabase()) {
                        mysql.importFromOldBanDatabase();
                        mysql.importFromOldBanHistoriesDatabase();
                        console.sendMessage(prefix + "§7Die MySQL Daten vom dem alten BanSystem wurden §2importiert§7.");
                    }
                    mysql.createTables(config);
                    console.sendMessage(prefix + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException | UnknownHostException | ParseException | ExecutionException | InterruptedException e) {
                console.sendMessage(prefix + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    console.sendMessage(prefix + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                console.sendMessage(prefix + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                e.printStackTrace();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        } else {
            createFileDatabase();
            SQLite sqlite = new SQLite(sqlitedatabase);
            banManager = new BanManagerSQLite(sqlite);
            sql = sqlite;
            try {
                sqlite.connect();
                console.sendMessage(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                console.sendMessage(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                console.sendMessage(prefix + "§cBitte überprüfe die eingetragenen SQlite daten in der Config.yml.");
                e.printStackTrace();
            }
            try {
                if(sqlite.isConnected()) {
                    sqlite.createTables(config);
                    console.sendMessage(prefix + "§7Die SQLite Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                console.sendMessage(prefix + "§7Die SQLite Tabellen §ckonnten nicht §7erstellt werden.");
                console.sendMessage(prefix + e.getMessage() + " " + e.getCause());
            }
        }



        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, UUIDFetcher::clearCache, 72000, 72000);

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.enable"))
            console.sendMessage(
                    prefix + "§cBitte trage die IP des Servers in der config.yml ein.");


        console.sendMessage(prefix + "§7Das BanSystem wurde gestartet.");

        try {
            if (updatechecker.checkForUpdates()) {
                console.sendMessage(prefix + "§cEin neues Update ist verfügbar.");
                console.sendMessage(prefix + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        new Thread(() -> {
//            try {
//                serversocket = new ServerSocket(6000);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();


        idManager = new IDManager(config, sql, new File(this.getDataFolder(), "config.yml"));
        urlUtil = new URLUtil(messages, config);

        init(pluginmanager);

    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            if (sql.isConnected())
                sql.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        AsyncPlayerChatEvent.getHandlerList().unregister(instance);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
        PlayerQuitEvent.getHandlerList().unregister(instance);
        PlayerJoinEvent.getHandlerList().unregister(instance);
        PlayerPreLoginEvent.getHandlerList().unregister(instance);

        console.sendMessage(BanSystemSpigot.prefix + "§7Das BanSystem wurde gestoppt.");

    }


    // create Config files
    private void createConfig() {
        try {
            File configfile = new File(this.getDataFolder(), "config.yml");
            if(!this.getDataFolder().exists()) {
                this.getDataFolder().mkdir();
            }
            if(!configfile.exists()) {
                configfile.createNewFile();
                config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
                ConfigurationUtil.initConfig(config);
                config.save(configfile);
            }
            File messagesfile = new File(this.getDataFolder(), "messages.yml");
            if(!messagesfile.exists()) {
                messagesfile.createNewFile();
                messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
                ConfigurationUtil.initMessages(messages);
                messages.save(messagesfile);
            }
            File blacklistfile = new File(this.getDataFolder(), "blacklist.yml");
            if (!blacklistfile.exists()) {
                blacklistfile.createNewFile();
                blacklist = new SpigotConfig(YamlConfiguration.loadConfiguration(blacklistfile));
                ConfigurationUtil.initBlacklist(blacklist);
                blacklist.save(blacklistfile);
            }
            messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
            config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
            blacklist = new SpigotConfig(YamlConfiguration.loadConfiguration(blacklistfile));
//
//            if (messages.getString("bansystem.help.header") == null) {
//                messages.set("bansystem.help", "");
//                messages.set("bansystem.help", null);
//
//                messages.set("bansystem.help.header", "§8§m--------§8[ §cBanSystem §8]§m--------");
//                messages.set("bansystem.help.entry", "§e/%command% §8» §7%description%");
//                messages.set("bansystem.help.footer", "§8§m-----------------------------");
//
//                messages.save(messagesfile);
//            }
        } catch (IOException e) {
            System.err.println("[Bansystem] Dateien konnten nicht erstellt werden.");
        }
    }

    private void createFileDatabase() {
        try {
            sqlitedatabase = new File(this.getDataFolder(), "database.db");

            if (!sqlitedatabase.exists()) {
                sqlitedatabase.createNewFile();
            }
        } catch (IOException e) {
            console.sendMessage(prefix + "Die SQLite datenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            prefix = messages.getString("prefix").replaceAll("&", "§");

            Banscreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (Banscreen == null) {
                    Banscreen = screen.replaceAll("%P%", prefix) + "\n";
                } else
                    Banscreen = Banscreen + screen.replaceAll("%P%", prefix) + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");

            ads = new ArrayList<>();
            blockedCommands = new ArrayList<>();
            blockedWords = new ArrayList<>();

            ads.addAll(blacklist.getStringList("Ads"));

            blockedCommands.addAll(config.getStringList("mute.blockedCommands"));

            blockedWords.addAll(blacklist.getStringList("Words"));

        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten.");
            e.printStackTrace();
        }
    }

    @Override
    public Database getSQL() {
        return sql;
    }

    @Override
    public User getUser(String name) {
        return new SpigotUser(Bukkit.getPlayer(name));
    }

    @Override
    public User getUser(UUID uniqueId) {
        return new SpigotUser(Bukkit.getPlayer(uniqueId));
    }

    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof Player) {
            ((Player) u.getRawUser()).kickPlayer(msg);
        }
    }

    @Override
    public Config getMessages() {
        return messages;
    }

    @Override
    public Config getConfiguration() {
        return config;
    }

    private void init(PluginManager pluginManager) {
        getCommand("ban").setExecutor(new CommandWrapper(new CMDban(banManager, config, messages, sql),true));
        getCommand("check").setExecutor(new CommandWrapper(new CMDcheck(banManager, sql, messages), true));
        getCommand("deletehistory").setExecutor(new CommandWrapper(new CMDdeletehistory(banManager, messages, sql), true));
        getCommand("delhistory").setExecutor(new CommandWrapper(new CMDdeletehistory(banManager, messages, sql), true));
        getCommand("history").setExecutor(new CommandWrapper(new CMDhistory(banManager, messages, config, sql), true));
        getCommand("kick").setExecutor(new CommandWrapper(new CMDkick(messages, sql, banManager), true));
        getCommand("unban").setExecutor(new CommandWrapper(new CMDunban(banManager, sql, messages, config), true));
        getCommand("unmute").setExecutor(new CommandWrapper(new CMDunmute(banManager, messages, config, sql), true));
        getCommand("bansystem").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, sql, mysql, idManager, timeFormatUtil, banManager), false));
        getCommand("bansys").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, sql, mysql, idManager, timeFormatUtil, banManager), false));

        pluginManager.registerEvents(new AsyncPlayerChatListener(config, messages, banManager, mysql, blacklist), this);
        pluginManager.registerEvents(new PlayerCommandPreprocessListener(banManager, config, messages, blockedCommands), this);
        pluginManager.registerEvents(new PlayerConnectionListener(banManager, config, messages, Banscreen, instance, urlUtil), this);
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public String getBanScreen() {
        return Banscreen;
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            users.add(new SpigotUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new SpigotUser(Bukkit.getConsoleSender());
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }

    public static BanManager getBanmanager() {
        return banManager;
    }

    public static Plugin getPlugin() {
        return instance;
    }

    public static List<String> getBlockedWords() {
        return blockedWords;
    }

    public static List<String> getBlockedCommands() {
        return blockedCommands;
    }

    public static List<String> getAds() {
        return ads;
    }
}
