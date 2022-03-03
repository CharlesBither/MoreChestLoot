package tech.secretgarden.morechestloot;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
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
            for (StructureType structureType : structureList) {
                if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                    Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);

                    if (location.distance(structureLocation) <= 100) {
                        //At this point. a chest has been placed near a structure
                        System.out.println("you placed a block near a structure");
                        if (e.getBlockPlaced().getLocation().add(1,0,0).getBlock().getType().equals(Material.CHEST)) {
                            System.out.println("placed next to a chest");
                            Block otherBlock = e.getBlockPlaced().getLocation().add(1,0,0).getBlock();
                            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);


                            if (coreProtectMethods.actionLookup(blockLookup)) {
                                if (!placedBlockCheck(otherBlock)) {
                                    e.setCancelled(true);
                                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                                }
                            }
                        } if (e.getBlockPlaced().getLocation().add(0,0,1).getBlock().getType().equals(Material.CHEST)) {
                            System.out.println("placed next to a chest");
                            Block otherBlock = e.getBlockPlaced().getLocation().add(0,0,1).getBlock();
                            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);
                            if (coreProtectMethods.actionLookup(blockLookup)) {
                                if (!placedBlockCheck(otherBlock)) {
                                    e.setCancelled(true);
                                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                                }
                            }
                        } if (e.getBlockPlaced().getLocation().subtract(1,0,0).getBlock().getType().equals(Material.CHEST)) {
                            System.out.println("placed next to a chest");
                            Block otherBlock = e.getBlockPlaced().getLocation().subtract(1,0,0).getBlock();
                            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);
                            if (coreProtectMethods.actionLookup(blockLookup)) {
                                if (!placedBlockCheck(otherBlock)) {
                                    e.setCancelled(true);
                                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                                }
                            }
                        } if (e.getBlockPlaced().getLocation().subtract(0,0,1).getBlock().getType().equals(Material.CHEST)) {
                            System.out.println("placed next to a chest");
                            Block otherBlock = e.getBlockPlaced().getLocation().subtract(0,0,1).getBlock();
                            List<String[]> blockLookup = CoreProtect.blockLookup(otherBlock, 60 * 60 * 24 * 365 * 5);
                            if (coreProtectMethods.actionLookup(blockLookup)) {
                                if (!placedBlockCheck(otherBlock)) {
                                    e.setCancelled(true);
                                    e.getPlayer().sendMessage(ChatColor.RED + "You cannot place a chest next to a Loot Chest!");
                                }
                            }
                        }
                        placedBlocks.add(location);
                    }

                }
            }
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {

        //did player click on a chest?
        if (e.getAction().equals(RIGHT_CLICK_BLOCK)) {
            Block clickedBlock = e.getClickedBlock();
            if (clickedBlock.getType().equals(Material.CHEST) || clickedBlock.getType().equals(Material.CHEST_MINECART) ||
                    clickedBlock.getType().equals(Material.TRAPPED_CHEST) || clickedBlock.getType().equals(Material.BARREL)) {
                Location location = e.getClickedBlock().getLocation();
                Block block = location.getBlock();

                //is this chest located near a structure?
                for (StructureType structureType : structureList) {
                    if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                        Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);
                        if (location.distance(structureLocation) <= 100) {
                            System.out.println("You interacted near a structure");
                            //look up the chest with cp api
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

                                if (coreProtectMethods.actionLookup(blockLookup)) {
                                    //lookup database to see if they have not placed/broke specified chest before.
                                    for (Location loc : placedBlocks) {
                                        if (loc.equals(location)) {
                                            return;
                                            //WHAT IF I PLACED A CHEST NEXT TO A LOOT CHEST
                                        }
                                    }
                                    if (database.check(e, x, y, z, uuid)) {
                                        //check db. does this location exist for the player? (true == does not exist --- false will return method & OPEN SPECIFIED INVENTORY)
                                        //WILL BE SET CANCELLED BY METHOD so that a virtual inventory can be created in its place

                                        //define the chest
                                        String title = "Loot Chest " + stringX + " " + stringY + " " + stringZ;
                                        Inventory inv = Bukkit.createInventory(null, 36, title);

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
                                        //AT THIS POINT, EVENT HAS BEEN CANCELLED, INVENTORY HAS BEEN CREATED, AND STORED IN DB.
                                    }
                                }
                            }
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
            if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.CHEST_MINECART) ||
                    block.getType().equals(Material.TRAPPED_CHEST) || block.getType().equals(Material.BARREL)) {
                for (StructureType structureType : structureList) {
                    if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                        Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);
                        if (location.distance(structureLocation) <= 100) {
                            System.out.println("You opened an inventory near a structure");
                            List<String[]> blockLookup = CoreProtect.blockLookup(block, 60 * 60 * 24 * 365 * 5);
                            if (blockLookup == null) {
                                System.out.println("lookup is null! This is an error!");
                            } else {
                                int x = block.getX();
                                int y = block.getY();
                                int z = block.getZ();
                                if (coreProtectMethods.actionLookup(blockLookup)) {
                                    //lookup database to see if they have placed/broke specified chest before.
                                    for (Location loc : placedBlocks) {
                                        if (loc.equals(location)) {
                                            System.out.println("this block has been placed");
                                            return;
                                        }
                                    }
                                    ItemStack[] items = e.getInventory().getContents();

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
                        }

                    }
                }
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {

        Location location = e.getPlayer().getLocation();

        for (StructureType structureType : structureList) {
            if (location.getWorld().locateNearestStructure(location, structureType, 3, false) != null) {
                Location structureLocation = location.getWorld().locateNearestStructure(location, structureType, 3, false);
                if (location.distance(structureLocation) <= 100) {
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
    }

    private boolean placedBlockCheck(Block otherBlock) {
        int i = 1;
        for (Location loc : placedBlocks) {
            if (loc.equals(otherBlock.getLocation())) {
                //placed block and checked block are the same
                i = i + 1;
            }
        }
        return i == 2;
    }

    /*
    @EventHandler
    public void click(InventoryClickEvent e) {
        Biome biome = e.getWhoClicked().getLocation().getBlock().getBiome();
        //gets biome player is standing in
        if (biome.equals(Biome.END_HIGHLANDS) || biome.equals(Biome.END_BARRENS) || biome.equals(Biome.END_MIDLANDS) || biome.equals(Biome.SMALL_END_ISLANDS)) {
            //is clicked inventory instance of a loot chest
            if (e.getView().getTitle().contains("Loot Chest")) {

                //assign new inventory data -> String
                String uuid = e.getWhoClicked().getUniqueId().toString();
                String title = e.getView().getTitle();
                String invString = invConversion.inventoryToString(e.getView().getTopInventory(), title);

                //find inventory by uuid & xyz values
                int key = database.getKey(uuid, title);
                if (key > 0) {
                    //update db with new inventory string
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

     */
}
