package com.cruikshank.mod.entity.goal;

import com.cruikshank.mod.block.EmeraldChestBlockEntity;
import com.cruikshank.mod.entity.EmeraldGolem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;

public class TradeWithVillagerGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEARCH_RANGE = 16;
    private static final int COOLDOWN_TICKS = 100;

    private final EmeraldGolem golem;

    private enum Phase { IDLE, WALKING_TO_VILLAGER, TRADING, RETURNING }

    private Phase phase = Phase.IDLE;
    private Villager targetVillager;
    private MerchantOffer targetOffer;
    private int cooldown = 0;
    private int stuckTimer = 0;

    public TradeWithVillagerGoal(EmeraldGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return golem.getChestPos() != null && phase == Phase.IDLE && findTradeOpportunity();
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == Phase.IDLE) return false;
        if (targetVillager != null && !targetVillager.isAlive()) {
            returnItemsToChest();
            reset();
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        stuckTimer = 0;
    }

    @Override
    public void stop() {
        reset();
    }

    @Override
    public void tick() {
        stuckTimer++;
        switch (phase) {
            case WALKING_TO_VILLAGER -> tickWalkToVillager();
            case TRADING -> tickTrade();
            case RETURNING -> tickReturn();
            default -> {}
        }
    }

    private boolean findTradeOpportunity() {
        BlockPos chestPos = golem.getChestPos();
        if (!(golem.level().getBlockEntity(chestPos) instanceof EmeraldChestBlockEntity chest)) return false;

        List<Villager> villagers = golem.level().getEntitiesOfClass(Villager.class,
                golem.getBoundingBox().inflate(SEARCH_RANGE),
                v -> v.isAlive() && !v.isBaby() && v.getOffers() != null && !v.getOffers().isEmpty());

        for (Villager villager : villagers) {
            for (MerchantOffer offer : villager.getOffers()) {
                if (offer.isOutOfStock()) continue;
                if (!offer.getResult().is(Items.EMERALD)) continue;

                ItemStack costA = offer.getCostA();
                int available = countItemInChest(chest, costA);
                if (available >= costA.getCount()) {
                    // Check costB if present
                    if (!offer.getCostB().isEmpty()) {
                        int availableB = countItemInChest(chest, offer.getCostB());
                        if (availableB < offer.getCostB().getCount()) continue;
                    }

                    targetVillager = villager;
                    targetOffer = offer;

                    // Take cost items from chest
                    ItemStack taken = takeItemFromChest(chest, costA, costA.getCount());
                    golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, taken);

                    phase = Phase.WALKING_TO_VILLAGER;
                    stuckTimer = 0;
                    LOGGER.info("CRUIKSHANK: Golem taking {} to trade with villager", taken);
                    return true;
                }
            }
        }
        return false;
    }

    private void tickWalkToVillager() {
        if (targetVillager == null || !targetVillager.isAlive()) {
            returnItemsToChest();
            reset();
            return;
        }

        golem.getLookControl().setLookAt(targetVillager, 30.0F, 30.0F);
        double dist = golem.distanceToSqr(targetVillager);

        if (dist < 4.0) { // within 2 blocks
            phase = Phase.TRADING;
            stuckTimer = 0;
            return;
        }

        if (stuckTimer % 20 == 0) {
            golem.getNavigation().moveTo(targetVillager, 1.0);
        }

        if (stuckTimer > 400) { // stuck for 20 seconds, give up
            LOGGER.info("CRUIKSHANK: Golem stuck walking to villager, returning items");
            returnItemsToChest();
            reset();
        }
    }

    private void tickTrade() {
        if (targetOffer == null || targetOffer.isOutOfStock()) {
            returnItemsToChest();
            reset();
            return;
        }

        ItemStack held = golem.getMainHandItem();
        ItemStack costA = targetOffer.getCostA();

        if (held.getCount() >= costA.getCount()) {
            // Execute trade
            held.shrink(costA.getCount());
            if (held.isEmpty()) {
                golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            // Use the offer
            targetOffer.increaseUses();

            // Give emeralds to golem
            ItemStack result = targetOffer.getResult().copy();
            golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, result);

            // Trigger villager trade effects
            targetVillager.notifyTrade(targetOffer);

            LOGGER.info("CRUIKSHANK: Golem traded for {}", result);

            phase = Phase.RETURNING;
            stuckTimer = 0;
        } else {
            // Not enough items somehow, return what we have
            returnItemsToChest();
            reset();
        }
    }

    private void tickReturn() {
        BlockPos chestPos = golem.getChestPos();
        if (chestPos == null) {
            reset();
            return;
        }

        double dist = golem.distanceToSqr(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5);

        if (dist < 4.0) { // within 2 blocks of chest
            depositToChest();
            reset();
            return;
        }

        if (stuckTimer % 20 == 0) {
            golem.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5, 1.0);
        }

        if (stuckTimer > 400) {
            LOGGER.info("CRUIKSHANK: Golem stuck returning, dropping items");
            reset();
        }
    }

    private void depositToChest() {
        BlockPos chestPos = golem.getChestPos();
        if (!(golem.level().getBlockEntity(chestPos) instanceof EmeraldChestBlockEntity chest)) return;

        ItemStack held = golem.getMainHandItem();
        if (!held.isEmpty()) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack slot = chest.getItem(i);
                if (slot.isEmpty()) {
                    chest.setItem(i, held.copy());
                    golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    chest.setChanged();
                    LOGGER.info("CRUIKSHANK: Golem deposited {} in chest", held);
                    return;
                }
                if (ItemStack.isSameItemSameTags(slot, held) && slot.getCount() < slot.getMaxStackSize()) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    int toAdd = Math.min(space, held.getCount());
                    slot.grow(toAdd);
                    held.shrink(toAdd);
                    if (held.isEmpty()) {
                        golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        chest.setChanged();
                        LOGGER.info("CRUIKSHANK: Golem deposited emeralds in chest");
                        return;
                    }
                }
            }
            chest.setChanged();
        }
    }

    private void returnItemsToChest() {
        BlockPos chestPos = golem.getChestPos();
        if (chestPos == null) return;
        if (!(golem.level().getBlockEntity(chestPos) instanceof EmeraldChestBlockEntity chest)) return;

        ItemStack held = golem.getMainHandItem();
        if (!held.isEmpty()) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack slot = chest.getItem(i);
                if (slot.isEmpty()) {
                    chest.setItem(i, held.copy());
                    golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    chest.setChanged();
                    return;
                }
                if (ItemStack.isSameItemSameTags(slot, held) && slot.getCount() < slot.getMaxStackSize()) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    int toAdd = Math.min(space, held.getCount());
                    slot.grow(toAdd);
                    held.shrink(toAdd);
                    if (held.isEmpty()) {
                        golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        chest.setChanged();
                        return;
                    }
                }
            }
            chest.setChanged();
        }
    }

    private int countItemInChest(EmeraldChestBlockEntity chest, ItemStack target) {
        int count = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.is(target.getItem())) {
                count += slot.getCount();
            }
        }
        return count;
    }

    private ItemStack takeItemFromChest(EmeraldChestBlockEntity chest, ItemStack target, int amount) {
        ItemStack result = new ItemStack(target.getItem(), 0);
        int remaining = amount;
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.is(target.getItem())) {
                int toTake = Math.min(remaining, slot.getCount());
                slot.shrink(toTake);
                remaining -= toTake;
                result.grow(toTake);
                if (slot.isEmpty()) {
                    chest.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        chest.setChanged();
        return result;
    }

    private void reset() {
        phase = Phase.IDLE;
        targetVillager = null;
        targetOffer = null;
        cooldown = COOLDOWN_TICKS;
        stuckTimer = 0;
        golem.getNavigation().stop();
    }
}
