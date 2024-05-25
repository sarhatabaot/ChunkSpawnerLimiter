package com.cyprias.chunkspawnerlimiter.compare;

import org.bukkit.entity.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MobGroupCompare implements EntityCompare {
    private final String mobGroup;

    public MobGroupCompare(String mobGroup) {
        this.mobGroup = mobGroup;
    }

    @Override
    public boolean isSimilar(Entity entity) {
        return (getMobGroup(entity).equals(this.mobGroup));
    }

    @Contract(pure = true)
    public static @NotNull String getMobGroup(Entity entity) {
        // Determine the general group this mob belongs to.
        if (entity instanceof Animals) {
            // Chicken, Cow, MushroomCow, Ocelot, Pig, Sheep, Wolf
            return "ANIMAL";
        }

        if (entity instanceof Monster) {
            // Blaze, CaveSpider, Creeper, Enderman, Giant, PigZombie, Silverfish, Skeleton, Spider, Witch, Wither, Zombie
            return "MONSTER";
        }

        if (entity instanceof Ambient) {
            // Bat
            return "AMBIENT";
        }

        if (entity instanceof WaterMob) {
            // Squid, Fish
            return "WATER_MOB";
        }

        if (entity instanceof NPC) {
            // Villager
            return "NPC";
        }

        if (entity instanceof Vehicle) {
            //Boat, Minecart, TnT Minecart etc.
            return "VEHICLE";
        }

        // Anything else.
        return "OTHER";
    }
}
