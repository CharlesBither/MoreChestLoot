package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public final class MoreChestLoot extends JavaPlugin {

    public static ArrayList<String> configList = new ArrayList<>();
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
        }

        System.out.println("Connected to database = " + Database.isConnected());

        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS Players (" +
                     "ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                     "UUID VARCHAR(36), " +
                     "Inv TEXT(65000), " +
                     "X INT, " +
                     "Y INT, " +
                     "Z INT, " +
                     "Timestamp TIMESTAMP NOT NULL);")) {
            statement.executeUpdate();

        } catch (Exception x) {
            x.printStackTrace();
        }

        CoreProtectAPI api = getCoreProtect();
        if (api != null){ // Ensure we have access to the API
            api.testAPI(); // Will print out "[CoreProtect] API test successful." in the console.
        }
        Bukkit.getPluginManager().registerEvents(new EventListener(), this);
    }
    public CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 7) {
            return null;
        }

        return CoreProtect;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Plugin has been disabled");
    }
}
