package jp.makot.voiceterminal;

import com.mojang.logging.LogUtils;
import jp.makot.voiceterminal.registry.TerminalItems;
import jp.makot.voiceterminal.registry.TerminalRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point for the handheld recorder items and their recipes. */
@Mod(VoiceTerminal.MOD_ID)
public final class VoiceTerminal {
    public static final String MOD_ID = "voiceterminal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoiceTerminal(IEventBus modBus) {
        TerminalItems.ITEMS.register(modBus);
        TerminalItems.TABS.register(modBus);
        TerminalRecipes.SERIALIZERS.register(modBus);
    }

}
