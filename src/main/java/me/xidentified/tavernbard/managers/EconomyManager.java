package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.TavernBard;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final TavernBard plugin;
    private Economy economy;

    public EconomyManager(TavernBard plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

    public boolean chargePlayer(Player player, double amount) {
        if(economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount);
            return true;  // Good to go!
        }
        return false;  // Not enough monies
    }
}
