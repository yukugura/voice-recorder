package jp.makot.voiceterminal.registry;

import jp.makot.voiceterminal.VoiceTerminal;
import jp.makot.voiceterminal.recipe.RecorderDuplicationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TerminalRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, VoiceTerminal.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RecorderDuplicationRecipe>> RECORDER_DUPLICATION =
            SERIALIZERS.register("recorder_duplication", () -> new SimpleCraftingRecipeSerializer<>(RecorderDuplicationRecipe::new));

    private TerminalRecipes() { }
}
