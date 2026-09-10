package jp.makot.voiceterminal.network;

import jp.makot.voiceterminal.VoiceTerminal;
import jp.makot.voiceterminal.storage.RecorderAudioStore;
import jp.makot.voiceterminal.item.RecorderItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = VoiceTerminal.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TerminalNetworking {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(RecorderCommandPayload.TYPE, RecorderCommandPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (payload.action() == RecorderCommandPayload.Action.START) jp.makot.voiceterminal.client.RecorderClient.commandStart(payload.recordingId());
                    else jp.makot.voiceterminal.client.RecorderClient.commandStop(payload.recordingId());
                });
        event.registrar("1").playToServer(RecorderUploadPayload.TYPE, RecorderUploadPayload.STREAM_CODEC,
                (payload, context) -> RecorderAudioStore.append(((ServerPlayer) context.player()).getUUID(), payload.recordingId(), payload.pcm()));
        event.registrar("1").playToServer(RecorderFinishPayload.TYPE, RecorderFinishPayload.STREAM_CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    if (RecorderAudioStore.finish(player.getUUID(), payload.recordingId()) && RecorderItem.complete(player, payload.recordingId()))
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.voiceterminal.recorder_finished"), true);
                });
        event.registrar("1").playToClient(RecorderPlaybackPayload.TYPE, RecorderPlaybackPayload.STREAM_CODEC,
                (payload, context) -> jp.makot.voiceterminal.client.RecorderClient.receivePlayback(payload));
    }
    public static void sendRecorderPlayback(ServerPlayer player, java.util.UUID id) {
        try {
            byte[] pcm = RecorderAudioStore.read(id);
            int total = (pcm.length + RecorderUploadPayload.CHUNK_BYTES - 1) / RecorderUploadPayload.CHUNK_BYTES;
            for (int index = 0; index < total; index++) {
                int start = index * RecorderUploadPayload.CHUNK_BYTES;
                int end = Math.min(pcm.length, start + RecorderUploadPayload.CHUNK_BYTES);
                PacketDistributor.sendToPlayer(player, new RecorderPlaybackPayload(id, index, total, java.util.Arrays.copyOfRange(pcm, start, end)));
            }
        } catch (java.io.IOException ignored) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.voiceterminal.recorder_missing"), true);
        }
    }
    private TerminalNetworking() { }
}
