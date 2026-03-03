package com.cruikshank.mod.spell;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class SpellHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getRawText().trim();
        if (message.equalsIgnoreCase("lumos") || message.equalsIgnoreCase("nox")) {
            ServerPlayer player = event.getPlayer();
            if (!isHoldingStick(player)) {
                LOGGER.info("CRUIKSHANK: {} cast attempted but no stick held", message);
                return;
            }
            if (message.equalsIgnoreCase("lumos")) {
                handleLumos(player);
            } else {
                handleNox(player);
            }
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
}
