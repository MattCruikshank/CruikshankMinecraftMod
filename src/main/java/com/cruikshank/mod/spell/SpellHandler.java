package com.cruikshank.mod.spell;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class SpellHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getRawText().trim().toLowerCase();
        switch (message) {
            case "lumos":
            case "nox":
            case "alohomora":
            case "expelliarmus":
            case "flipendo":
                break;
            default:
                return;
        }

        ServerPlayer player = event.getPlayer();
        if (!isHoldingStick(player)) {
            LOGGER.info("CRUIKSHANK: {} cast attempted but no stick held", message);
            return;
        }

        switch (message) {
            case "lumos" -> handleLumos(player);
            case "nox" -> handleNox(player);
            case "alohomora" -> handleAlohomora(player);
            case "expelliarmus" -> handleExpelliarmus(player);
            case "flipendo" -> handleFlipendo(player);
        }
    }

    private boolean isHoldingStick(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.STICK)
                || player.getItemInHand(InteractionHand.OFF_HAND).is(Items.STICK);
    }

    private BlockHitResult raycast(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookEnd = eyePos.add(player.getLookAngle().scale(200));
        return player.serverLevel().clip(new ClipContext(
                eyePos, lookEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    private void handleLumos(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hitResult = raycast(player);

        if (hitResult.getType() == HitResult.Type.MISS) {
            LOGGER.info("CRUIKSHANK: Lumos — looking at sky, no torch placed");
            return;
        }

        Direction face = hitResult.getDirection();
        BlockPos targetPos = hitResult.getBlockPos().relative(face);

        if (!level.getBlockState(targetPos).isAir()) {
            LOGGER.info("CRUIKSHANK: Lumos — target position not air, no torch placed");
            return;
        }

        if (face == Direction.DOWN) {
            LOGGER.info("CRUIKSHANK: Lumos — hit bottom face, can't hang torch");
            return;
        }

        BlockState torchState;
        if (face == Direction.UP) {
            torchState = Blocks.TORCH.defaultBlockState();
        } else {
            torchState = Blocks.WALL_TORCH.defaultBlockState()
                    .setValue(WallTorchBlock.FACING, face);
        }

        level.setBlockAndUpdate(targetPos, torchState);
        LOGGER.info("CRUIKSHANK: Lumos! Torch placed at {} on {} face", targetPos, face);
    }

    private void handleNox(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hitResult = raycast(player);

        if (hitResult.getType() == HitResult.Type.MISS) {
            LOGGER.info("CRUIKSHANK: Nox — looking at sky, nothing to remove");
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.LAVA)) {
            LOGGER.info("CRUIKSHANK: Nox — lava is excluded, not removing");
            return;
        }

        if (state.getLightEmission(level, pos) > 0) {
            level.removeBlock(pos, false);
            LOGGER.info("CRUIKSHANK: Nox! Removed {} at {}", state.getBlock(), pos);
        }
    }

    private EntityHitResult raycastEntity(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        double range = 50.0;
        Vec3 endPos = eyePos.add(lookDir.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                range * range
        );
    }

    private void handleAlohomora(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hitResult = raycast(player);

        if (hitResult.getType() == HitResult.Type.MISS) {
            LOGGER.info("CRUIKSHANK: Alohomora — looking at sky, nothing to open");
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof DoorBlock) {
            boolean wasOpen = state.getValue(BlockStateProperties.OPEN);
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, !wasOpen));

            // Toggle the other half of the door to keep both in sync
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.getBlock() instanceof DoorBlock) {
                level.setBlockAndUpdate(otherPos, otherState.setValue(BlockStateProperties.OPEN, !wasOpen));
            }
            LOGGER.info("CRUIKSHANK: Alohomora! Toggled door at {}", pos);
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            boolean wasOpen = state.getValue(BlockStateProperties.OPEN);
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, !wasOpen));
            LOGGER.info("CRUIKSHANK: Alohomora! Toggled trapdoor at {}", pos);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            boolean wasOpen = state.getValue(BlockStateProperties.OPEN);
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, !wasOpen));
            LOGGER.info("CRUIKSHANK: Alohomora! Toggled fence gate at {}", pos);
        } else {
            LOGGER.info("CRUIKSHANK: Alohomora — {} is not a door, trapdoor, or fence gate", state.getBlock());
        }
    }

    private void handleExpelliarmus(ServerPlayer player) {
        EntityHitResult hitResult = raycastEntity(player);

        if (hitResult == null) {
            LOGGER.info("CRUIKSHANK: Expelliarmus — no entity in sight");
            return;
        }

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity living)) {
            LOGGER.info("CRUIKSHANK: Expelliarmus — {} is not a living entity", target.getType().getDescriptionId());
            return;
        }

        ItemStack held = living.getMainHandItem();
        if (held.isEmpty()) {
            LOGGER.info("CRUIKSHANK: Expelliarmus — {} has nothing in main hand", living.getName().getString());
            return;
        }

        living.spawnAtLocation(held.copy());

        if (living instanceof Mob mob) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        } else if (living instanceof Player targetPlayer) {
            targetPlayer.getInventory().setItem(targetPlayer.getInventory().selected, ItemStack.EMPTY);
        }

        LOGGER.info("CRUIKSHANK: Expelliarmus! Disarmed {} — dropped {}", living.getName().getString(), held.getItem());
    }

    private void handleFlipendo(ServerPlayer player) {
        EntityHitResult hitResult = raycastEntity(player);

        if (hitResult == null) {
            LOGGER.info("CRUIKSHANK: Flipendo — no entity in sight");
            return;
        }

        Entity target = hitResult.getEntity();
        Vec3 lookDir = player.getLookAngle();
        Vec3 knockback = new Vec3(lookDir.x * 2.0, 0.4, lookDir.z * 2.0);
        target.setDeltaMovement(knockback);
        target.hurtMarked = true;

        LOGGER.info("CRUIKSHANK: Flipendo! Launched {} away", target.getName().getString());
    }
}
