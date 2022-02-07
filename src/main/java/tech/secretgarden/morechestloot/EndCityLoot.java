package tech.secretgarden.morechestloot;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class EndCityLoot implements LootTable {
    @Override
    public Collection<ItemStack> populateLoot(Random random, LootContext context) {

        int loot = context.getLootingModifier(); // You'll need to create your own LootContext for this.
        double lootingMod = loot * .02;

        final List<ItemStack> items = new ArrayList<>();

        if (random.nextDouble() <= (.05 + lootingMod)) {
            // We get the next double from the Random instance passed to the method.
            // If you only want on roll on all of the items you make a variable out of the random.nextDouble() to keep the double consistent throughout the method.
            // If the double is less than or exactly equal the default chance (5%) plus the looting modifier we add the drops to the items list.
            int dropAmount = random.nextInt(3);
            items.add(new ItemStack(Material.ELYTRA, dropAmount == 0 ? 1 : dropAmount)); // We add a new ItemStack, in this case Leather with a random amount from 1 to 3, if the random returns an integer of 0 we instead give 1 leather as a drop instead.
        }

        return items;
    }

    @Override
    public void fillInventory(Inventory inventory, Random random, LootContext context) {
    }

    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(JavaPlugin.getProvidingPlugin(MoreChestLoot.class), "end_city_loot");
    }
}
