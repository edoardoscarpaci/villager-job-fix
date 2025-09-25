package villagerjobfix.villagerjobfix;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;


public class VillagerChangeOffersCommand {
    
    protected static final String LIMIT_KEY = "limit";
    protected static final String WIDTH_KEY = "width";
    protected static final String HEIGHT_KEY = "height";
    protected static final String DEPTH_KEY = "depth";

    protected static final int DEFAULT_WIDTH = 16;
    protected static final int DEFAULT_HEIGHT = 16;
    protected static final int DEFAULT_DEPTH = 16;

    protected static final int DEFAULT_LIMIT = 1;
    public static final Logger LOGGER = LoggerFactory.getLogger("logger");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("villagerChangeOffers")
        .executes(ctx -> villagerChangeOffers(ctx.getSource()))
        
        .then(CommandManager.argument(LIMIT_KEY, IntegerArgumentType.integer())
            .executes(ctx -> villagerChangeOffers(ctx.getSource(),IntegerArgumentType.getInteger(ctx, LIMIT_KEY)))
            .then(CommandManager.argument(WIDTH_KEY, IntegerArgumentType.integer())
                .then(CommandManager.argument(HEIGHT_KEY, IntegerArgumentType.integer())
                    .then(CommandManager.argument(DEPTH_KEY, IntegerArgumentType.integer())
                        .executes(ctx -> villagerChangeOffers(ctx.getSource(),IntegerArgumentType.getInteger(ctx, LIMIT_KEY), IntegerArgumentType.getInteger(ctx, WIDTH_KEY),IntegerArgumentType.getInteger(ctx, HEIGHT_KEY),IntegerArgumentType.getInteger(ctx, DEPTH_KEY)))
                        )
                    )
                )
            )
        );
    }
    
    public static int villagerChangeOffers(ServerCommandSource source) {
        return villagerChangeOffers(source, DEFAULT_LIMIT,DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_DEPTH);
    }

    public static int villagerChangeOffers(ServerCommandSource source,int limit) {
        return villagerChangeOffers(source, limit,DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_DEPTH);
    }

    public static int villagerChangeOffers(ServerCommandSource source,int limit, int width, int height, int depth) {
        if (limit <= 0) {
            source.sendError(Text.literal("Limit must be strictly positive").formatted(Formatting.RED));
            return 0; // Failure
        }
        
        ServerPlayerEntity  player = source.getPlayer(); // Get the player who executed the command

        ServerWorld world =  source.getWorld();
        List<VillagerEntity> notTradedVillager = world.getEntitiesByClass(VillagerEntity.class, Box.of(player.getPos(), width, height, depth), villager -> villager.getExperience() == 0).subList(0, limit);;
        
        for (VillagerEntity villagerEntity : notTradedVillager) {
            VillagerJobFixUtils.fillRecipes(villagerEntity,world);
            villagerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false));

        }
     
        return Command.SINGLE_SUCCESS; // Success
    }


}
