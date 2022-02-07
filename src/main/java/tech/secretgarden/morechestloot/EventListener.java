package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class EventListener implements Listener {

    private final InvConversion invConversion = new InvConversion();
    private final Database database = new Database();
    private final CoreProtectAPI CoreProtect = new CoreProtectAPI();
    private static final List<Location> placedBlocks = new ArrayList<>();
    private final EndCityLoot endCityLoot = new EndCityLoot();
    //if a chest is placed and opened quickly after, CP api does not have enough time to lookup block properly. placedBlocks will perform the check instead.

    LocalDateTime date = LocalDateTime.now();
    Timestamp timestamp = Timestamp.valueOf(date);

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType().equals(Material.CHEST)) {
            Location loc = e.getBlockPlaced().getLocation();
            Biome biome = e.getBlockPlaced().getBiome();
            if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
                placedBlocks.add(loc);
            }
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (e.getAction().equals(RIGHT_CLICK_BLOCK) && e.getClickedBlock().getType().equals(Material.CHEST)) {
            Location location = e.getClickedBlock().getLocation();
            Block block = location.getBlock();
            Biome biome = location.getBlock().getBiome();
            if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
                List<String[]> blockLookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);
                if (blockLookup == null) {
                    System.out.println("lookup is null! This is an error!");
                } else {
                    int x = block.getX();
                    int y = block.getY();
                    int z = block.getZ();
                    String stringX = Integer.toString(x);
                    String stringY = Integer.toString(y);
                    String stringZ = Integer.toString(z);
                    String uuid = e.getPlayer().getUniqueId().toString();

                    if (actionLookup(blockLookup)) {
                        //lookup database to see if they have opened chest before.
                        for (Location loc : placedBlocks) {
                            if (loc.equals(location)) {
                                return;
                            }
                        }
                        if (check(e, x, y, z, uuid)) {
                            e.setCancelled(true);
                            String title = "Loot Chest " + stringX + " " + stringY + " " + stringZ;
                            Inventory inv = Bukkit.createInventory(null, 36, title);
                            Chest chest = (Chest) block.getState();
                            LootTables lootTables = (LootTables.END_CITY_TREASURE);
                            LootTable lootTable = lootTables.getLootTable();
                            chest.setLootTable(lootTable);
                            chest.update();
                            ItemStack[] items = chest.getBlockInventory().getContents();
                            inv.setContents(items);
                            double random = new Random().nextDouble();
                            if (random <= 0.06) {
                                inv.addItem(new ItemStack(Material.ELYTRA));
                            }
                            double random1 = new Random().nextDouble();
                            if (random1 <= 0.03) {
                                inv.addItem(new ItemStack(Material.NETHER_STAR));
                            }
                            e.getPlayer().openInventory(inv);
                            String invString = invConversion.inventoryToString(inv, title);
                            System.out.println("created loot chest");
                            try (Connection connection = database.getPool().getConnection();
                                 PreparedStatement statement = connection.prepareStatement("INSERT INTO players (UUID, Inv, X, Y, Z, Timestamp) VALUES (?,?,?,?,?,?);")) {
                                statement.setString(1, uuid);
                                statement.setString(2, invString);
                                statement.setInt(3, x);
                                statement.setInt(4, y);
                                statement.setInt(5, z);
                                statement.setTimestamp(6, timestamp);
                                statement.executeUpdate();

                            } catch (SQLException exception) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void click(InventoryClickEvent e) {
        Biome biome = e.getWhoClicked().getLocation().getBlock().getBiome();
        //gets biome player is standing in
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
            if (e.getView().getTitle().contains("Loot Chest")) {
                String uuid = e.getWhoClicked().getUniqueId().toString();
                String title = e.getView().getTitle();
                String invString = invConversion.inventoryToString(e.getView().getTopInventory(), title);
                int key = getKey(uuid, title);
                if (key > 0) {
                    try (Connection connection = database.getPool().getConnection();
                         PreparedStatement statement = connection.prepareStatement("UPDATE players SET " +
                                 "Inv = ? WHERE ID = " + key + ";")) {
                        statement.setString(1, invString);
                        statement.executeUpdate();

                    } catch (SQLException x) {
                        x.printStackTrace();
                    }
                }
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        Biome biome = e.getPlayer().getLocation().getBlock().getBiome();
        //gets biome player is standing in
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
            if (e.getView().getTitle().contains("Loot Chest")) {
                String uuid = e.getPlayer().getUniqueId().toString();
                String title = e.getView().getTitle();
                String invString = invConversion.inventoryToString(e.getView().getTopInventory(), title);
                int key = getKey(uuid, title);
                if (key > 0) {
                    try (Connection connection = database.getPool().getConnection();
                         PreparedStatement statement = connection.prepareStatement("UPDATE players SET " +
                                 "Inv = ? WHERE ID = " + key + ";")) {
                        statement.setString(1, invString);
                        statement.executeUpdate();

                    } catch (SQLException x) {
                        x.printStackTrace();
                    }
                }
            }
        }
    }

    private int getKey(String uuid, String title) {
        int i = 0;
        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT ID, X, Y, Z, Inv FROM players WHERE UUID = '" + uuid + "'")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String stringX = Integer.toString(rs.getInt("X"));
                String stringY = Integer.toString(rs.getInt("Y"));
                String stringZ = Integer.toString(rs.getInt("Z"));
                if (title.contains(stringX) && title.contains(stringY) && title.contains(stringZ)) {
                    i =  rs.getInt("ID");
                }
            }
        } catch (SQLException x) {
            x.printStackTrace();
        }
        return i;
    }

    private boolean check(PlayerInteractEvent e, int x, int y, int z, String uuid) {
        int i = 0;
        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT Inv, X, Y, Z FROM players WHERE UUID = ?")) {
            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                while (rs.next()) {
                    int xx = rs.getInt("X");
                    int yy = rs.getInt("Y");
                    int zz = rs.getInt("Z");
                    String invString = rs.getString("Inv");
                    Inventory inv = invConversion.stringToInventory(invString);
                    if (xx == x && yy == y && zz == z) {
                        i = i + 1;
                        e.setCancelled(true);
                        e.getPlayer().openInventory(inv);
                    }
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return i == 0;
    }

    private boolean actionLookup(List<String[]> lookup) {
        int i = 1;
        for (String[] value : lookup) {
            //gets every result from CP api blockLookup method.
            CoreProtectAPI.ParseResult result = CoreProtect.parseResult(value);
            int action = result.getActionId();
            //actions are either placed, broken, or interact.
            if (action == 1 || action == 0) {
                i = i + 1;
            }
        }
        return i == 1;
    }
}
