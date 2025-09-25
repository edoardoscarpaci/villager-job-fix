package villagerjobfix.villagerjobfix;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;


import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

public class VillagerFindJobUtils {
    protected static final Set<RegistryKey<PointOfInterestType>> WORKSTATIONS = Set.of(
            PointOfInterestTypes.ARMORER,
            PointOfInterestTypes.BUTCHER,
            PointOfInterestTypes.LIBRARIAN,
            PointOfInterestTypes.LEATHERWORKER,
            PointOfInterestTypes.FISHERMAN,
            PointOfInterestTypes.WEAPONSMITH,
            PointOfInterestTypes.TOOLSMITH,
            PointOfInterestTypes.CARTOGRAPHER,
            PointOfInterestTypes.CLERIC,
            PointOfInterestTypes.FARMER,
            PointOfInterestTypes.FLETCHER,
            PointOfInterestTypes.MASON);

    protected static final Set<RegistryKey<VillagerProfession>> PROFESSIONS = Set.of(
            VillagerProfession.NONE,
            VillagerProfession.ARMORER,
            VillagerProfession.BUTCHER,
            VillagerProfession.CARTOGRAPHER,
            VillagerProfession.CLERIC,
            VillagerProfession.FARMER,
            VillagerProfession.FISHERMAN,
            VillagerProfession.FLETCHER,
            VillagerProfession.LEATHERWORKER,
            VillagerProfession.LIBRARIAN,
            VillagerProfession.MASON,
            VillagerProfession.NITWIT,
            VillagerProfession.SHEPHERD,
            VillagerProfession.TOOLSMITH,
            VillagerProfession.WEAPONSMITH);

    public static List<ProfessionPOI> findPOI(ServerWorld world, BlockPos center, int radius) {

        Function<? super RegistryKey, ? super RegistryEntry> getEntryFromRegistry = (registryKey) -> world.getRegistryManager()
                .getEntryOrThrow(registryKey);

        Stream<PointOfInterest> poiStream = world.getPointOfInterestStorage().getInSquare(
                poiType -> WORKSTATIONS.contains(poiType.getKey().orElse(null)),
                center,
                radius,
                PointOfInterestStorage.OccupationStatus.HAS_SPACE);

        
        return poiStream.map(poi -> {
            RegistryEntry<PointOfInterestType> poiEntry =(RegistryEntry<PointOfInterestType>) getEntryFromRegistry.apply(poi.getType().getKey().get());
            RegistryKey<VillagerProfession> professionKey = PROFESSIONS.stream()
                    .filter(profKey -> {
                        RegistryEntry<VillagerProfession> professionEntry = (RegistryEntry<VillagerProfession>) getEntryFromRegistry.apply(profKey);
                        return professionEntry.value().acquirableWorkstation().test(poiEntry);
                    })
                    .findFirst()
                    .orElse(null);

            RegistryEntry<VillagerProfession> profession = (RegistryEntry<VillagerProfession>) getEntryFromRegistry.apply(professionKey);
            
            return new ProfessionPOI(profession, poi);            
        }).toList();
            
    }
}
