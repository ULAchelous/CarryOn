package io.ula.carryon.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.NbtContents;

import java.util.Optional;

public record CarryOnStateData(Optional<CompoundTag> containerNbt,Optional<Boolean> isCarrying) {
    public static final Codec<CarryOnStateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    CompoundTag.CODEC.optionalFieldOf("container_bnt").forGetter(CarryOnStateData::containerNbt),
                    Codec.BOOL.optionalFieldOf("is_carrying").forGetter(CarryOnStateData::isCarrying)
            )
            .apply(instance,CarryOnStateData::new)

    );
}
