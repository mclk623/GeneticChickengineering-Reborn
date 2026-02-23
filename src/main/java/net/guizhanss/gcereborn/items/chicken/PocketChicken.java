package net.guizhanss.gcereborn.items.chicken;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonObject;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;

import net.guizhanss.gcereborn.GeneticChickengineering;
import net.guizhanss.gcereborn.core.adapters.AnimalsAdapter;
import net.guizhanss.gcereborn.core.genetics.DNA;
import net.guizhanss.gcereborn.utils.Keys;

public class PocketChicken extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable, DistinctiveItem {

    public static final AnimalsAdapter<Chicken> ADAPTER = new AnimalsAdapter<>(Chicken.class);

    public PocketChicken(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

@Override
@Nonnull
public ItemUseHandler getItemHandler() {
    return e -> {
        e.cancel();

        Optional<Block> block = e.getClickedBlock();
        if (block.isEmpty()) {
            return;
        }

        Block b = block.get();
        Location location = b.getRelative(e.getClickedFace()).getLocation();
        
        ItemMeta meta = e.getItem().getItemMeta();
        JsonObject json = PersistentDataAPI.get(meta, Keys.POCKET_CHICKEN_ADAPTER, ADAPTER);
        
        // 修复点1：如果数据损坏，直接消耗物品并返回（可选生成普通鸡）
        if (json == null) {
            // 如果你想给玩家一个"安慰奖"：生成一只无基因的鸡
            // Chicken entity = b.getWorld().spawn(location.toCenterLocation(), Chicken.class);
            
            // 直接删除损坏的物品
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                ItemUtils.consumeItem(e.getItem(), false);
            }
            e.getPlayer().sendMessage(ChatColor.RED + "这个口袋鸡已损坏，已被移除");
            return;
        }

        // 正常流程：生成鸡并应用数据
        Chicken entity = b.getWorld().spawn(location.toCenterLocation(), Chicken.class);
        
        try {
            ADAPTER.apply(entity, json);
            int[] dnaState = PersistentDataAPI.getIntArray(meta, Keys.POCKET_CHICKEN_DNA);
            DNA dna = (dnaState != null) ? new DNA(dnaState) : new DNA();

            String dss = dna.getStateString();
            PersistentDataAPI.setString(entity, Keys.CHICKEN_DNA, dss);

            if (GeneticChickengineering.getConfigService().isDisplayResources() && dna.isKnown()) {
                String name = ChatColor.WHITE + "(" + ChickenTypes.getDisplayName(dna.getTyping()) + ")";
                if (!json.get("_customName").isJsonNull()) {
                    name = json.get("_customName").getAsString() + " " + name;
                }
                entity.setCustomName(name);
                entity.setCustomNameVisible(true);
            }
        } catch (Exception ex) {
            // 修复点2：万一apply失败，移除生成的鸡并消耗物品
            entity.remove();
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                ItemUtils.consumeItem(e.getItem(), false);
            }
            e.getPlayer().sendMessage(ChatColor.RED + "口袋鸡数据应用失败，物品已移除");
            return;
        }

        // 修复点3：确保消耗物品（移到finally或每个分支都处理）
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemUtils.consumeItem(e.getItem(), false);
        }
    };
}

    @Override
    @ParametersAreNonnullByDefault
    public boolean canStack(ItemMeta meta1, ItemMeta meta2) {
        return meta1.getPersistentDataContainer().equals(meta2.getPersistentDataContainer());
    }
}
