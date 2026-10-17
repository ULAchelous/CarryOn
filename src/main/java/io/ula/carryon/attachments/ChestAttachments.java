package io.ula.carryon.attachments;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public class ChestAttachments {
    public static final AttachmentType<Boolean> PLAYER_CNT_DATA = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("carry_on","lock_mark")
            ,playerCntDataBuilder -> playerCntDataBuilder
                    .persistent(Codec.BOOL)
                    .initializer(() -> false)
                    .copyOnDeath()
    );
}
