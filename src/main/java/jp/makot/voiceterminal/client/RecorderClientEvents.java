package jp.makot.voiceterminal.client;

import jp.makot.voiceterminal.VoiceTerminal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = VoiceTerminal.MOD_ID, value = Dist.CLIENT)
public final class RecorderClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RecorderClient.tick();
    }

    private RecorderClientEvents() { }
}
