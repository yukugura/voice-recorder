package jp.makot.voiceterminal.network;

import jp.makot.voiceterminal.VoiceTerminal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** A bounded PCM fragment; chunking keeps a one-minute recording below packet limits. */
public record RecorderUploadPayload(UUID recordingId, byte[] pcm) implements CustomPacketPayload {
    public static final int CHUNK_BYTES = 8_192;
    public static final Type<RecorderUploadPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VoiceTerminal.MOD_ID, "recorder_upload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecorderUploadPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RecorderUploadPayload::recordingId,
            ByteBufCodecs.byteArray(CHUNK_BYTES), RecorderUploadPayload::pcm,
            RecorderUploadPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
