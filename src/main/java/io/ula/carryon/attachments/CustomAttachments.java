package io.ula.carryon.attachments;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class CustomAttachments {
    public static final AttachmentType<Boolean> LOCK_STATE = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("carry_on","lock_mark")
            ,dataBuilder -> dataBuilder
                    .persistent(Codec.BOOL)
                    .initializer(() -> false)
    );
    public static final AttachmentType<CarryOnStateData> CARRY_ON_STATE = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("carry_on","carry_on_state"),
            carryOnStateDataBuilder -> carryOnStateDataBuilder
                    .persistent(CarryOnStateData.CODEC)
                    .initializer(() -> new CarryOnStateData(Optional.empty(),Optional.of(false),Optional.of(false),Optional.empty(),Optional.empty()))
            );
}
