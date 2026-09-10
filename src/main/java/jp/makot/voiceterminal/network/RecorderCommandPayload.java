package jp.makot.voiceterminal.network;

import jp.makot.voiceterminal.VoiceTerminal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Server-authoritative instruction for the physical recorder. */
public record RecorderCommandPayload(Action action, UUID recordingId) implements CustomPacketPayload {
    public static final Type<RecorderCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VoiceTerminal.MOD_ID, "recorder_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecorderCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), RecorderCommandPayload::action,
            UUIDUtil.STREAM_CODEC, RecorderCommandPayload::recordingId,
            RecorderCommandPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Action { START, STOP }
}
