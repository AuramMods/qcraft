package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.AutomatedObserver;
import com.acikek.qcraft.block.QuantumOre;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.item.Goggles;
import com.acikek.qcraft.recipe.EntangledPairRecipe;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.QuantumComputerData;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@GameTestHolder(QCraft.ID)
@PrefixGameTestTemplate(false)
public class QCraftGameTests {

    private static final String AREA = "test_area";
    private static final BlockPos FIRST_STATION = new BlockPos(5, 1, 5);
    private static final BlockPos SECOND_STATION = new BlockPos(16, 1, 5);

    @GameTest(template = AREA)
    public static void recipesAndAdvancementDataLoad(GameTestHelper test) {
        Player player = fakePlayer(test, FIRST_STATION);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        CraftingRecipe qBlock = recipe(test, "quantum_block");
        CraftingRecipe entangle = recipe(test, "entangled_pair");
        CraftingRecipe refill = recipe(test, "coolant_cell_refill");
        test.assertFalse(qBlock.matches(new TransientCraftingContainer(player.inventoryMenu, 2, 2), test.getLevel()),
                "The six-face recipe requires a crafting table");
        grid.setItem(4, new ItemStack(com.acikek.qcraft.item.Items.OBSERVATION_ESSENCE.get()));
        grid.setItem(0, new ItemStack(Blocks.STONE));
        grid.setItem(1, new ItemStack(Blocks.GOLD_BLOCK));
        test.assertTrue(qBlock.matches(grid, test.getLevel()), "The six-face recipe should accept distinct blocks");
        ItemStack crafted = qBlock.assemble(grid, test.getLevel().registryAccess());
        test.assertTrue(crafted.is(com.acikek.qcraft.block.Blocks.OBSERVER_DEPENDENT_BLOCK.get().asItem()), "Wrong qBlock item");
        String[] faces = QBlockItem.getFaces(crafted);
        test.assertTrue(faces[0].equals("minecraft:stone") && faces[1].equals("minecraft:gold_block")
                        && faces[2].equals("minecraft:air"), "Face IDs and empty slots must survive crafting");

        grid.clearContent();
        grid.setItem(2, crafted.copy());
        grid.setItem(3, new ItemStack(com.acikek.qcraft.item.Items.ENTANGLEMENT_ESSENCE.get()));
        grid.setItem(4, crafted.copy());
        test.assertFalse(entangle.matches(grid, test.getLevel()), "Entanglement must stay in one row");
        grid.clearContent();
        grid.setItem(3, crafted.copy());
        grid.setItem(4, new ItemStack(com.acikek.qcraft.item.Items.ENTANGLEMENT_ESSENCE.get()));
        grid.setItem(5, crafted.copy());
        test.assertTrue(entangle.matches(grid, test.getLevel()), "Two matching qBlocks should entangle");
        ItemStack pair = entangle.assemble(grid, test.getLevel().registryAccess());
        test.assertTrue(pair.getCount() == 2 && Frequential.getFrequency(pair).isPresent() && QBlockItem.equals(pair, crafted),
                "Entangled output should contain two qBlocks with the same faces and a frequency");

        grid.setItem(3, new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_COMPUTER.get()));
        grid.setItem(5, grid.getItem(3).copy());
        test.assertTrue(entangle.matches(grid, test.getLevel()), "Matching computers should entangle");
        test.assertTrue(entangle.assemble(grid, test.getLevel().registryAccess()).getCount() == 2,
                "Entangling computers should yield both machines");

