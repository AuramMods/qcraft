package com.acikek.qcraft.item;

import com.acikek.qcraft.block.Blocks;
import com.acikek.qcraft.block.qblock.QBlock;

public class QBlockEssence extends Essence {

    public QBlockEssence(Properties settings, Type essenceType) {
        super(settings, essenceType);
    }

    public QBlock getQBlock() {
        return switch(essenceType) {
            case OBSERVATION -> Blocks.OBSERVER_DEPENDENT_BLOCK.get();
            case SUPERPOSITION -> Blocks.QUANTUM_BLOCK.get();
            default -> null;
        };
    }
}
