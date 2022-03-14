package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.StructureType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class MoreChestLoot extends JavaPlugin {

    public static ArrayList<String> configList = new ArrayList<>();
    public static CoreProtectAPI api;
    private Database database = new Database();
    public ArrayList<String> getList() {
        configList.add(getConfig().getString("HOST"));
        configList.add(getConfig().getString("PORT"));
        configList.add(getConfig().getString("DATABASE"));
        configList.add(getConfig().getString("USERNAME"));
        configList.add(getConfig().getString("PASSWORD"));
        return configList;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("Plugin has loaded");

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        if (getConfig().getString("HOST") != null) {
            try {
                getList();
                Database.connect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try (Connection connection = database.getPool().getConnection();
                 PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS player (" +
                         "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                         "uuid VARCHAR(36), " +
                         "inv TEXT(65000), " +
                         "x INT, " +
                         "y INT, " +
                         "z INT, " +
                         "world VARCHAR(30)," +
                         "timestamp TIMESTAMP NOT NULL);")) {
                statement.executeUpdate();

            } catch (Exception x) {
                x.printStackTrace();
            }
            ping.runTaskTimer(this, 20, 20 * 60);
        }

        System.out.println("Connected to database = " + Database.isConnected());

        Bukkit.getPluginManager().registerEvents(new EventListener(), this);

        Map<String, StructureType> structureMap = StructureType.getStructureTypes();
        for (Map.Entry<String, StructureType> entry : structureMap.entrySet()) {
            EventListener.structureList.add(entry.getValue());
        }

        if (getCpAPI() != null) {
            System.out.println("Found CP API");
        } else {
            System.out.println("CP API not found");
        }

    }

    public CoreProtect getCpAPI() {
        Plugin cpPlugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");
        if (cpPlugin instanceof CoreProtect) {
            return (CoreProtect) cpPlugin;
        } else {
            return null;
        }
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Plugin has been disabled");
        Database.disconnect();
    }

    BukkitRunnable ping = new BukkitRunnable() {
        @Override
        public void run() {
            try (Connection connection = database.getPool().getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                statement.executeQuery();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    };
}
