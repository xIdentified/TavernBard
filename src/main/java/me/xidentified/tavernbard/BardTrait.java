package me.xidentified.tavernbard;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import net.citizensnpcs.api.event.NPCRightClickEvent;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;


@TraitName("bard")
public class BardTrait extends Trait {
    private final TavernBard plugin;
    private final SongManager songManager;

    public BardTrait() {
        super("bard");
        this.plugin = getPlugin(TavernBard.class);
        this.songManager = plugin.getSongManager();
    }

    @Persist private boolean isBard = true;

    @Override
    public void load(DataKey key) {
        isBard = key.getBoolean("isBard", true);
    }

    @Override
    public void save(DataKey key) {
        key.setBoolean("isBard", isBard);
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (event.getNPC() == this.getNPC()) {
            Player player = event.getClicker();
            SongSelectionGUI gui = new SongSelectionGUI(plugin, songManager, event.getNPC());
            player.openInventory(gui.getInventory());
        }
    }

}
