package jp.makot.voiceterminal.item;

import jp.makot.voiceterminal.network.RecorderCommandPayload;
import jp.makot.voiceterminal.registry.TerminalItems;
import jp.makot.voiceterminal.storage.RecorderAudioStore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/** A physical, single-use voice recorder. The raw sound is always server-owned. */
public final class RecorderItem extends Item {
    public static final String ID = "recorder_audio_id";
    private static final String RECORDING = "recorder_recording";
    private static final String RECORDED = "recorder_recorded";
    private final boolean recordedVariant;

    public RecorderItem(Properties properties, boolean recordedVariant) {
        super(properties.stacksTo(1));
        this.recordedVariant = recordedVariant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if ((recordedVariant || tag.getBoolean(RECORDED)) && tag.hasUUID(ID)) {
            jp.makot.voiceterminal.network.TerminalNetworking.sendRecorderPlayback(serverPlayer, tag.getUUID(ID));
            return InteractionResultHolder.consume(stack);
        }
        if (tag.getBoolean(RECORDING) && tag.hasUUID(ID)) {
            PacketDistributor.sendToPlayer(serverPlayer, new RecorderCommandPayload(RecorderCommandPayload.Action.STOP, tag.getUUID(ID)));
            return InteractionResultHolder.consume(stack);
        }
        UUID id = UUID.randomUUID();
        if (!RecorderAudioStore.begin(serverPlayer.getUUID(), id)) return InteractionResultHolder.fail(stack);
        tag.putUUID(ID, id);
        tag.putBoolean(RECORDING, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PacketDistributor.sendToPlayer(serverPlayer, new RecorderCommandPayload(RecorderCommandPayload.Action.START, id));
        serverPlayer.displayClientMessage(Component.translatable("message.voiceterminal.recorder_started"), true);
        return InteractionResultHolder.consume(stack);
    }

    public static boolean complete(ServerPlayer player, UUID id) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof RecorderItem recorder) || recorder.recordedVariant) continue;
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.hasUUID(ID) || !tag.getUUID(ID).equals(id)) continue;
            tag.remove(RECORDING);
            tag.putBoolean(RECORDED, true);
            player.getInventory().setItem(slot, toRecordedStack(tag));
            return true;
        }
        return false;
    }

    public static boolean isRecorded(ItemStack stack) {
        return stack.is(TerminalItems.RECORDED_RECORDER.get()) ||
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(RECORDED);
    }

    public static ItemStack toRecordedStack(ItemStack source) {
        return toRecordedStack(source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    private static ItemStack toRecordedStack(CompoundTag tag) {
        ItemStack result = new ItemStack(TerminalItems.RECORDED_RECORDER.get());
        tag.remove(RECORDING);
        tag.putBoolean(RECORDED, true);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }
}
