
package net.sleepy.oreveins;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class OreVeinsMod implements ModInitializer {

    private static final Map<String, Block[]> ORES = new LinkedHashMap<>();

    static {
        add("coal", Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        add("iron", Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        add("copper", Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        add("gold", Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        add("redstone", Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        add("lapis", Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        add("diamond", Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        add("emerald", Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);

        ORES.put("nether_quartz",
                new Block[]{Blocks.NETHER_QUARTZ_ORE});
        ORES.put("nether_gold",
                new Block[]{Blocks.NETHER_GOLD_ORE});
        ORES.put("ancient_debris",
                new Block[]{Blocks.ANCIENT_DEBRIS});
    }

    private static void add(String name, Block stone, Block deepslate) {
        ORES.put(name, new Block[]{stone, deepslate});
    }

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                Commands.literal("spawn")
                    .then(Commands.literal("ore")
                        .then(Commands.argument("ore", StringArgumentType.word())
                            .suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(ORES.keySet(), builder))
                            .executes(context -> {

                                String oreName =
                                    StringArgumentType.getString(context, "ore");

                                Block[] ore = ORES.get(oreName);

                                if (ore == null) {
                                    context.getSource().sendFailure(
                                        Component.literal("Unknown ore!")
                                    );
                                    return 0;
                                }

                                ServerPlayer player =
                                    context.getSource().getPlayerOrException();

                                ServerLevel level = player.serverLevel();

                                int amount = generateVein(
                                    level,
                                    player.blockPosition().below(),
                                    ore
                                );

                                context.getSource().sendSuccess(
                                    () -> Component.literal(
                                        "Spawned " + amount + " " +
                                        oreName + " ore blocks!"
                                    ),
                                    true
                                );

                                return amount > 0 ? 1 : 0;
                            })
                        )
                    )
            );
        });
    }

    private static int generateVein(
        ServerLevel level,
        BlockPos center,
        Block[] ores
    ) {

        Random random = new Random();

        List<BlockPos> positions = new ArrayList<>();

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 0; y++) {
                for (int z = -2; z <= 2; z++) {

                    if (random.nextFloat() < 0.45f) {
                        positions.add(center.offset(x, y, z));
                    }
                }
            }
        }

        Collections.shuffle(positions);

        int placed = 0;

        for (BlockPos pos : positions) {

            if (!level.isInWorldBounds(pos)) continue;

            BlockState state = level.getBlockState(pos);

            Block selectedOre = null;

            if (ores.length == 1) {

                if (state.is(Blocks.NETHERRACK)) {
                    selectedOre = ores[0];
                }

            } else {

                if (state.is(Blocks.STONE)) {
                    selectedOre = ores[0];
                }

                if (state.is(Blocks.DEEPSLATE)) {
                    selectedOre = ores[1];
                }
            }

            if (selectedOre != null) {
                level.setBlock(pos, selectedOre.defaultBlockState(), 3);
                placed++;
            }

            if (placed >= 12) break;
        }

        return placed;
    }
}
