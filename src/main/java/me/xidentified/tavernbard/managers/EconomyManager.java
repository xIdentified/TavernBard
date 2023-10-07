package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final TavernBard plugin;
    private final MessageUtil messageUtil;
    private Economy economy;

    public EconomyManager(TavernBard plugin) {
        this.plugin = plugin;
        this.messageUtil = plugin.getMessageUtil();
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
            messageUtil.sendParsedMessage(player, "<green>Paid the bard " + amount + " to perform.");
            return true;  // Good to go!
        }
        messageUtil.sendParsedMessage(player, "<red>Insufficient funds. Costs " + amount + " to play.");
        return false;  // Not enough monies
    }
}
