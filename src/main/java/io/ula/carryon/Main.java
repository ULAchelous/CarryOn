package io.ula.carryon;

import com.mojang.brigadier.tree.RootCommandNode;
import io.ula.carryon.attachments.CarryOnStateData;
import io.ula.carryon.attachments.CustomAttachments;
import io.ula.carryon.commands.CarryOnCmd;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class Main implements ModInitializer {


    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {
            RootCommandNode<CommandSourceStack> rootNode = commandDispatcher.getRoot();
            rootNode.addChild(CarryOnCmd.carryOnCmd);
        });

        UseBlockCallback.EVENT.register(((player, level, interactionHand, blockHitResult) -> {
            if(!level.isClientSide()){
                BlockEntity blockEntity = level.getBlockEntity(blockHitResult.getBlockPos());
                BlockPos pos = blockHitResult.getBlockPos();
                CarryOnStateData data = player.getAttachedOrCreate(CustomAttachments.CARRY_ON_STATE);
                if(blockEntity instanceof ChestBlockEntity chestBlockEntity){
                    if(chestBlockEntity.getAttachedOrCreate(CustomAttachments.LOCK_STATE)) {
                        player.sendSystemMessage(Component.literal("无法操作：容器处于搬运中锁定状态").withStyle(ChatFormatting.RED));
                        return InteractionResult.SUCCESS;
                    }else if(data.readyCarry().isPresent() && data.readyCarry().get()){
                        CompoundTag nbt = chestBlockEntity.saveWithoutMetadata(level.registryAccess());
                        chestBlockEntity.clearContent();
                        chestBlockEntity.setAttached(CustomAttachments.LOCK_STATE,true);
                        ClientboundSoundPacket soundPacket = new ClientboundSoundPacket(Holder.direct(SoundEvents.CHEST_LOCKED),SoundSource.BLOCKS,pos.getX(),pos.getY(),pos.getZ(),1.0f,1.0f,1);
                        ((ServerPlayer)player).connection.send(soundPacket);
                        player.sendSystemMessage(Component.literal(String.format("CARRY_ON: 搬起了位于(%d,%d,%d) %s的容器",pos.getX(),pos.getY(),pos.getZ(),level.dimension().identifier().toString())).withStyle(ChatFormatting.GREEN));
                        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(Component.literal("你正搬起一个容器！").withStyle(ChatFormatting.YELLOW));
                        ((ServerPlayer)player).connection.send(packet);
                        player.setAttached(CustomAttachments.CARRY_ON_STATE,new CarryOnStateData(Optional.of(nbt),Optional.of(true),Optional.of(false),Optional.of(pos),Optional.of(level.dimension().identifier())));
                        return InteractionResult.SUCCESS;
                    }
                }
                if(data.isCarrying().isPresent() && data.isCarrying().get()){
                    BlockPos abovePos = new BlockPos(pos.getX(),pos.getY() + 1,pos.getZ());
                    if(level.getBlockState(abovePos).isAir()){
                        level.setBlock(abovePos, Blocks.CHEST.getStateForPlacement(BlockPlaceContext.at(new BlockPlaceContext(player,interactionHand,new ItemStack(Items.CHEST),blockHitResult),pos,player.getDirection())),3);
                        ChestBlockEntity newEntity = (ChestBlockEntity) level.getBlockEntity(abovePos);
                        ValueInput valueInput = TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data.containerNbt().get());
                        newEntity.loadWithComponents(valueInput);
                        ServerLevel level1 = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,data.levelId().get()));
                        if(level1.getBlockEntity(data.originalPos().get()) instanceof  ChestBlockEntity originalChest){
                            for(int i = 0; i < originalChest.getContainerSize(); i++){
                                ItemStack stack = originalChest.removeItemNoUpdate(i);
                                if(stack.isEmpty()) continue;
                                ItemStack leftover = HopperBlockEntity.addItem(null, newEntity, stack, null);
                                if(!leftover.isEmpty()){
                                    Containers.dropItemStack(level1, originalChest.getBlockPos().getX() + 0.5, originalChest.getBlockPos().getY() + 0.5, originalChest.getBlockPos().getZ() + 0.5, leftover);
                                }
                            }
                            originalChest.clearContent();
                            level1.setBlock(data.originalPos().get(),Blocks.AIR.defaultBlockState(),3);
                        }
                        ClientboundSoundPacket soundPacket = new ClientboundSoundPacket(Holder.direct(SoundEvents.WOOD_PLACE),SoundSource.BLOCKS,pos.getX(),pos.getY(),pos.getZ(),1.0f,1.0f,1);
                        ((ServerPlayer)player).connection.send(soundPacket);
                        player.setAttached(CustomAttachments.CARRY_ON_STATE,new CarryOnStateData(Optional.empty(),Optional.of(false),Optional.of(false),Optional.empty(),Optional.empty()));
                    }
                }
            }
            return InteractionResult.PASS;
        }));

        PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos, blockState, blockEntity) -> {
            if(blockEntity instanceof ChestBlockEntity chestBlockEntity){
                if(chestBlockEntity.getAttachedOrCreate(CustomAttachments.LOCK_STATE)){
                    player.sendSystemMessage(Component.literal("无法操作：容器处于搬运中锁定状态").withStyle(ChatFormatting.RED));
                    return false;
                }
            }
            return true;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((serverGamePacketListener, minecraftServer) -> {
            ServerPlayer player = serverGamePacketListener.player;
            CarryOnStateData data = player.getAttachedOrCreate(CustomAttachments.CARRY_ON_STATE);
            if(data.isCarrying().isPresent() && data.isCarrying().get()) {
                ServerLevel level = minecraftServer.getLevel(ResourceKey.create(Registries.DIMENSION, data.levelId().get()));
                if (data.originalPos().isPresent() && level.getBlockEntity(data.originalPos().get()) instanceof ChestBlockEntity chestBlockEntity) {
                    if (data.containerNbt().isPresent()) {
                        ValueInput valueInput = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.containerNbt().get());
                        chestBlockEntity.loadWithComponents(valueInput);
                    }
                    chestBlockEntity.setAttached(CustomAttachments.LOCK_STATE, false);
                }
                player.setAttached(CustomAttachments.CARRY_ON_STATE, new CarryOnStateData(Optional.empty(), Optional.of(false), Optional.of(false), Optional.empty(), Optional.empty()));
            }
            });
    }
}
