package com.acikek.qcraft.block.qblock;

import net.minecraft.world.level.block.Block;

public class InertQBlock extends Block {

    public static final Properties DEFAULT_SETTINGS = Properties.of().strength(5.0f, 5.0f);

    public InertQBlock() {
        super(DEFAULT_SETTINGS);
    }
}
