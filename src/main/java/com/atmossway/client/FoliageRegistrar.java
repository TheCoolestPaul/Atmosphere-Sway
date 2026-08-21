package com.atmossway.client;

import com.atmossway.AtmosSway;
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.client.behavior.BuiltinBehaviors;
import com.github.razorplay01.sway.client.behavior.multiblock.HangingVineMultiblockBehavior;
import com.github.razorplay01.sway.registry.SwayRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final class FoliageRegistrar {
    static final OptionalPlantGroup SUPPLEMENTARIES = new OptionalPlantGroup(
            "supplementaries",
            "Supplementaries",
            List.of(
                    standardPlant("supplementaries", "flax"),
                    standardPlant("supplementaries", "wild_flax")
            )
    );

    static final OptionalPlantGroup IMMERSIVE_WEATHERING = new OptionalPlantGroup(
            "immersive_weathering",
            "Immersive Weathering",
            List.of(
                    standardPlant("immersive_weathering", "dune_grass"),
                    standardPlant("immersive_weathering", "frosty_grass"),
                    standardPlant("immersive_weathering", "frosty_fern"),
                    standardPlant("immersive_weathering", "weeds"),
                    standardPlant("immersive_weathering", "ivy"),
                    standardPlant("immersive_weathering", "moss"),
                    hangingPlant("immersive_weathering", "hanging_roots_wall")
            )
    );

    static final List<OptionalPlantGroup> OPTIONAL_PLANT_GROUPS = List.of(
            SUPPLEMENTARIES,
            IMMERSIVE_WEATHERING
    );

    private static final RegistrationGate REGISTRATION = new RegistrationGate();

    private FoliageRegistrar() {
    }

    static void register() {
        RegistrationResult result = REGISTRATION.register(
                SwayRegistry::initialize,
                BuiltInRegistries.BLOCK::getOptional,
                block -> block != Blocks.AIR,
                block -> SwayAPI.register(block, 1.0F),
                FoliageRegistrar::registerHangingPlant
        );
        if (!result.executed()) {
            return;
        }

        AtmosSway.LOGGER.info("Initialized SWAY's registry with {} directly registered blocks",
                SwayAPI.getRegistry().size());
        for (OptionalPlantGroup group : OPTIONAL_PLANT_GROUPS) {
            int registeredPlants = result.registeredPlants(group.modId());
            if (registeredPlants > 0) {
                AtmosSway.LOGGER.info("Registered {} optional {} plants with SWAY",
                        registeredPlants, group.displayName());
            }
        }
    }

    private static void registerHangingPlant(Block block) {
        SwayAPI.register(block, 1.0F);
        HangingVineMultiblockBehavior.addBlock(block);
        SwayAPI.setBlockPipeline(block, List.of(
                BuiltinBehaviors.ENTITY_COLLISION_KEY,
                BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                BuiltinBehaviors.HANGING_VINE_MULTIBLOCK_KEY,
                BuiltinBehaviors.HANGING_VINE_DEFORMATION_KEY,
                BuiltinBehaviors.VINE_CLIMB_TENSION_KEY,
                BuiltinBehaviors.multiplierKey(1.0F)
        ));
    }

    private static OptionalPlant standardPlant(String namespace, String path) {
        return new OptionalPlant(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                PlantBehavior.STANDARD
        );
    }

    private static OptionalPlant hangingPlant(String namespace, String path) {
        return new OptionalPlant(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                PlantBehavior.HANGING
        );
    }

    enum PlantBehavior {
        STANDARD,
        HANGING
    }

    record OptionalPlant(ResourceLocation id, PlantBehavior behavior) {
    }

    record OptionalPlantGroup(String modId, String displayName, List<OptionalPlant> plants) {
        OptionalPlantGroup {
            plants = List.copyOf(plants);
        }
    }

    record RegistrationResult(boolean executed, Map<String, Integer> registeredByMod) {
        RegistrationResult {
            registeredByMod = Map.copyOf(registeredByMod);
        }

        int registeredPlants(String modId) {
            return registeredByMod.getOrDefault(modId, 0);
        }
    }

    static final class RegistrationGate {
        private boolean registered;

        synchronized <T> RegistrationResult register(
                Runnable initializeSway,
                Function<ResourceLocation, Optional<T>> blockLookup,
                Predicate<T> isEligible,
                Consumer<T> registerStandardBlock,
                Consumer<T> registerHangingBlock
        ) {
            if (registered) {
                return new RegistrationResult(false, Map.of());
            }

            initializeSway.run();
            Map<String, Integer> registeredByMod = new LinkedHashMap<>();
            for (OptionalPlantGroup group : OPTIONAL_PLANT_GROUPS) {
                int registeredPlants = 0;
                for (OptionalPlant plant : group.plants()) {
                    Optional<T> block = blockLookup.apply(plant.id());
                    if (block.isEmpty() || !isEligible.test(block.get())) {
                        continue;
                    }

                    if (plant.behavior() == PlantBehavior.HANGING) {
                        registerHangingBlock.accept(block.get());
                    } else {
                        registerStandardBlock.accept(block.get());
                    }
                    registeredPlants++;
                }
                registeredByMod.put(group.modId(), registeredPlants);
            }

            registered = true;
            return new RegistrationResult(true, registeredByMod);
        }
    }
}
