package net.ultragrav.serializer.spigot;

import net.ultragrav.serializer.Meta;
import org.bukkit.configuration.ConfigurationSection;

public class MetaSpigot {
    public static Meta fromConfig(ConfigurationSection config) {
        Meta meta = new Meta();
        for (String str : config.getKeys(true)) {
            String[] strs = str.split("\\.");
            Meta met = meta;
            for (int i = 0; i < strs.length - 1; i++) {
                met = met.getOrSet(strs[i], new Meta());
            }
            met.set(strs[strs.length - 1], config.get(str));
        }
        return meta;
    }
}