        grid.clearContent();
        ItemStack coolant = new ItemStack(com.acikek.qcraft.item.Items.COOLANT_CELL.get());
        coolant.setDamageValue(coolant.getMaxDamage());
        grid.setItem(0, coolant);
        grid.setItem(1, new ItemStack(Blocks.BLUE_ICE));
        test.assertTrue(refill.matches(grid, test.getLevel()), "An exhausted coolant cell should be rechargeable");
        test.assertTrue(refill.assemble(grid, test.getLevel().registryAccess()).getDamageValue() == 0, "Coolant charge was not restored");
        for (String id : List.of("quantum_dust", "quantum_dust_block", "observation_essence", "superposition_essence",
                "entanglement_essence", "automated_observer", "anti_observation_goggles", "quantum_computer", "coolant_cell")) {
            test.assertTrue(test.getLevel().getRecipeManager().byKey(QCraft.id(id)).isPresent(), "Missing recipe " + id);
        }
        test.assertTrue(test.getLevel().getServer().getAdvancements().getAdvancement(QCraft.id("qcraft/quantum_computer_teleport")) != null,
                "The teleportation advancement should load with its custom trigger");
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void oreLightsAndDropsDust(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        BlockPos source = new BlockPos(5, 1, 4);
        BlockPos lower = source.east();
        var ore = com.acikek.qcraft.block.Blocks.QUANTUM_ORE.get();
        var deepslate = com.acikek.qcraft.block.Blocks.DEEPSLATE_QUANTUM_ORE.get();
        test.setBlock(source, ore);
        test.setBlock(lower, deepslate);
        for (BlockPos rel : List.of(source, lower)) {
            BlockPos pos = test.absolutePos(rel);
            level.getBlockState(pos).attack(level, pos, player);
            test.assertBlockProperty(rel, QuantumOre.LIT, true);
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, null, player, new ItemStack(Items.IRON_PICKAXE));
            test.assertTrue(drops.stream().anyMatch(stack -> stack.is(com.acikek.qcraft.item.Items.QUANTUM_DUST.get())), "Quantum ore must drop dust");
            test.randomTick(rel);
            test.assertBlockProperty(rel, QuantumOre.LIT, false);
        }
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void entangledBlocksObserveAndPersist(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        ItemStack goggles = new ItemStack(com.acikek.qcraft.item.Items.ANTI_OBSERVATION_GOGGLES.get());
        player.setItemSlot(EquipmentSlot.HEAD, goggles);
        test.assertTrue(Goggles.isWearingGoggles(player, Goggles.Type.ANTI_OBSERVATION), "Goggles must equip on the head");

        BlockPos first = new BlockPos(6, 1, 4);
        BlockPos second = new BlockPos(15, 1, 4);
        UUID frequency = UUID.randomUUID();
        for (BlockPos pos : List.of(first, second)) {
            ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_BLOCK.get());
            QBlockItem.applyFaces(stack, List.of("minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block",
                    "minecraft:stone", "minecraft:gold_block", "minecraft:gold_block"));
            EntangledPairRecipe.applyFrequency(stack, frequency);
            place(test, player, pos, stack);
            test.assertBlockPresent(com.acikek.qcraft.block.Blocks.INERT_QUANTUM_BLOCK.get(), pos);
        }
        var observer = com.acikek.qcraft.block.Blocks.AUTOMATED_OBSERVER.get();
        test.setBlock(first.east().below(), Blocks.STONE);
        test.setBlock(first.east(), observer.defaultBlockState().setValue(AutomatedObserver.FACING, Direction.EAST));
        test.setBlock(first.east(2), Blocks.REDSTONE_BLOCK);
        test.runAtTickTime(6, () -> {
            test.assertBlockProperty(first.east(), AutomatedObserver.POWERED, true);
            test.assertTrue(test.getBlockState(first) == test.getBlockState(second), "The entangled displays should match");
            test.assertBlock(first, block -> block == Blocks.STONE || block == Blocks.GOLD_BLOCK, "An observer must collapse the pair");

            QBlockData data = QBlockData.get(level);
            QBlockData saved = QBlockData.fromNbt(data.save(new CompoundTag()));
            var restored = saved.locations.get(test.absolutePos(first)).orElseThrow();
            test.assertTrue(restored.observed && restored.frequency.orElseThrow().equals(frequency)
                            && saved.frequencies.counterpart(restored).pos.equals(test.absolutePos(second)),
                    "Saving must retain observation, frequency, and counterpart");
            test.setBlock(first.east(2), Blocks.AIR);
        });
        test.runAtTickTime(12, () -> {
            test.assertBlockPresent(com.acikek.qcraft.block.Blocks.INERT_QUANTUM_BLOCK.get(), first);
            test.assertBlockPresent(com.acikek.qcraft.block.Blocks.INERT_QUANTUM_BLOCK.get(), second);
            test.succeed();
        });
    }

    @GameTest(template = AREA)
    public static void playerBreakRecoversConfiguredQBlock(GameTestHelper test) {
        var player = fakePlayer(test, FIRST_STATION);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.OBSERVER_DEPENDENT_BLOCK.get());
        QBlockItem.applyFaces(stack, Collections.nCopies(6, "minecraft:stone"));
        UUID frequency = UUID.randomUUID();
        EntangledPairRecipe.applyFrequency(stack, frequency);
        BlockPos pos = FIRST_STATION.east();
        ItemStack expected = stack.copy();
        place(test, player, pos, stack);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos absolute = test.absolutePos(pos);
        test.assertTrue(player.gameMode.destroyBlock(absolute), "Survival player should be able to break a qBlock");
        test.assertFalse(QBlockData.get(test.getLevel()).locations.has(absolute), "Breaking must remove the qBlock record");
        var drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2));
        test.assertTrue(drops.size() == 1 && QBlockItem.equals(drops.get(0).getItem(), expected)
                        && Frequential.getFrequency(drops.get(0).getItem()).orElseThrow().equals(frequency),
                "Mining must drop one configured block with the original frequency, got " + drops.stream().map(ItemEntity::getItem).toList());
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void computerSwapValidatesAndChargesOnce(GameTestHelper test) {
        var player = fakePlayer(test, FIRST_STATION.north());
        UUID frequency = UUID.randomUUID();
        QuantumComputerBlockEntity source = station(test, player, FIRST_STATION, frequency);
        QuantumComputerBlockEntity target = station(test, player, SECOND_STATION, frequency);
        test.assertTrue(QuantumComputerData.get(test.getLevel()).locations.has(target.getBlockPos()), "Target placement must register its frequency");
        BlockPos a = FIRST_STATION.offset(1, 0, 1);
        BlockPos b = SECOND_STATION.offset(1, 0, 1);
        test.setBlock(a, Blocks.GOLD_BLOCK);
        test.setBlock(b, Blocks.DIAMOND_BLOCK);
        QuantumComputerMenu menu = new QuantumComputerMenu(6, player.getInventory(), source);
        test.assertFalse(menu.clickMenuButton(player, QuantumComputerMenu.ENERGIZE_BUTTON), "A closed menu cannot energize");
        player.containerMenu = menu;

        menu.clickMenuButton(player, QuantumComputerMenu.ENERGIZE_BUTTON);
        test.assertTrue(menu.getStatus() == QuantumComputer.Status.NO_COOLANT, "A swap requires coolant in the server inventory");
        test.assertBlockPresent(Blocks.GOLD_BLOCK, a);
        var coolant = new ItemStack(com.acikek.qcraft.item.Items.COOLANT_CELL.get());
        source.setItem(0, coolant);

        test.setBlock(b, Blocks.CHEST);
        menu.clickMenuButton(player, QuantumComputerMenu.ENERGIZE_BUTTON);
        test.assertTrue(menu.getStatus() == QuantumComputer.Status.UNSUPPORTED_BLOCKS && coolant.getDamageValue() == 0,
                "A chest in either region must prevent mutation and coolant use");
        test.assertBlockPresent(Blocks.CHEST, b);
        test.setBlock(b, Blocks.DIAMOND_BLOCK);
        test.setBlock(SECOND_STATION.north(2), Blocks.AIR);
        menu.clickMenuButton(player, QuantumComputerMenu.ENERGIZE_BUTTON);
        test.assertTrue(menu.getStatus() == QuantumComputer.Status.ERRORED_COUNTERPART && coolant.getDamageValue() == 0,
                "The server must recheck both pylon systems at click time");
        pylon(test, player, SECOND_STATION, QBlock.Face.NORTH, 2);
        menu.clickMenuButton(player, QuantumComputerMenu.ENERGIZE_BUTTON);
        test.assertTrue(menu.getStatus() == QuantumComputer.Status.READY && coolant.getDamageValue() == 1,
                "A successful swap must consume exactly one coolant charge");
        test.assertBlockPresent(Blocks.DIAMOND_BLOCK, a);
        test.assertBlockPresent(Blocks.GOLD_BLOCK, b);
        test.assertTrue(test.getBlockEntity(FIRST_STATION) == source && test.getBlockEntity(SECOND_STATION) == target,
                "The computers themselves must stay in place");
        QuantumComputerBlockEntity restored = new QuantumComputerBlockEntity(source.getBlockPos(), source.getBlockState());
        restored.load(source.saveWithoutMetadata());
        test.assertTrue(restored.getItem(0).getDamageValue() == 1 && restored.frequency().orElseThrow().equals(frequency),
                "Saving must retain coolant charge and computer frequency");
        test.succeed();
    }

    private static CraftingRecipe recipe(GameTestHelper test, String path) {
        return (CraftingRecipe) test.getLevel().getRecipeManager().byKey(QCraft.id(path)).orElseThrow();
    }

    private static FakePlayer fakePlayer(GameTestHelper test, BlockPos pos) {
        var player = new FakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), "qCraftGameTest"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        Vec3 absolute = Vec3.atBottomCenterOf(test.absolutePos(pos));
        player.moveTo(absolute.x, absolute.y, absolute.z, 0, 0);
        return player;
    }

    private static void place(GameTestHelper test, Player player, BlockPos relative, ItemStack stack) {
        test.setBlock(relative.below(), Blocks.STONE);
        BlockPos support = test.absolutePos(relative.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "Could not place the item at " + relative);
    }

    private static QuantumComputerBlockEntity station(GameTestHelper test, Player player, BlockPos pos, UUID frequency) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(com.acikek.qcraft.item.Items.ANTI_OBSERVATION_GOGGLES.get()));
        ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_COMPUTER.get());
        EntangledPairRecipe.applyFrequency(stack, frequency);
        place(test, player, pos, stack);
        for (QBlock.Face face : QBlock.Face.CARDINALS) {
            pylon(test, player, pos, face, 2);
        }
        return (QuantumComputerBlockEntity) test.getBlockEntity(pos);
    }

    private static void pylon(GameTestHelper test, Player player, BlockPos center, QBlock.Face side, int distance) {
        ItemStack base = new ItemStack(com.acikek.qcraft.block.Blocks.OBSERVER_DEPENDENT_BLOCK.get());
        QBlockItem.applyFaces(base, Collections.nCopies(6, "minecraft:obsidian"));
        side.getOpposite().apply(base, "minecraft:gold_block");
        place(test, player, center.relative(side.direction, distance), base);
    }
}
