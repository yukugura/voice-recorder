package jp.makot.voiceterminal.network;

import jp.makot.voiceterminal.VoiceTerminal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Server-to-client PCM stream for private playback by the recorder owner. */
public record RecorderPlaybackPayload(UUID recordingId, int index, int total, byte[] pcm) implements CustomPacketPayload {
    public static final Type<RecorderPlaybackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VoiceTerminal.MOD_ID, "recorder_playback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecorderPlaybackPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RecorderPlaybackPayload::recordingId,
            ByteBufCodecs.VAR_INT, RecorderPlaybackPayload::index,
            ByteBufCodecs.VAR_INT, RecorderPlaybackPayload::total,
            ByteBufCodecs.byteArray(RecorderUploadPayload.CHUNK_BYTES), RecorderPlaybackPayload::pcm,
            RecorderPlaybackPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
