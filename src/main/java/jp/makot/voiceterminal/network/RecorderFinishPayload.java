package jp.makot.voiceterminal.network;

import jp.makot.voiceterminal.VoiceTerminal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record RecorderFinishPayload(UUID recordingId) implements CustomPacketPayload {
    public static final Type<RecorderFinishPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VoiceTerminal.MOD_ID, "recorder_finish"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecorderFinishPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, RecorderFinishPayload::recordingId, RecorderFinishPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
