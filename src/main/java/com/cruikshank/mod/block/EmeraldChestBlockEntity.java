package com.cruikshank.mod.block;

import com.cruikshank.mod.CruikshankMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class EmeraldChestBlockEntity extends RandomizableContainerBlockEntity {

    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public EmeraldChestBlockEntity(BlockPos pos, BlockState state) {
        super(CruikshankMod.EMERALD_CHEST_BE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.cruikshank.emerald_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
}
