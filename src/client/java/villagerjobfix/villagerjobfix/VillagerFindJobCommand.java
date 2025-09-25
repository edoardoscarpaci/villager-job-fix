package villagerjobfix.villagerjobfix;

import java.util.Comparator;
import java.util.List;

import org.joml.Math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;

import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;


public class VillagerFindJobCommand {
    
    protected static final String LIMIT_KEY = "limit";
    protected static final String RADIUS_KEY = "radius";

    protected static final int DEFAULT_RADIUS = 16;
    protected static final int DEFAULT_LIMIT = 1;

    public static final Logger LOGGER = LoggerFactory.getLogger("logger");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(
        CommandManager.literal("villagerFindJob")
        // Base command
        .executes(ctx -> villagerFindJob(ctx.getSource()))

        // With 1 argument: limit
        .then(CommandManager.argument(LIMIT_KEY, IntegerArgumentType.integer())
            .executes(ctx -> villagerFindJob(
                ctx.getSource(),
                IntegerArgumentType.getInteger(ctx, LIMIT_KEY)
            ))

            // With 2 arguments: limit + radius
            .then(CommandManager.argument(RADIUS_KEY, IntegerArgumentType.integer())
                .executes(ctx -> villagerFindJob(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, LIMIT_KEY),
                    IntegerArgumentType.getInteger(ctx, RADIUS_KEY)
                ))
            )
        )
        );
        
    }
    
    public static int villagerFindJob(ServerCommandSource source) {
        return villagerFindJob(source, DEFAULT_LIMIT,DEFAULT_RADIUS);
    }

    public static int villagerFindJob(ServerCommandSource source,int limit) {
        return villagerFindJob(source, limit,DEFAULT_RADIUS);
    }

    public static int villagerFindJob(ServerCommandSource source,int limit, int radius) {
        if (limit <= 0) {
            source.sendError(Text.literal("Limit must be strictly positive").formatted(Formatting.RED));
            return 0; // Failure
        }
        
        ServerPlayerEntity  player = source.getPlayer(); // Get the player who executed the command

        ServerWorld world =  source.getWorld();
        List<VillagerEntity> notTradedVillager = world.getEntitiesByClass(VillagerEntity.class, Box.of(player.getPos(), radius,radius,radius), villager -> villager.getExperience() == 0);
        notTradedVillager.sort(Comparator.comparingDouble(villager ->player.getPos().distanceTo(villager.getPos()) ));
        
        if (notTradedVillager.isEmpty()) {
            source.sendError(Text.literal("No villager found").formatted(Formatting.RED));
            return 0; // Failure
        }

        notTradedVillager = notTradedVillager.subList(0, Math.min(limit, notTradedVillager.size()));
        
        List<ProfessionPOI> pois = VillagerFindJobUtils.findPOI(world, player.getBlockPos(), radius);

        for (VillagerEntity villagerEntity : notTradedVillager) {
            
            ProfessionPOI closestPoi = findClosestPoi(villagerEntity, pois);
            if (closestPoi == null) {
                source.sendFeedback(
                    () -> Text.literal("No POI found for villager at " + villagerEntity.getBlockPos().toShortString()),
                    false
                    );
                    continue;
                }
            
            BlockPos poiPos = closestPoi.poi().getPos();
            
            source.sendFeedback(
                () -> Text.literal("Villager at " + villagerEntity.getBlockPos().toShortString() + 
                             " closest POI: " + poiPos.toShortString()),
                false
            );
            
            world.spawnParticles(
                ParticleTypes.GLOW_SQUID_INK,
                poiPos.getX() + 0.5,  // center of block
                poiPos.getY() + 1.0,  // just above block
                poiPos.getZ() + 0.5,
                20,   // count of particles
                0.25, // spread X
                0.25, // spread Y
                0.25, // spread Z
                0.05  // speed
            );

            villagerEntity.getBrain().remember(
                MemoryModuleType.JOB_SITE,
                GlobalPos.create(world.getRegistryKey(), poiPos)
            );
            VillagerData resetData = villagerEntity.getVillagerData().withProfession(world.getRegistryManager().getEntryOrThrow(VillagerProfession.NONE));
            villagerEntity.setVillagerData(resetData);

            // 3. Reinitialize brain after resetting
            villagerEntity.reinitializeBrain(world);

            // 4. Assign the desired profession
            VillagerData newData = villagerEntity.getVillagerData().withProfession(closestPoi.profession());
            villagerEntity.setVillagerData(newData);

            villagerEntity.reinitializeBrain(world);
            
            villagerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false));
        }
     
        return Command.SINGLE_SUCCESS; // Success
    }


    private static ProfessionPOI findClosestPoi(VillagerEntity villager, List<ProfessionPOI> pois) {
        return pois.stream()
            .min(Comparator.comparingDouble(pos -> pos.poi().getPos().getSquaredDistance(villager.getPos())))
            .orElse(null);

    }

   
}
