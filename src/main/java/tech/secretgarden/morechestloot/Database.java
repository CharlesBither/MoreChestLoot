package tech.secretgarden.morechestloot;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Database {
        /*
    private final String HOST = "";
    private final int PORT = 3306;
    private final String DATABASE = "";
    private final String USERNAME = "";
    private final String PASSWORD = "";
     */
    private final InvConversion invConversion = new InvConversion();

    static ArrayList<String> list = MoreChestLoot.configList;
    public static ComboPooledDataSource pool;

    public static void connect() throws SQLException {

        try {
            pool = new ComboPooledDataSource();
            pool.setDriverClass("com.mysql.jdbc.Driver");
            pool.setJdbcUrl("jdbc:mysql://" + list.get(0) + ":" + list.get(1) + "/" + list.get(2) + "?useSSL=true");
            pool.setUser(list.get(3));
            pool.setPassword(list.get(4));
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnected() {
        return pool != null;
    }

    public ComboPooledDataSource getPool() {
        return pool;
    }

    public static void disconnect() {
        if (isConnected()) {
            pool.close();
        }
    }

    public int getKey(String uuid, String title) {
        int i = 0;
        try (Connection connection = getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, x, y, z, inv FROM player WHERE uuid = '" + uuid + "'")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String stringX = Integer.toString(rs.getInt("x"));
                String stringY = Integer.toString(rs.getInt("y"));
                String stringZ = Integer.toString(rs.getInt("z"));
                if (title.contains(stringX) && title.contains(stringY) && title.contains(stringZ)) {
                    i =  rs.getInt("id");
                }
            }
        } catch (SQLException x) {
            x.printStackTrace();
        }
        return i;
    }

    public boolean check(PlayerInteractEvent e, int x, int y, int z, String uuid) {
        int i = 0;
        try (Connection connection = getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT inv, x, y, z FROM player WHERE uuid = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int xx = rs.getInt("x");
                int yy = rs.getInt("y");
                int zz = rs.getInt("z");
                String invString = rs.getString("inv");
                Inventory inv = invConversion.stringToInventory(invString);
                if (xx == x && yy == y && zz == z) {
                    i = i + 1;
                    e.setCancelled(true);
                    e.getPlayer().openInventory(inv);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return i == 0;
    }
}
