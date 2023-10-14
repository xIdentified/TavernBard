package me.xidentified.tavernbard;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;

@TraitName("bard")
public class BardTrait extends Trait {

    @Persist private boolean isBard = true;

    public BardTrait() {
        super("bard");
    }

    // Keeping load and save methods as they are
    @Override
    public void load(DataKey key) {
        isBard = key.getBoolean("isBard", true);
    }

    @Override
    public void save(DataKey key) {
        key.setBoolean("isBard", isBard);
    }

    public boolean isBard() {
        return isBard;
    }

}

