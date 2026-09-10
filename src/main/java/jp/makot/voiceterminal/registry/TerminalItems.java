package jp.makot.voiceterminal.registry;

import jp.makot.voiceterminal.VoiceTerminal;
import jp.makot.voiceterminal.item.RecorderItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TerminalItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoiceTerminal.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoiceTerminal.MOD_ID);

    public static final DeferredHolder<Item, RecorderItem> RECORDER = ITEMS.register("recorder", () -> new RecorderItem(new Item.Properties(), false));
    public static final DeferredHolder<Item, RecorderItem> RECORDED_RECORDER = ITEMS.register("recorded_recorder", () -> new RecorderItem(new Item.Properties(), true));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TERMINAL_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.voiceterminal"))
            .icon(() -> RECORDER.get().getDefaultInstance())
            .displayItems((parameters, output) -> { output.accept(RECORDER.get()); output.accept(RECORDED_RECORDER.get()); })
            .build());

    private TerminalItems() {}
}
