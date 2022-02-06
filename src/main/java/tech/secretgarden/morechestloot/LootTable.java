package tech.secretgarden.morechestloot;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.Lootable;

import java.util.Collection;
import java.util.Random;

public class LootTable implements org.bukkit.loot.LootTable {
    @Override
    public Collection<ItemStack> populateLoot(Random random, LootContext context) {
        return null;
    }

    @Override
    public void fillInventory(Inventory inventory, Random random, LootContext context) {

    }

    @Override
    public NamespacedKey getKey() {
        return null;
    }
}
