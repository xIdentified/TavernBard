package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.TavernBard;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemCostManager {

    private Material costItem;
    private final int costAmount;
    private boolean isEnabled;

    public ItemCostManager(String itemName, int costAmount, boolean isEnabled, TavernBard plugin) {
        this.costAmount = costAmount;
        this.isEnabled = isEnabled;

        try {
            this.costItem = Material.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid item-cost item in config.yml. Item cost feature disabled.");
            this.isEnabled = false;
        }
    }

    public boolean canAfford(Player player) {
        if (!isEnabled) return true;

        PlayerInventory inventory = player.getInventory();
        return inventory.contains(costItem, costAmount);
    }

    public void deductCost(Player player) {
        if (!isEnabled) return;

        PlayerInventory inventory = player.getInventory();
        inventory.removeItem(new ItemStack(costItem, costAmount));
    }

    public Material getCostItem() {
        return costItem;
    }

    public int getCostAmount() {
        return costAmount;
    }

    public String formatEnumName(String enumName) {
        String[] words = enumName.split("_");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            formattedName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
        }

        return formattedName.toString().trim();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

}