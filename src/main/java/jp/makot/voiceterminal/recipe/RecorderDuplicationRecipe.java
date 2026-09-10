package jp.makot.voiceterminal.recipe;

import jp.makot.voiceterminal.item.RecorderItem;
import jp.makot.voiceterminal.registry.TerminalItems;
import jp.makot.voiceterminal.registry.TerminalRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Keeps one source recorder and converts one empty recorder into a linked copy. */
public final class RecorderDuplicationRecipe extends CustomRecipe {
    public RecorderDuplicationRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int empty = 0;
        int recorded = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(TerminalItems.RECORDER.get()) && !RecorderItem.isRecorded(stack)) empty++;
            else if (RecorderItem.isRecorded(stack)) recorded++;
            else return false;
        }
        return empty == 1 && recorded == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return source(input).map(RecorderItem::toRecordedStack).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (RecorderItem.isRecorded(stack)) remaining.set(slot, RecorderItem.toRecordedStack(stack));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return new ItemStack(TerminalItems.RECORDED_RECORDER.get()); }

    @Override
    public RecipeSerializer<?> getSerializer() { return TerminalRecipes.RECORDER_DUPLICATION.get(); }

    private static java.util.Optional<ItemStack> source(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (RecorderItem.isRecorded(stack)) return java.util.Optional.of(stack);
        }
        return java.util.Optional.empty();
    }
}
