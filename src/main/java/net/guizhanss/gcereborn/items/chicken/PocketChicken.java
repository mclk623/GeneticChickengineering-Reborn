package net.guizhanss.gcereborn.items.chicken;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

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
            Location location = b.getRelative(e.getClickedFace()).getLocation().toCenterLocation();
            ItemMeta meta = e.getItem().getItemMeta();
            JsonObject json = PersistentDataAPI.get(meta, Keys.POCKET_CHICKEN_ADAPTER, ADAPTER);
            
            // 生成鸡实体
            Chicken entity = b.getWorld().spawn(location, Chicken.class);
            
            try {
                if (json == null) {
                    // 【降级处理】数据损坏，设为普通鸡（野生DNA）
                    DNA defaultDna = new DNA();
                    PersistentDataAPI.setString(entity, Keys.CHICKEN_DNA, defaultDna.getStateString());
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "§l[系统] §e口袋鸡数据已损坏，降级为普通鸡");
                } else {
                    // 正常处理流程
                    ADAPTER.apply(entity, json);
                    
                    int[] dnaState = PersistentDataAPI.getIntArray(meta, Keys.POCKET_CHICKEN_DNA);
                    DNA dna = (dnaState != null) ? new DNA(dnaState) : new DNA();
                    PersistentDataAPI.setString(entity, Keys.CHICKEN_DNA, dna.getStateString());

                    // 设置显示名称
                    if (GeneticChickengineering.getConfigService().isDisplayResources() && dna.isKnown()) {
                        String name = ChatColor.WHITE + "(" + ChickenTypes.getDisplayName(dna.getTyping()) + ")";
                        if (json.has("_customName") && !json.get("_customName").isJsonNull()) {
                            name = json.get("_customName").getAsString() + " " + name;
                        }
                        entity.setCustomName(name);
                        entity.setCustomNameVisible(true);
                    }
                }
            } catch (Exception ex) {
                // 【异常降级】apply失败时，移除错误实体，生成新的普通鸡
                entity.remove();
                entity = b.getWorld().spawn(location, Chicken.class);
                
                DNA defaultDna = new DNA();
                PersistentDataAPI.setString(entity, Keys.CHICKEN_DNA, defaultDna.getStateString());
                
                e.getPlayer().sendMessage(ChatColor.RED + "§l[系统] §c口袋鸡数据异常，已降级为普通鸡");
                // 修复：使用 Bukkit.getLogger() 替代 inst()
                Bukkit.getLogger().warning("[GeneticChickengineering] PocketChicken 数据应用失败: " + ex.getMessage());
            }
            
            // 【关键修复】确保无论如何（成功、损坏、异常）都消耗物品
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
