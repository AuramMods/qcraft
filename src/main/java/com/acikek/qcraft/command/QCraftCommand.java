package com.acikek.qcraft.command;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.world.state.QBlockData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QCraftCommand implements Command<CommandSourceStack> {

    public static final String NAME = "qcraft";

    public static final Map<String, String> QUOTES = Map.of(
            "The cosmos is within us. We are a way for the universe to know itself.", "Carl Sagan",
            "Not only does God play dice but... he sometimes throws them where they cannot be seen.", "Stephen Hawking",
            "Not only is the Universe stranger than we think, it is stranger than we can think.", "Werner Heisenberg",
            "It is a quantum world. We are just living in it.", "Rodrigue Rizk",
            "You are my qubit! I shall never see you, but I know you do exist...", "Rodrigue Rizk",
            "Quantum mechanics makes absolutely no sense.", "Roger Penrose",
            "We must be clear that when it comes to atoms, language can be used only as in poetry.", "Niels Bohr"
    );

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        IModInfo metadata = ModList.get().getModContainerById(QCraft.ID).orElseThrow().getModInfo();
        MutableComponent title = Component.literal(metadata.getDisplayName())
                .withStyle(style -> style.withBold(true).withColor(0x1ec76a));
        Component version = Component.literal(" v" + metadata.getVersion())
                .withStyle(style -> style.withBold(false))
                .withStyle(ChatFormatting.GREEN);
        title.append(version);
        Map.Entry<String, String> quote = QUOTES.entrySet().stream().toList().get(context.getSource().getLevel().random.nextInt(QUOTES.size()));
        MutableComponent quoteText = Component.literal("\"" + quote.getKey() + "\"")
                .withStyle(ChatFormatting.GRAY);
        Component author = Component.literal(" - " + quote.getValue())
                .withStyle(style -> style.withItalic(true));
        quoteText.append(author);
        context.getSource().sendSuccess(() -> title, false);
        context.getSource().sendSuccess(() -> quoteText, false);
        return 0;
    }

    public enum ClearType {

        LOCATIONS("locations"),
        FREQUENCIES("frequencies"),
        ALL("all");

        public Component text;

        ClearType(String key) {
            text = Component.translatable("command.qcraft.clear." + key);
        }

        public int clear(QBlockData data) {
            return switch (this) {
                case LOCATIONS -> data.clearLocations();
                case FREQUENCIES -> data.clearFrequencies();
                case ALL -> data.reset();
            };
        }

        public static final String[] SUGGESTS = {
                "locations", "frequencies"
        };
    }

    public static class ClearTypeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (String type : ClearType.SUGGESTS) {
                builder.suggest(type);
            }
            return builder.buildFuture();
        }
    }

    public int clear(CommandContext<CommandSourceStack> context, String type) throws CommandSyntaxException {
        try {
            ClearType clearType = type != null ? ClearType.valueOf(type) : ClearType.ALL;
            QBlockData data = QBlockData.get(context.getSource().getLevel());
            int cleared = clearType.clear(data);
            Component text = Component.translatable("command.qcraft.clear.success", cleared, clearType.text);
            context.getSource().sendSuccess(() -> text, false);
            return 0;
        }
        catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Component.translatable("command.qcraft.clear.failure"));
        }
    }

    public int kit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = EntityArgument.getPlayer(context, "player");
        for (ItemStack base : QBlockItem.getPylonBases()) {
            player.addItem(base);
        }
        return 0;
    }

    public static void register(RegisterCommandsEvent event) {
        new QCraftCommand().register(event.getDispatcher());
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME)
                        .then(Commands.literal("clear")
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests(new ClearTypeSuggestionProvider())
                                        .executes(ctx -> clear(ctx, StringArgumentType.getString(ctx, "type").toUpperCase(java.util.Locale.ROOT))))
                                .executes(ctx -> clear(ctx, null)))
                        .then(Commands.literal("kit")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::kit)))
                .requires(source -> source.hasPermission(4))
                .executes(this));
    }
}
