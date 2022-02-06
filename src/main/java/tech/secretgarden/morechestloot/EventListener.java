package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class EventListener implements Listener {

    private final InvConversion invConversion = new InvConversion();
    private final Database database = new Database();
    private final CoreProtectAPI CoreProtect = new CoreProtectAPI();
    private final List<ItemStack> endItems = new ArrayList<>();

    LocalDateTime date = LocalDateTime.now();
    Timestamp timestamp = Timestamp.valueOf(date);

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
                    String uuid = e.getPlayer().getUniqueId().toString();

                    if (actionLookup(blockLookup)) {
                        System.out.println("this is a clean chest");
                        //lookup database to see if they have opened chest before.
                        if (check(e, x, y, z, uuid)) {
                            System.out.println("testing");
                            e.setCancelled(true);
                            Chest chest = (Chest) block.getState();
                            Inventory inv = Bukkit.createInventory(null, 36);
                            LootTable lt = LootTables.END_CITY_TREASURE.getLootTable();
                            chest.setLootTable(lt);
                            List<ItemStack> items = Arrays.asList(chest.getInventory().getContents());
                            for (ItemStack item : items) {
                                if (item != null) {
                                    inv.addItem(item);
                                }
                            }

                            e.getPlayer().openInventory(inv);
                            System.out.println("testing1");
                            String invString = invConversion.inventoryToString(inv);
                            System.out.println("converted inv");
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
                            System.out.println("finished");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        Biome biome = e.getWhoClicked().getLocation().getBlock().getBiome();
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
            if (e.getClickedInventory().getType().equals(InventoryType.CHEST)) {
                try (Connection connection = database.getPool().getConnection();
                    PreparedStatement statement = connection.prepareStatement("")) {

                } catch (SQLException x) {
                    x.printStackTrace();
                }
            }
        }
    }

    private boolean check(PlayerInteractEvent e, int x, int y, int z, String uuid) {
        int i = 0;
        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT Inv, X, Y, Z FROM players WHERE UUID = '" + uuid + "'")) {
            ResultSet rs = statement.executeQuery();
            System.out.println("test");
            if (rs.next()) {
                while (rs.next()) {

                    System.out.println("test1");
                    int xx = rs.getInt("X");
                    int yy = rs.getInt("Y");
                    int zz = rs.getInt("Z");
                    String invString = rs.getString("Inv");
                    Inventory inv = invConversion.stringToInventory(invString);
                    if (xx == x && yy == y && zz == z) {
                        System.out.println("test2");
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
/*
    private ArrayList<Integer> actionLookup(List<String[]> lookup) {
        ArrayList<Integer> actionList = new ArrayList<>();
        for (String[] value : lookup) {
            //gets every result from CP api blockLookup method.
            CoreProtectAPI.ParseResult result = CoreProtect.parseResult(value);
            int action = result.getActionId();
            //actions are either placed, broken, or interact.
            if (action == 1 || action == 0) {
                actionList.add(1);
            }
        }
        return actionList;
    }

 */

    @EventHandler
    public void generate(LootGenerateEvent e) {
        Location loc = e.getLootContext().getLocation();
        Biome biome = loc.getWorld().getBiome(loc);
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
            BlockState block = e.getLootContext().getLocation().getWorld().getBlockState(loc);
            Block chest = block.getBlock();
            if (e.getInventoryHolder().getInventory().getHolder().equals(chest)) {
                List<ItemStack> items = e.getLoot();
                endItems.addAll(items);
                e.setCancelled(true);
            }
        }
    }
}
