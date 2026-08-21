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
    private static final ResourceLocation TOMATOES = id("farmersdelight", "tomatoes");
    private static final ResourceLocation TOMATOES_ON_ROPE = id("farmersdelight", "tomatoes_on_rope");
    private static final ResourceLocation RICE = id("farmersdelight", "rice");
    private static final ResourceLocation RICE_PANICLES = id("farmersdelight", "rice_panicles");

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
    void definesExactFarmersDelightPlantsAndBehaviors() {
        assertEquals(
                List.of(
                        "farmersdelight:brown_mushroom_colony",
                        "farmersdelight:red_mushroom_colony",
                        "farmersdelight:sandy_shrub",
                        "farmersdelight:wild_cabbages",
                        "farmersdelight:wild_onions",
                        "farmersdelight:wild_tomatoes",
                        "farmersdelight:wild_carrots",
                        "farmersdelight:wild_potatoes",
                        "farmersdelight:wild_beetroots",
                        "farmersdelight:wild_rice",
                        "farmersdelight:cabbages",
                        "farmersdelight:onions",
                        "farmersdelight:budding_tomatoes",
                        "farmersdelight:tomatoes",
                        "farmersdelight:tomatoes_on_rope",
                        "farmersdelight:rice",
                        "farmersdelight:rice_panicles"
                ),
                FoliageRegistrar.FARMERS_DELIGHT.plantIds().stream()
                        .map(ResourceLocation::toString)
                        .toList()
        );
        assertEquals(
                13,
                FoliageRegistrar.FARMERS_DELIGHT.plants().stream()
                        .filter(plant -> plant.behavior() == FoliageRegistrar.PlantBehavior.STANDARD)
                        .count()
        );
        assertEquals(
                List.of(TOMATOES, TOMATOES_ON_ROPE),
                FoliageRegistrar.FARMERS_DELIGHT.plants().stream()
                        .filter(plant -> plant.behavior() == FoliageRegistrar.PlantBehavior.GROWING)
                        .map(FoliageRegistrar.OptionalPlant::id)
                        .toList()
        );
        assertEquals(
                List.of(new FoliageRegistrar.LinkedPlantPair(RICE, RICE_PANICLES)),
                FoliageRegistrar.FARMERS_DELIGHT.linkedPlantPairs()
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
        List<Object> growing = new ArrayList<>();
        List<List<Object>> linked = new ArrayList<>();

        for (FoliageRegistrar.LinkedPlantPair pair : FoliageRegistrar.FARMERS_DELIGHT.linkedPlantPairs()) {
            blocks.put(pair.lowerId(), new Object());
            blocks.put(pair.upperId(), new Object());
        }

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                () -> { },
                id -> Optional.ofNullable(blocks.get(id)),
                block -> true,
                standard::add,
                hanging::add,
                growing::add,
                (lower, upper) -> linked.add(List.of(lower, upper))
        );

        assertTrue(result.executed());
        assertEquals(2, result.registeredPlants("supplementaries"));
        assertEquals(7, result.registeredPlants("immersive_weathering"));
        assertEquals(17, result.registeredPlants("farmersdelight"));
        assertEquals(21, standard.size());
        assertEquals(List.of(blocks.get(HANGING_ROOTS)), hanging);
        assertEquals(List.of(blocks.get(TOMATOES), blocks.get(TOMATOES_ON_ROPE)), growing);
        assertEquals(List.of(List.of(blocks.get(RICE), blocks.get(RICE_PANICLES))), linked);
    }

    @Test
    void safelySkipsAllOptionalPlantsWhenModsAreAbsent() {
        AtomicInteger initializations = new AtomicInteger();
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();
        List<Object> growing = new ArrayList<>();
        List<List<Object>> linked = new ArrayList<>();

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                initializations::incrementAndGet,
                id -> Optional.<Object>empty(),
                block -> true,
                standard::add,
                hanging::add,
                growing::add,
                (lower, upper) -> linked.add(List.of(lower, upper))
        );

        assertTrue(result.executed());
        assertEquals(1, initializations.get());
        assertEquals(0, result.registeredPlants("supplementaries"));
        assertEquals(0, result.registeredPlants("immersive_weathering"));
        assertEquals(0, result.registeredPlants("farmersdelight"));
        assertTrue(standard.isEmpty());
        assertTrue(hanging.isEmpty());
        assertTrue(growing.isEmpty());
        assertTrue(linked.isEmpty());
    }

    @Test
    void handlesPartialAvailabilityAndAirEntriesPerMod() {
        Object flax = new Object();
        Object duneGrass = new Object();
        Object air = new Object();
        Object rice = new Object();
        Object tomatoes = new Object();
        Map<ResourceLocation, Object> blocks = Map.of(
                FLAX, flax,
                WILD_FLAX, air,
                DUNE_GRASS, duneGrass,
                HANGING_ROOTS, air,
                RICE, rice,
                RICE_PANICLES, air,
                TOMATOES, tomatoes
        );
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();
        List<Object> growing = new ArrayList<>();
        List<List<Object>> linked = new ArrayList<>();

        FoliageRegistrar.RegistrationResult result = new FoliageRegistrar.RegistrationGate().register(
                () -> { },
                id -> Optional.ofNullable(blocks.get(id)),
                block -> block != air,
                standard::add,
                hanging::add,
                growing::add,
                (lower, upper) -> linked.add(List.of(lower, upper))
        );

        assertEquals(1, result.registeredPlants("supplementaries"));
        assertEquals(1, result.registeredPlants("immersive_weathering"));
        assertEquals(2, result.registeredPlants("farmersdelight"));
        assertEquals(List.of(flax, duneGrass, rice), standard);
        assertTrue(hanging.isEmpty());
        assertEquals(List.of(tomatoes), growing);
        assertTrue(linked.isEmpty());
    }

    @Test
    void registrationIsIdempotent() {
        Object flax = new Object();
        AtomicInteger initializations = new AtomicInteger();
        List<Object> standard = new ArrayList<>();
        List<Object> hanging = new ArrayList<>();
        List<Object> growing = new ArrayList<>();
        List<List<Object>> linked = new ArrayList<>();
        FoliageRegistrar.RegistrationGate gate = new FoliageRegistrar.RegistrationGate();

        FoliageRegistrar.RegistrationResult first = gate.register(
                initializations::incrementAndGet,
                id -> id.equals(FLAX) ? Optional.of(flax) : Optional.empty(),
                block -> true,
                standard::add,
                hanging::add,
                growing::add,
                (lower, upper) -> linked.add(List.of(lower, upper))
        );
        FoliageRegistrar.RegistrationResult second = gate.register(
                initializations::incrementAndGet,
                id -> Optional.of(new Object()),
                block -> true,
                standard::add,
                hanging::add,
                growing::add,
                (lower, upper) -> linked.add(List.of(lower, upper))
        );

        assertTrue(first.executed());
        assertFalse(second.executed());
        assertEquals(0, second.registeredPlants("supplementaries"));
        assertEquals(0, second.registeredPlants("immersive_weathering"));
        assertEquals(0, second.registeredPlants("farmersdelight"));
        assertEquals(1, initializations.get());
        assertEquals(List.of(flax), standard);
        assertTrue(hanging.isEmpty());
        assertTrue(growing.isEmpty());
        assertTrue(linked.isEmpty());
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
