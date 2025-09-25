package villagerjobfix.villagerjobfix;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterest;

public record ProfessionPOI (RegistryEntry<VillagerProfession> profession, PointOfInterest poi) {


}
