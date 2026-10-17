package io.ula.carryon.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;
import java.util.logging.Level;

public record CarryOnStateData(Optional<CompoundTag> containerNbt,Optional<Boolean> isCarrying,Optional<Boolean> readyCarry,Optional<BlockPos> originalPos,Optional<Identifier> levelId) {
    public static final Codec<CarryOnStateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    CompoundTag.CODEC.optionalFieldOf("container_nbt").forGetter(CarryOnStateData::containerNbt),
                    Codec.BOOL.optionalFieldOf("is_carrying").forGetter(CarryOnStateData::isCarrying),
                    Codec.BOOL.optionalFieldOf("ready_carrying").forGetter(CarryOnStateData::readyCarry),
                    BlockPos.CODEC.optionalFieldOf("original_pos").forGetter(CarryOnStateData::originalPos),
                    Identifier.CODEC.optionalFieldOf("levelId").forGetter(CarryOnStateData::levelId)
            )
            .apply(instance,CarryOnStateData::new)

    );
}
