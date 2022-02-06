package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventListener implements Listener {

    private final InvConversion invConversion = new InvConversion();
    private final Database database = new Database();
    private final CoreProtectAPI CoreProtect = new CoreProtectAPI();
    private final List<ItemStack> endItems = new ArrayList<>();

    LocalDateTime date = LocalDateTime.now();
    Timestamp timestamp = Timestamp.valueOf(date);

    



    @EventHandler
    public void open(InventoryOpenEvent e) {

        if (e.getInventory().getType().equals(InventoryType.CHEST)) {
            BlockState blockState = (BlockState) e.getInventory().getHolder();
            Block block = blockState.getBlock();
            if (block.getType().equals(Material.CHEST)) {
                Location loc = block.getLocation();//loc of opened inv
                Biome biome = loc.getWorld().getBiome(loc);
                if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {

                    List<String[]> lookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);

                    if (lookup == null) {
                        System.out.println("lookup is null! This is an error!");
                    } else {
                        int x = loc.getBlockX();
                        int y = loc.getBlockY();
                        int z = loc.getBlockZ();
                        String uuid = e.getPlayer().getUniqueId().toString();


                        ArrayList<Integer> actionList = blockLookup(lookup);

                        if (actionList.isEmpty()) {
                            System.out.println("arraylist is empty");
                            //lookup database to see if they have opened chest before.
                            if (check(e, x, y, z, uuid)) {
                                System.out.println("testing");
                                e.setCancelled(true);
                                Inventory inv = Bukkit.createInventory(null, 36);
                                for (ItemStack item : endItems) {
                                    inv.addItem(item);
                                }
                                e.getPlayer().openInventory(inv);
                                System.out.println("testing1");
                                String invString = invConversion.inventoryToString(inv);
                                try (Connection connection = database.getPool().getConnection();
                                     PreparedStatement statement = connection.prepareStatement("INSERT INTO players (UUID, INV, X, Y, Z, Timestamp) VALUES (?,?,?,?,?,?);")) {
                                    statement.setString(1, uuid);
                                    statement.setString(2, invString);
                                    statement.setInt(3, x);
                                    statement.setInt(4, y);
                                    statement.setInt(5, z);
                                    statement.setTimestamp(6, timestamp);

                                } catch (SQLException exception) {
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean check(InventoryOpenEvent e, int x, int y, int z, String uuid) {
        int i = 0;
        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT Inv, X, Y, Z FROM Players WHERE UUID = '" + uuid + "'")) {
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
                        e.getPlayer().openInventory(inv);
                    }
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return i == 0;
    }

    private ArrayList<Integer> blockLookup(List<String[]> lookup) {
        ArrayList<Integer> actionList = new ArrayList<>();
        for (String[] value : lookup) {
            CoreProtectAPI.ParseResult result = CoreProtect.parseResult(value);
            int action = result.getActionId();
            if (action == 1 || action == 0) {
                actionList.add(1);
            }
        }
        return actionList;
    }

        /*
        if (loc.getWorld().getBiome(loc).equals(Biome.END_HIGHLANDS)) {
            try (Connection connection = database.getPool().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT Inv, X, Y, Z FROM Players WHERE UUID = '" + uuid + "'")) {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    if (x == rs.getInt("X") && y == rs.getInt("Y") && z == rs.getInt("Z")) {
                        String inv = rs.getString("Inv");
                        e.setCancelled(true);
                        Inventory inventory = invConversion.stringToInventory(inv);
                        e.getPlayer().openInventory(inventory);
                    }
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }



            /*
            - Connect to database table Players
            - dbLoc = ps(SELECT Location FROM Players WHERE UUID = 'uuid';);
            - ResultSet rs = ps.executeQuery;
            - while (rs.next) {
                if (loc == dbloc) {
                    inv = ps(SELECT Inv WHERE Location = loc)
                    rs = ps.executeQuery;
                    while (rs.next) {
                        player.openInventory(inv)
                    }
                }

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








        /*
        Location loc = e.getLootContext().getLocation();
        //Location loc = e.getInventoryHolder().getInventory().getLocation();//loc of opened inv
        Biome biome = loc.getWorld().getBiome(loc);
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS)) {
            String uuid = e.getInventoryHolder().getInventory().getViewers().get(0).getUniqueId().toString();
            String test = e.getLootContext().getKiller().getUniqueId().toString();
            String test1 = e.getEntity().getUniqueId().toString();
            System.out.println(uuid + test + test1);
            e.setCancelled(true);

            //String uuid = e.getLootContext().getKiller().getUniqueId().toString();
            //String uuid = e.getEntity().getUniqueId().toString();
            Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST);
            LootContext context = e.getLootContext();
            e.getLootTable().fillInventory(inv, null, new LootContext.Builder(e.getEntity().getLocation()).build());


            String invString = invConversion.inventoryToString(inv);
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            try (Connection connection = database.getPool().getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO TABLE Players UUID, INV, X, Y, Z, Timestamp VALUES ?,?,?,?,?,?;")) {
                statement.setString(1, uuid);
                statement.setString(2, invString);
                statement.setInt(3, x);
                statement.setInt(4, y);
                statement.setInt(5, z);
                statement.setTimestamp(6, timestamp);

            } catch (SQLException exception) {
                exception.printStackTrace();
            }


        }

        - Check if player is in highlands DONE
        - set cancelled
        - create inventory (chest, 36)
        - Inv to String()
        - Connect to database table Players
        - add values ID, UUID, Inv, Location, Timestamp

    }

     */

    }

}
