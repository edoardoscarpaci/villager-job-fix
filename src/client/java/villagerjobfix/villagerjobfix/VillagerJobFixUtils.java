package villagerjobfix.villagerjobfix;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

public class VillagerJobFixUtils {
    protected static final Random random = Random.create();


    public static void fillRecipes(VillagerEntity villagerEntity, ServerWorld world) {
      VillagerData villagerData = villagerEntity.getVillagerData();
      RegistryKey<VillagerProfession> registryKey = (RegistryKey)villagerData.profession().getKey().orElse((RegistryKey<VillagerProfession>)null);
      if (registryKey != null) {
         Int2ObjectMap int2ObjectMap2;
         if (world.getEnabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<TradeOffers.Factory[]> int2ObjectMap = (Int2ObjectMap)TradeOffers.REBALANCED_PROFESSION_TO_LEVELED_TRADE.get(registryKey);
            int2ObjectMap2 = int2ObjectMap != null ? int2ObjectMap : (Int2ObjectMap)TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(registryKey);
         } else {
            int2ObjectMap2 = (Int2ObjectMap)TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(registryKey);
         }

         if (int2ObjectMap2 != null && !int2ObjectMap2.isEmpty()) {
            TradeOffers.Factory[] factorys = (TradeOffers.Factory[])int2ObjectMap2.get(villagerData.level());
            if (factorys != null) {
               TradeOfferList tradeOfferList = getNewTradeOfferList(factorys, 2,villagerEntity);
               villagerEntity.setOffers(tradeOfferList);
            }
         }
      }
    }

    protected static TradeOfferList getNewTradeOfferList(TradeOffers.Factory[] pool, int count,VillagerEntity villagerEntity) {
      ArrayList<TradeOffers.Factory> arrayList = Lists.newArrayList(pool);
      int i = 0;
      TradeOfferList recipeList = new TradeOfferList();

      while(i < count && !arrayList.isEmpty()) {
         TradeOffer tradeOffer = ((TradeOffers.Factory)arrayList.remove(random.nextInt(arrayList.size()))).create(villagerEntity, random);
         if (tradeOffer != null) {
            recipeList.add(tradeOffer);
            ++i;
         }
      }
      return recipeList;
   }
}
