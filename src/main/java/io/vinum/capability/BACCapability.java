package io.vinum.capability;

import io.vinum.common.Defines;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Serializable attachments replace the old Forge capability implementation. */
public final class BACCapability {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Defines.MODID);
    // Default attachment copying preserves dimension transfers, but resets on death.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BAC>> BAC_STATE =
            ATTACHMENT_TYPES.register("bac", () -> AttachmentType.serializable(BAC::new).build());
    private BACCapability() {}
}
