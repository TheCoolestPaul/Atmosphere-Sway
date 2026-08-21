package com.atmossway.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoliageRegistrarTest {
    private static final ResourceLocation FLAX = id("supplementaries", "flax");
    private static final ResourceLocation WILD_FLAX = id("supplementaries", "wild_flax");
    private static final ResourceLocation DUNE_GRASS = id("immersive_weathering", "dune_grass");
    private static final ResourceLocation HANGING_ROOTS =
            id("immersive_weathering", "hanging_roots_wall");

    @Test
    void definesExactImmersiveWeatheringPlantsAndBehaviors() {
        assertEquals(
                List.of(
                        "immersive_weathering:dune_grass",
                        "immersive_weathering:frosty_grass",
                        "immersive_weathering:frosty_fern",
                        "immersive_weathering:weeds",
                        "immersive_weathering:ivy",
                        "immersive_weathering:moss",
                        "immersive_weathering:hanging_roots_wall"
                ),
                FoliageRegistrar.IMMERSIVE_WEATHERING.plants().stream()
                        .map(plant -> plant.id().toString())
                        .toList()
        );
        assertEquals(
                List.of(
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.STANDARD,
                        FoliageRegistrar.PlantBehavior.HANGING
                ),
                FoliageRegistrar.IMMERSIVE_WEATHERING.plants().stream()
                        .map(FoliageRegistrar.OptionalPlant::behavior)
                        .toList()
        );
    }

    @Test
    void registersEachPresentPlantWithItsAssignedBehavior() {
        Map<ResourceLocation, Object> blocks = new HashMap<>();
        for (FoliageRegistrar.OptionalPlantGroup group : FoliageRegistrar.OPTIONAL_PLANT_GROUPS) {
            for (FoliageRegistrar.OptionalPlant plant : group.plants()) {
                blocks.put(plant.id(), new Object());
            }
        }
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                () -> { },
                id -> Optional.ofNullable(blocks.get(id)),
                block -> true,
                standard::add,
                hanging::add
        );

        assertTrue(result.executed());
        assertEquals(2, result.registeredPlants("supplementaries"));
        assertEquals(7, result.registeredPlants("immersive_weathering"));
        assertEquals(8, standard.size());
        assertEquals(List.of(blocks.get(HANGING_ROOTS)), hanging);
    }

    @Test
    void safelySkipsAllOptionalPlantsWhenModsAreAbsent() {
        AtomicInteger initializations = new AtomicInteger();
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                initializations::incrementAndGet,
                id -> Optional.<Object>empty(),
                block -> true,
                standard::add,
                hanging::add
        );

        assertTrue(result.executed());
        assertEquals(1, initializations.get());
        assertEquals(0, result.registeredPlants("supplementaries"));
        assertEquals(0, result.registeredPlants("immersive_weathering"));
        assertTrue(standard.isEmpty());
        assertTrue(hanging.isEmpty());
    }

    @Test
    void handlesPartialAvailabilityAndAirEntriesPerMod() {
        Object flax = new Object();
        Object duneGrass = new Object();
        Object air = new Object();
        Map<ResourceLocation, Object> blocks = Map.of(
                FLAX, flax,
                WILD_FLAX, air,
                DUNE_GRASS, duneGrass,
                HANGING_ROOTS, air
        );
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                () -> { },
                id -> Optional.ofNullable(blocks.get(id)),
                block -> block != air,
                standard::add,
                hanging::add
        );

        assertEquals(1, result.registeredPlants("supplementaries"));
        assertEquals(1, result.registeredPlants("immersive_weathering"));
        assertEquals(List.of(flax, duneGrass), standard);
        assertTrue(hanging.isEmpty());
    }

    @Test
    void registrationIsIdempotent() {
        Object flax = new Object();
        AtomicInteger initializations = new AtomicInteger();
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();
        FoliageRegistrar.RegistrationGate gate = new FoliageRegistrar.RegistrationGate();

        FoliageRegistrar.RegistrationResult first = gate.register(
                initializations::incrementAndGet,
                id -> id.equals(FLAX) ? Optional.of(flax) : Optional.empty(),
                block -> true,
                standard::add,
                hanging::add
        );
        FoliageRegistrar.RegistrationResult second = gate.register(
                initializations::incrementAndGet,
                id -> Optional.of(new Object()),
                block -> true,
                standard::add,
                hanging::add
        );

        assertTrue(first.executed());
        assertFalse(second.executed());
        assertEquals(0, second.registeredPlants("supplementaries"));
        assertEquals(0, second.registeredPlants("immersive_weathering"));
        assertEquals(1, initializations.get());
        assertEquals(List.of(flax), standard);
        assertTrue(hanging.isEmpty());
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
