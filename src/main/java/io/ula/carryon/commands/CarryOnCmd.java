package io.ula.carryon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.ula.carryon.attachments.CarryOnStateData;
import io.ula.carryon.attachments.CustomAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class CarryOnCmd {
    private static LiteralArgumentBuilder<CommandSourceStack> carryOnCmdBuilder = Commands.literal("carry-on").executes(context -> {
       ServerPlayer executor = context.getSource().getPlayer();
        CarryOnStateData data = executor.getAttachedOrCreate(CustomAttachments.CARRY_ON_STATE);
       if(data.isCarrying().get()){
           executor.sendSystemMessage(Component.literal("无法操作：已有正在搬运的容器").withStyle(ChatFormatting.RED));
           return 0;
       }else{
           executor.sendSystemMessage(Component.literal("CARRY_ON: 进入搬运模式").withStyle(ChatFormatting.GREEN));
           executor.sendSystemMessage(Component.literal("CARRY_ON: 你下一个右键的容器将被搬起").withStyle(ChatFormatting.GREEN));
           executor.setAttached(CustomAttachments.CARRY_ON_STATE,new CarryOnStateData(data.containerNbt(),data.isCarrying(), Optional.of(true),Optional.empty(),Optional.empty()));
       }
        return 0;
    }).requires(commandSourceStack -> commandSourceStack.isPlayer());

    public static LiteralCommandNode<CommandSourceStack> carryOnCmd = carryOnCmdBuilder.build();
}
