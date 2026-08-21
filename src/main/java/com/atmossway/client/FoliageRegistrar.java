package com.atmossway.client;

import com.atmossway.AtmosSway;
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.api.behavior.BehaviorKey;
import com.github.razorplay01.sway.client.behavior.BuiltinBehaviors;
import com.github.razorplay01.sway.client.behavior.multiblock.GrowingVineMultiblockBehavior;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final class FoliageRegistrar {
    private static final BehaviorKey LINKED_PLANT_BEHAVIOR_KEY =
            BehaviorKey.create(AtmosSway.MOD_ID, "linked_two_block_plant");

    static final OptionalPlantGroup SUPPLEMENTARIES = new OptionalPlantGroup(
            "supplementaries",
            "Supplementaries",
            List.of(
                    standardPlant("supplementaries", "flax"),
                    standardPlant("supplementaries", "wild_flax")
            ),
            List.of()
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
            ),
            List.of()
    );

    static final OptionalPlantGroup FARMERS_DELIGHT = new OptionalPlantGroup(
            "farmersdelight",
            "Farmer's Delight",
            List.of(
                    standardPlant("farmersdelight", "brown_mushroom_colony"),
                    standardPlant("farmersdelight", "red_mushroom_colony"),
                    standardPlant("farmersdelight", "sandy_shrub"),
                    standardPlant("farmersdelight", "wild_cabbages"),
                    standardPlant("farmersdelight", "wild_onions"),
                    standardPlant("farmersdelight", "wild_tomatoes"),
                    standardPlant("farmersdelight", "wild_carrots"),
                    standardPlant("farmersdelight", "wild_potatoes"),
                    standardPlant("farmersdelight", "wild_beetroots"),
                    standardPlant("farmersdelight", "wild_rice"),
                    standardPlant("farmersdelight", "cabbages"),
                    standardPlant("farmersdelight", "onions"),
                    standardPlant("farmersdelight", "budding_tomatoes"),
                    growingPlant("farmersdelight", "tomatoes"),
                    growingPlant("farmersdelight", "tomatoes_on_rope")
            ),
            List.of(new LinkedPlantPair(
                    ResourceLocation.fromNamespaceAndPath("farmersdelight", "rice"),
                    ResourceLocation.fromNamespaceAndPath("farmersdelight", "rice_panicles")
            ))
    );

    static final List<OptionalPlantGroup> OPTIONAL_PLANT_GROUPS = List.of(
            SUPPLEMENTARIES,
            IMMERSIVE_WEATHERING,
            FARMERS_DELIGHT
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
                FoliageRegistrar::registerHangingPlant,
                FoliageRegistrar::registerGrowingPlant,
                FoliageRegistrar::registerLinkedPlantPair
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

    private static void registerGrowingPlant(Block block) {
        SwayAPI.register(block, 1.0F);
        GrowingVineMultiblockBehavior.addBlock(block);
        SwayAPI.setBlockPipeline(block, List.of(
                BuiltinBehaviors.ENTITY_COLLISION_KEY,
                BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                BuiltinBehaviors.GROWING_VINE_MULTIBLOCK_KEY,
                BuiltinBehaviors.GROWING_VINE_DEFORMATION_KEY,
                BuiltinBehaviors.VINE_CLIMB_TENSION_KEY,
                BuiltinBehaviors.multiplierKey(1.0F)
        ));
    }

    private static void registerLinkedPlantPair(Block lowerBlock, Block upperBlock) {
        SwayAPI.register(lowerBlock, 1.0F);
        SwayAPI.register(upperBlock, 1.0F);
        SwayAPI.registerBehavior(
                LINKED_PLANT_BEHAVIOR_KEY,
                new LinkedTwoBlockPlantBehavior(lowerBlock, upperBlock)
        );
        List<BehaviorKey> pipeline = List.of(
                BuiltinBehaviors.ENTITY_COLLISION_KEY,
                BuiltinBehaviors.PROXIMITY_FORCE_KEY,
                LINKED_PLANT_BEHAVIOR_KEY,
                BuiltinBehaviors.multiplierKey(1.0F)
        );
        SwayAPI.setBlockPipeline(lowerBlock, pipeline);
        SwayAPI.setBlockPipeline(upperBlock, pipeline);
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

    private static OptionalPlant growingPlant(String namespace, String path) {
        return new OptionalPlant(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                PlantBehavior.GROWING
        );
    }

    enum PlantBehavior {
        STANDARD,
        HANGING,
        GROWING
    }

    record OptionalPlant(ResourceLocation id, PlantBehavior behavior) {
    }

    record LinkedPlantPair(ResourceLocation lowerId, ResourceLocation upperId) {
    }

    record OptionalPlantGroup(
            String modId,
            String displayName,
            List<OptionalPlant> plants,
            List<LinkedPlantPair> linkedPlantPairs
    ) {
        OptionalPlantGroup {
            plants = List.copyOf(plants);
            linkedPlantPairs = List.copyOf(linkedPlantPairs);
        }

        List<ResourceLocation> plantIds() {
            List<ResourceLocation> ids = new java.util.ArrayList<>(plants.size() + linkedPlantPairs.size() * 2);
            plants.forEach(plant -> ids.add(plant.id()));
            linkedPlantPairs.forEach(pair -> {
                ids.add(pair.lowerId());
                ids.add(pair.upperId());
            });
            return List.copyOf(ids);
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
                Consumer<T> registerHangingBlock,
                Consumer<T> registerGrowingBlock,
                BiConsumer<T, T> registerLinkedPlantPair
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

                    switch (plant.behavior()) {
                        case STANDARD -> registerStandardBlock.accept(block.get());
                        case HANGING -> registerHangingBlock.accept(block.get());
                        case GROWING -> registerGrowingBlock.accept(block.get());
                    }
                    registeredPlants++;
                }
                for (LinkedPlantPair pair : group.linkedPlantPairs()) {
                    Optional<T> lowerBlock = eligibleBlock(pair.lowerId(), blockLookup, isEligible);
                    Optional<T> upperBlock = eligibleBlock(pair.upperId(), blockLookup, isEligible);
                    if (lowerBlock.isPresent() && upperBlock.isPresent()) {
                        registerLinkedPlantPair.accept(lowerBlock.get(), upperBlock.get());
                        registeredPlants += 2;
                    } else {
                        lowerBlock.ifPresent(registerStandardBlock);
                        upperBlock.ifPresent(registerStandardBlock);
                        registeredPlants += (lowerBlock.isPresent() ? 1 : 0) + (upperBlock.isPresent() ? 1 : 0);
                    }
                }
                registeredByMod.put(group.modId(), registeredPlants);
            }

            registered = true;
            return new RegistrationResult(true, registeredByMod);
        }

        private static <T> Optional<T> eligibleBlock(
                ResourceLocation id,
                Function<ResourceLocation, Optional<T>> blockLookup,
                Predicate<T> isEligible
        ) {
            return blockLookup.apply(id).filter(isEligible);
        }
    }
}
