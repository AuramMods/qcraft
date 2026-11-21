package com.acikek.qcraft.block.quantum_computer;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.AutomatedObserver;
import com.acikek.qcraft.block.QuantumOre;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.block.qblock.QBlockItem;
import com.acikek.qcraft.compat.CuriosGoggles;
import com.acikek.qcraft.item.Goggles;
import com.acikek.qcraft.recipe.EntangledPairRecipe;
import com.acikek.qcraft.world.state.QBlockData;
import com.acikek.qcraft.world.state.QuantumComputerData;
import com.acikek.qcraft.world.state.frequency.Frequential;
import com.acikek.qcraft.world.state.location.QBlockLocation;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@GameTestHolder(QCraft.ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = QCraft.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class QCraftGameTests {

    private static final String AREA = "test_area";
    private static final BlockPos FIRST_STATION = new BlockPos(5, 1, 5);
    private static final BlockPos SECOND_STATION = new BlockPos(17, 1, 5);

    private static Block interruptingBlock;
    private static BlockPos interruptAt;

    @SubscribeEvent
    public static void registerTestBlock(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, QCraft.id("test_interrupting_block"), () -> {
            interruptingBlock = new Block(BlockBehaviour.Properties.of()) {
                @Override
                public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
                    if (pos.equals(interruptAt)) {
                        throw new IllegalStateException("GameTest interrupted the region swap");
                    }
                }
            };
            return interruptingBlock;
        });
    }

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
        equipAntiObservationGoggles(player);

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
        drops.get(0).discard();
        test.succeed();
    }

    @GameTest(template = AREA, timeoutTicks = 600)
    public static void fieldGuideCircuitLightsForOnlyOneOutcome(GameTestHelper test) {
        BlockPos target = FIRST_STATION;
        BlockPos lamp = target.west();
        BlockPos observerPos = target.east();
        BlockPos lever = target.east(2);
        Block inert = com.acikek.qcraft.block.Blocks.INERT_QUANTUM_BLOCK.get();
        var player = test.makeMockSurvivalPlayer();
        equipAntiObservationGoggles(player);
        ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_BLOCK.get());
        QBlockItem.applyFaces(stack, Collections.nCopies(6, "minecraft:stone"));
        QBlock.Face.EAST.apply(stack, "minecraft:redstone_block");
        place(test, player, target, stack);
        test.setBlock(lamp, Blocks.REDSTONE_LAMP);
        test.setBlock(observerPos.below(), Blocks.STONE);
        test.setBlock(observerPos, com.acikek.qcraft.block.Blocks.AUTOMATED_OBSERVER.get()
                .defaultBlockState().setValue(AutomatedObserver.FACING, Direction.EAST));
        test.setBlock(lever.below(), Blocks.STONE);
        test.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));

        var results = new HashSet<Block>();
        for (int trial = 0; trial < 32; trial++) {
            int start = 1 + trial * 16;
            test.runAtTickTime(start, () -> {
                test.assertBlockPresent(inert, target);
                test.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false);
                test.pullLever(lever);
            });
            test.runAtTickTime(start + 6, () -> {
                test.assertBlockProperty(observerPos, AutomatedObserver.POWERED, true);
                Block state = test.getBlockState(target).getBlock();
                test.assertTrue(state == Blocks.REDSTONE_BLOCK || state == Blocks.STONE,
                        "The observer should select the east or west face");
                test.assertBlockProperty(lamp, RedstoneLampBlock.LIT, state == Blocks.REDSTONE_BLOCK);
                results.add(state);
                test.pullLever(lever);
            });
        }
        test.runAtTickTime(513, () -> {
            test.assertBlockPresent(inert, target);
            test.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false);
            test.assertTrue(results.size() == 2, "Repeated trials should produce both opposite faces");
            test.succeed();
        });
    }

    @GameTest(template = AREA)
    public static void gogglesUseCuriosInsteadOfArmor(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slot = new SlotContext(CuriosGoggles.SLOT, player, 0, false, true);
        ItemStack anti = new ItemStack(com.acikek.qcraft.item.Items.ANTI_OBSERVATION_GOGGLES.get());
        ItemStack quantum = new ItemStack(com.acikek.qcraft.item.Items.QUANTUM_GOGGLES.get());
        test.assertTrue(inventory.getStacksHandler(CuriosGoggles.SLOT).orElseThrow().getSlots() == 1,
                "Players must have one Goggles slot");
        for (ItemStack goggles : List.of(anti, quantum)) {
            test.assertFalse(goggles.getItem() instanceof ArmorItem, "Goggles must not be armor");
            test.assertTrue(CuriosApi.isStackValid(slot, goggles), "The Goggles slot must accept both qCraft pairs");
        }
        test.assertFalse(CuriosApi.isStackValid(slot, new ItemStack(Items.IRON_HELMET)), "Goggles slot must reject helmets");
        player.setItemSlot(EquipmentSlot.HEAD, anti.copy());
        test.assertFalse(Goggles.isWearingGoggles(player, Goggles.Type.ANTI_OBSERVATION), "Legacy helmet slot must have no effect");
        Goggles.recoverLegacyHelmet(new PlayerEvent.PlayerLoggedInEvent(player));
        test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && player.getInventory().contains(anti),
                "Logging in must recover legacy goggles into the inventory");
        inventory.setEquippedCurio(CuriosGoggles.SLOT, 0, quantum);
        test.assertFalse(Goggles.isWearingGoggles(player, Goggles.Type.ANTI_OBSERVATION), "Quantum Goggles are cosmetic");
        equipAntiObservationGoggles(player);
        test.assertTrue(Goggles.isWearingGoggles(player, Goggles.Type.ANTI_OBSERVATION), "Anti-Observation Goggles work when equipped as a curio");
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void tileFacesKeepSeparateInventoriesAndDropOnce(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos pos = FIRST_STATION;
        BlockPos absolute = test.absolutePos(pos);
        var player = fakePlayer(test, pos.south(2));
        equipAntiObservationGoggles(player);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        grid.setItem(QBlock.Face.CENTER, new ItemStack(com.acikek.qcraft.item.Items.OBSERVATION_ESSENCE.get()));
        grid.setItem(QBlock.Face.UP.slot, seededChest(new ItemStack(Items.EMERALD, 3)));
        grid.setItem(QBlock.Face.EAST.slot, seededChest(new ItemStack(Items.DIAMOND, 2)));
        CraftingRecipe recipe = recipe(test, "quantum_block");
        test.assertTrue(recipe.matches(grid, level), "The essence recipe must accept tagged block entity items");
        ItemStack configured = recipe.assemble(grid, level.registryAccess());
        ItemStack expected = configured.copy();
        place(test, player, pos, configured);
        QBlockData data = QBlockData.get(level);
        var location = data.locations.get(absolute).orElseThrow();
        showFace(data, location, level, QBlock.Face.UP);
        Container chest = (Container) level.getBlockEntity(absolute);
        assertContents(test, chest, Items.EMERALD, 3);
        chest.setItem(0, new ItemStack(Items.EMERALD, 7));
        showFace(data, location, level, QBlock.Face.EAST);
        assertContents(test, (Container) level.getBlockEntity(absolute), Items.DIAMOND, 2);
        data.unobserve(location, level);
        test.assertBlockPresent(com.acikek.qcraft.block.Blocks.INERT_OBSERVER_DEPENDENT_BLOCK.get(), pos);
        test.assertTrue(level.getBlockEntity(absolute) == null, "A hidden face must have no live block entity");

        var saved = QBlockData.fromNbt(data.save(new CompoundTag())).locations.get(absolute).orElseThrow();
        var pairGrid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        pairGrid.setItem(0, expected);
        pairGrid.setItem(1, new ItemStack(com.acikek.qcraft.item.Items.ENTANGLEMENT_ESSENCE.get()));
        pairGrid.setItem(2, saved.getItemStack());
        test.assertFalse(recipe(test, "entangled_pair").matches(pairGrid, level),
                "Pairing must not replace one chest's contents with a copy of another");
        pairGrid.setItem(0, saved.getItemStack());
        test.assertTrue(recipe(test, "entangled_pair").matches(pairGrid, level), "Identical saved faces can be paired");

        showFace(data, location, level, QBlock.Face.UP);
        chest = (Container) level.getBlockEntity(absolute);
        assertContents(test, chest, Items.EMERALD, 7);
        chest.setItem(0, new ItemStack(Items.EMERALD, 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        test.assertTrue(player.gameMode.destroyBlock(absolute), "The live chest face must be mineable");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2));
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().getItem() instanceof QBlockItem,
                "Transitions and mining must produce only one configured qBlock");
        ItemStack recovered = drops.get(0).getItem().copy();
        drops.get(0).discard();
        place(test, player, pos, recovered);
        location = data.locations.get(absolute).orElseThrow();
        showFace(data, location, level, QBlock.Face.UP);
        assertContents(test, (Container) level.getBlockEntity(absolute), Items.EMERALD, 5);
        showFace(data, location, level, QBlock.Face.EAST);
        assertContents(test, (Container) level.getBlockEntity(absolute), Items.DIAMOND, 2);

        test.assertTrue(level.destroyBlock(absolute, true), "The live container must support non-player breakage");
        drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2));
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().getItem() instanceof QBlockItem,
                "World breakage must drop only the configured qBlock");
        drops.get(0).discard();
        test.succeed();
    }

    private static ItemStack seededChest(ItemStack content) {
        ItemStack chest = new ItemStack(Items.CHEST);
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        items.set(0, content);
        chest.addTagElement("BlockEntityTag", ContainerHelper.saveAllItems(new CompoundTag(), items));
        return chest;
    }

    private static void showFace(QBlockData data, QBlockLocation location,
                                 ServerLevel level, QBlock.Face face) {
        data.observe(location, level, face, QBlock.Observation.AUTOMATED_OBSERVER, null);
    }

    private static void assertContents(GameTestHelper test, Container container, Item item, int count) {
        ItemStack stack = container.getItem(0);
        test.assertTrue(stack.is(item) && stack.getCount() == count, "Expected " + count + " " + item + " in the active face, got " + stack);
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

    @GameTest(template = AREA)
    public static void computerRollsBackInterruptedSwap(GameTestHelper test) {
        var player = fakePlayer(test, FIRST_STATION.north());
        UUID frequency = UUID.randomUUID();
        QuantumComputerBlockEntity source = station(test, player, FIRST_STATION, frequency);
        station(test, player, SECOND_STATION, frequency);
        BlockPos a = FIRST_STATION.offset(1, 0, 1);
        BlockPos b = SECOND_STATION.offset(1, 0, 1);
        test.setBlock(a, interruptingBlock);
        test.setBlock(b, Blocks.DIAMOND_BLOCK);
        var coolant = new ItemStack(com.acikek.qcraft.item.Items.COOLANT_CELL.get());
        source.setItem(0, coolant);
        interruptAt = test.absolutePos(b);
        try {
            test.assertTrue(QuantumComputer.energize(test.getLevel(), source, player) == QuantumComputer.Status.SWAP_FAILED,
                    "The rejected state change should fail the swap");
        } finally {
            interruptAt = null;
        }
        test.assertBlockPresent(interruptingBlock, a);
        test.assertBlockPresent(Blocks.DIAMOND_BLOCK, b);
        test.assertTrue(coolant.getDamageValue() == 0, "An interrupted swap must not use coolant");
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void computerRejectsOverlappingAndUnloadedRegions(GameTestHelper test) {
        var player = fakePlayer(test, FIRST_STATION.north());
        UUID frequency = UUID.randomUUID();
        BlockPos nearby = FIRST_STATION.offset(4, 0, 3);
        QuantumComputerBlockEntity source = station(test, player, FIRST_STATION, frequency, 3);
        station(test, player, nearby, frequency, 3);
        var coolant = new ItemStack(com.acikek.qcraft.item.Items.COOLANT_CELL.get());
        source.setItem(0, coolant);
        test.assertTrue(QuantumComputer.energize(test.getLevel(), source, player) == QuantumComputer.Status.OVERLAP,
                "Two intersecting boxes must not swap");
        test.setBlock(nearby, Blocks.AIR);

        var stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_COMPUTER.get());
        EntangledPairRecipe.applyFrequency(stack, frequency);
        BlockPos distant = source.getBlockPos().offset(-16_001, 0, -16_001);
        QuantumComputerData data = QuantumComputerData.get(test.getLevel());
        var unloaded = data.add(distant, stack);
        test.assertTrue(QuantumComputer.energize(test.getLevel(), source, player) == QuantumComputer.Status.UNLOADED,
                "An unloaded partner must not be swapped");
        test.assertFalse(test.getLevel().hasChunkAt(distant), "The attempted swap must not load remote chunks");
        test.assertTrue(coolant.getDamageValue() == 0, "Rejected swaps must not use coolant");
        data.remove(unloaded);
        test.succeed();
    }

    @GameTest(template = AREA)
    public static void hopperAcceptsOnlyCoolantAndComputerDropsIt(GameTestHelper test) {
        var player = fakePlayer(test, FIRST_STATION.north());
        UUID frequency = UUID.randomUUID();
        var stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_COMPUTER.get());
        EntangledPairRecipe.applyFrequency(stack, frequency);
        place(test, player, FIRST_STATION, stack);
        test.setBlock(FIRST_STATION.above(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) test.getBlockEntity(FIRST_STATION.above());
        hopper.setItem(0, new ItemStack(Items.STONE));
        hopper.setItem(1, new ItemStack(com.acikek.qcraft.item.Items.COOLANT_CELL.get()));
        var computer = (QuantumComputerBlockEntity) test.getBlockEntity(FIRST_STATION);
        test.startSequence().thenWaitUntil(() -> test.assertTrue(computer.getItem(0).is(com.acikek.qcraft.item.Items.COOLANT_CELL.get()),
                "The hopper should transfer coolant into the computer")).thenExecute(() -> {
            test.assertTrue(hopper.getItem(0).is(Items.STONE) && hopper.getItem(1).isEmpty(), "Hopper must insert coolant only");
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            BlockPos pos = test.absolutePos(FIRST_STATION);
            test.assertTrue(player.gameMode.destroyBlock(pos), "The player should break the computer");
            var drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            test.assertTrue(drops.size() == 2
                            && drops.stream().filter(item -> Frequential.getFrequency(item.getItem()).filter(frequency::equals).isPresent()).count() == 1
                            && drops.stream().filter(item -> item.getItem().is(com.acikek.qcraft.item.Items.COOLANT_CELL.get())).count() == 1,
                    "Breaking should drop one entangled computer and its coolant, got " + drops.stream().map(ItemEntity::getItem).toList());
        }).thenSucceed();
    }

    @GameTest(template = AREA)
    public static void qBlockRecordsStayInTheirDimensionAndQueueRemoteFaces(GameTestHelper test) {
        var level = test.getLevel();
        var otherLevel = level.getServer().getLevel(Level.NETHER);
        var player = test.makeMockSurvivalPlayer();
        equipAntiObservationGoggles(player);
        UUID frequency = UUID.randomUUID();
        ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.OBSERVER_DEPENDENT_BLOCK.get());
        QBlockItem.applyFaces(stack, List.of("minecraft:gold_block", "minecraft:stone", "minecraft:stone",
                "minecraft:stone", "minecraft:stone", "minecraft:stone"));
        EntangledPairRecipe.applyFrequency(stack, frequency);
        BlockPos local = test.absolutePos(FIRST_STATION);
        BlockPos distant = local.offset(-16_001, 0, -16_001);
        place(test, player, FIRST_STATION, stack.copy());
        QBlockData data = QBlockData.get(level);
        var first = data.locations.get(local).orElseThrow();
        var remote = data.add(first.type, distant, stack);
        var otherDimension = QBlockData.get(otherLevel);
        otherDimension.add(first.type, local, stack);
        data.observe(first, level, QBlock.Face.UP, QBlock.Observation.AUTOMATED_OBSERVER, null);
        test.assertBlockPresent(Blocks.GOLD_BLOCK, FIRST_STATION);
        test.assertTrue(otherDimension != data && otherDimension.frequencies.counterpart(otherDimension.locations.get(local).orElseThrow()) == null,
                "An equal position and frequency in a different dimension must not connect");
        test.assertFalse(level.hasChunkAt(distant), "An observed pair must leave the unloaded companion chunk unloaded");
        var saved = QBlockData.fromNbt(data.save(new CompoundTag()));
        test.assertTrue(remote.pendingFace == QBlock.Face.UP.index
                        && saved.locations.get(distant).orElseThrow().pendingFace == QBlock.Face.UP.index,
                "The pending display must be persisted for an unloaded companion");
        data.remove(remote);
        otherDimension.locations.get(local).ifPresent(otherDimension::remove);
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

    private static void equipAntiObservationGoggles(Player player) {
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow().setEquippedCurio(CuriosGoggles.SLOT, 0,
                new ItemStack(com.acikek.qcraft.item.Items.ANTI_OBSERVATION_GOGGLES.get()));
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
        return station(test, player, pos, frequency, 2);
    }

    private static QuantumComputerBlockEntity station(GameTestHelper test, Player player, BlockPos pos, UUID frequency, int distance) {
        equipAntiObservationGoggles(player);
        ItemStack stack = new ItemStack(com.acikek.qcraft.block.Blocks.QUANTUM_COMPUTER.get());
        EntangledPairRecipe.applyFrequency(stack, frequency);
        place(test, player, pos, stack);
        for (QBlock.Face face : QBlock.Face.CARDINALS) {
            pylon(test, player, pos, face, distance);
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
