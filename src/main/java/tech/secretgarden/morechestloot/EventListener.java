package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class EventListener implements Listener {

    private final InvConversion invConversion = new InvConversion();
    private final Database database = new Database();
    private final CoreProtectAPI CoreProtect = new CoreProtectAPI();
    private final CoreProtectMethods coreProtectMethods = new CoreProtectMethods();
    private static final List<Location> placedBlocks = new ArrayList<>();
    //if a chest is placed and opened quickly after, CP api does not have enough time to lookup block properly. placedBlocks will perform the check instead.
    public static List<StructureType> structureList = new ArrayList<>();

    LocalDateTime date = LocalDateTime.now();
    Timestamp timestamp = Timestamp.valueOf(date);

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType().equals(Material.CHEST)) {
            Location location = e.getBlockPlaced().getLocation();

            if (structureDistanceCheck(location)) {
                //At this point. a chest has been placed near a structure
                System.out.println("you placed a block near a structure");

                //checks if a chest was placed next to a loot chest
                checkBlockPlaceSouthEast(e, 1, 0);
                checkBlockPlaceSouthEast(e, 0, 1);
                checkBlockPlaceNorthWest(e, 1, 0);
                checkBlockPlaceNorthWest(e, 0, 1);

                if (!e.isCancelled()) {
                    placedBlocks.add(location);
                }
            }
            /*
            for (StructureType structureType : structureList) {
                if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                    Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);

                    if (location.distanceSquared(structureLocation) <= 10000) {
                        //At this point. a chest has been placed near a structure
                        System.out.println("you placed a block near a structure");

                        //checks if a chest was placed next to a loot chest
                        checkBlockPlaceSouthEast(e, 1, 0);
                        checkBlockPlaceSouthEast(e, 0, 1);
                        checkBlockPlaceNorthWest(e, 1, 0);
                        checkBlockPlaceNorthWest(e, 0, 1);

                        if (!e.isCancelled()) {
                            placedBlocks.add(location);
                        }
                    }
                }
            }

             */
        }
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("mcl.a")) {
            Player player = e.getPlayer();
            Block block = e.getBlock();
            Location location = block.getLocation();
            if (structureDistanceCheck(location)) {
                List<String[]> blockLookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);
                if (blockLookup == null) {
                    System.out.println("lookup is null! This is an error!");
                } else {
                    if (coreProtectMethods.actionLookup(blockLookup)) {
                        //(if a player has NOT placed this block before)
                        if (placedBlockMapCheck(block)) {
                            //(if a player has placed this block before)
                            return;
                        } else {
                            e.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You do not have permission to break a Loot Chest");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void open(InventoryOpenEvent e) {
        if (e.getInventory().getLocation() != null) {

            Block block = e.getInventory().getLocation().getBlock();
            Location location = block.getLocation();
            String uuid = e.getPlayer().getUniqueId().toString();
            if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.CHEST_MINECART) ||
                    block.getType().equals(Material.TRAPPED_CHEST) || block.getType().equals(Material.BARREL)) {
                if (structureDistanceCheck(location)) {
                    //Looking up block with cp api
                    List<String[]> blockLookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);
                    if (blockLookup == null) {
                        System.out.println("lookup is null! This is an error!");
                    } else {
                        int x = block.getX();
                        int y = block.getY();
                        int z = block.getZ();

                        if (block.getType().equals(Material.CHEST)) {
                            Chest chest = (Chest) block.getState();
                            InventoryHolder holder = chest.getBlockInventory().getHolder();
                            if (holder instanceof DoubleChest) {
                                DoubleChest doubleChest = (DoubleChest) holder;
                                Chest leftChest = (Chest) doubleChest.getLeftSide();
                                x = leftChest.getX();
                                y = leftChest.getY();
                                z = leftChest.getZ();

                                if (coreProtectMethods.actionLookup(blockLookup)) {
                                    //(if a player has NOT placed this block before)
                                    if (placedBlockMapCheck(block)) {
                                        //(if a player has placed this block before)
                                        return;
                                    } else {
                                        checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                        //If player has loot chest it will stop here
                                        setInventory(e, x, y, z, location);
                                    }
                                }
                            } else {
                                //block = single chest
                                if (coreProtectMethods.actionLookup(blockLookup)) {
                                    //(if a player has NOT placed this block before)
                                    if (placedBlockMapCheck(block)) {
                                        //(if a player has placed this block before)
                                        return;
                                    } else {
                                        checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                        //If player has loot chest it will stop here
                                        setInventory(e, x, y, z, location);
                                    }
                                }
                            }
                        } else {
                            //block = a block other than a chest
                            if (coreProtectMethods.actionLookup(blockLookup)) {
                                //(if a player has NOT placed this block before)
                                if (placedBlockMapCheck(block)) {
                                    //(if a player has placed this block before)
                                    return;
                                } else {
                                    checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                    //If player has loot chest it will stop here
                                    setInventory(e, x, y, z, location);
                                }
                            }
                        }
                    }
                }
                /*
                for (StructureType structureType : structureList) {
                    if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                        Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);
                        if (location.distanceSquared(structureLocation) <= 10000) {
                            //Looking up block with cp api
                            List<String[]> blockLookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);
                            if (blockLookup == null) {
                                System.out.println("lookup is null! This is an error!");
                            } else {

                                if (block.getType().equals(Material.CHEST)) {
                                    Chest chest = (Chest) block.getState();
                                    InventoryHolder holder = chest.getBlockInventory().getHolder();
                                    if (holder instanceof DoubleChest) {
                                        DoubleChest doubleChest = (DoubleChest) holder;
                                        Chest leftChest = (Chest) doubleChest.getLeftSide();
                                        int x = leftChest.getX();
                                        int y = leftChest.getY();
                                        int z = leftChest.getZ();

                                        if (coreProtectMethods.actionLookup(blockLookup)) {
                                            //(if a player has NOT placed this block before)
                                            if (placedBlockMapCheck(block)) {
                                                //(if a player has placed this block before)
                                                return;
                                            } else {
                                                checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                                //If player has loot chest it will stop here
                                                setInventory(e, x, y, z, location);
                                            }
                                        }
                                    } else {
                                        //block = single chest
                                        int x = block.getX();
                                        int y = block.getY();
                                        int z = block.getZ();
                                        if (coreProtectMethods.actionLookup(blockLookup)) {
                                            //(if a player has NOT placed this block before)
                                            if (placedBlockMapCheck(block)) {
                                                //(if a player has placed this block before)
                                                return;
                                            } else {
                                                checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                                //If player has loot chest it will stop here
                                                setInventory(e, x, y, z, location);
                                            }
                                        }
                                    }
                                } else {
                                    //block = a block other than a chest
                                    int x = block.getX();
                                    int y = block.getY();
                                    int z = block.getZ();
                                    if (coreProtectMethods.actionLookup(blockLookup)) {
                                        //(if a player has NOT placed this block before)
                                        if (placedBlockMapCheck(block)) {
                                            //(if a player has placed this block before)
                                            return;
                                        } else {
                                            checkOrMakeInventory(e, x, y, z, 27, uuid, location);
                                            //If player has loot chest it will stop here
                                            setInventory(e, x, y, z, location);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                 */
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {

        Location location = e.getPlayer().getLocation();

        if (structureDistanceCheck(location)) {
            System.out.println("You closed an inventory near a structure");
            if (e.getView().getTitle().contains("Loot Chest")) {
                String uuid = e.getPlayer().getUniqueId().toString();
                String title = e.getView().getTitle();
                String invString = invConversion.inventoryToString(e.getView().getTopInventory(), title);
                int key = database.getKey(uuid, title);
                if (key > 0) {
                    try (Connection connection = database.getPool().getConnection();
                         PreparedStatement statement = connection.prepareStatement("UPDATE player SET " +
                                 "inv = ? WHERE id = " + key + ";")) {
                        statement.setString(1, invString);
                        statement.executeUpdate();

                    } catch (SQLException x) {
                        x.printStackTrace();
                    }
                }
            }
        }
        /*
        for (StructureType structureType : structureList) {
            if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);
                if (location.distanceSquared(structureLocation) <= 10000) {
                    System.out.println("You closed an inventory near a structure");
                    if (e.getView().getTitle().contains("Loot Chest")) {
                        String uuid = e.getPlayer().getUniqueId().toString();
                        String title = e.getView().getTitle();
                        String invString = invConversion.inventoryToString(e.getView().getTopInventory(), title);
                        int key = database.getKey(uuid, title);
                        if (key > 0) {
                            try (Connection connection = database.getPool().getConnection();
                                 PreparedStatement statement = connection.prepareStatement("UPDATE player SET " +
                                         "inv = ? WHERE id = " + key + ";")) {
                                statement.setString(1, invString);
                                statement.executeUpdate();

                            } catch (SQLException x) {
                                x.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

         */
    }

    private boolean structureDistanceCheck(Location location) {
        int i = 1;
        for (StructureType structureType : structureList) {
            if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);

                if (location.distanceSquared(structureLocation) <= 10000) {
                    i = i + 1;

                }
            }
        }
        return i > 1;
    }

    private void checkBlockPlaceSouthEast(BlockPlaceEvent e, int x, int z) {
        if (e.getBlockPlaced().getLocation().add(x,0, z).getBlock().getType().equals(Material.CHEST)) {
            Block otherBlock = e.getBlockPlaced().getLocation().add(x,0, z).getBlock();
            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);
            if (coreProtectMethods.actionLookup(blockLookup)) {
                //player did not place or break otherBlock according to cp api
                if (!placedBlockMapCheck(otherBlock)) {
                    //check placedBlocks map in the event cp api has not updated otherBlock yet.
                    //False == otherBlock is a loot chest. Set cancelled
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                }
            }
        }
    }
    private void checkBlockPlaceNorthWest(BlockPlaceEvent e, int x, int z) {
        if (e.getBlockPlaced().getLocation().subtract(x,0, z).getBlock().getType().equals(Material.CHEST)) {
            Block otherBlock = e.getBlockPlaced().getLocation().subtract(x,0, z).getBlock();
            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);
            if (coreProtectMethods.actionLookup(blockLookup)) {
                //player did not place or break otherBlock according to cp api
                if (!placedBlockMapCheck(otherBlock)) {
                    //check placedBlocks map in the event cp api has not updated otherBlock yet.
                    //False == otherBlock is a loot chest. Set cancelled
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                }
            }
        }
    }

    private boolean placedBlockMapCheck(Block otherBlock) {
        int i = 1;
        for (Location loc : placedBlocks) {
            if (loc.equals(otherBlock.getLocation())) {
                //placed block and checked block are the same
                i = i + 1;
            }
        }
        return i == 2;
    }

    private void checkOrMakeInventory(InventoryOpenEvent e, int x, int y, int z, int invSize, String uuid, Location location) {
        if (database.check(e, x, y, z, uuid)) {
            String stringX = Integer.toString(x);
            String stringY = Integer.toString(y);
            String stringZ = Integer.toString(z);
            //check db. does this location exist for the player? (true == does not exist --- false will return method & OPEN SPECIFIED INVENTORY)
            //define the chest
            String title = "Loot Chest " + stringX + " " + stringY + " " + stringZ;
            Inventory inv = Bukkit.createInventory(null, invSize, title);

            //open newly created inventory for player
            String invString = invConversion.inventoryToString(inv, title);
            System.out.println("created loot chest");

            //store objects in db
            try (Connection connection = database.getPool().getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO player (uuid, inv, x, y, z, world, timestamp) VALUES (?,?,?,?,?,?,?);")) {
                statement.setString(1, uuid);
                statement.setString(2, invString);
                statement.setInt(3, x);
                statement.setInt(4, y);
                statement.setInt(5, z);
                statement.setString(6, location.getWorld().getName());
                statement.setTimestamp(7, timestamp);
                statement.executeUpdate();

            } catch (SQLException exception) {
                exception.printStackTrace();
            }

        }
    }

    private void setInventory(InventoryOpenEvent e, int x, int y, int z, Location location) {
        //getting items from natural chest
        ItemStack[] items = e.getInventory().getContents();

        //Finds inv that corresponds with clicked chest
        try (Connection connection = database.getPool().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT inv FROM player WHERE x = ? AND y = ? AND z = ? AND world = ?")) {
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setString(4, location.getWorld().getName());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String invString = rs.getString("inv");
                Inventory inv = invConversion.stringToInventory(invString);
                inv.setContents(items);
                double random = new Random().nextDouble();
                if (random <= 0.06) {
                    inv.addItem(new ItemStack(Material.ELYTRA));
                }
                double random1 = new Random().nextDouble();
                if (random1 <= 0.04) {
                    inv.addItem(new ItemStack(Material.NETHER_STAR));
                }
                e.setCancelled(true);
                e.getPlayer().openInventory(inv);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
