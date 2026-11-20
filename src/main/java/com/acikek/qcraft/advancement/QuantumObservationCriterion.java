package com.acikek.qcraft.advancement;

import com.acikek.qcraft.QCraft;
import com.acikek.qcraft.block.qblock.QBlock;
import com.acikek.qcraft.predicate.EnumPredicate;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class QuantumObservationCriterion extends SimpleCriterionTrigger<QuantumObservationCriterion.Conditions> {

    public static ResourceLocation ID = QCraft.id("quantum_observation");

    @Override
    protected Conditions createInstance(JsonObject obj, ContextAwarePredicate playerPredicate, DeserializationContext predicateDeserializer) {
        BlockPredicate face = BlockPredicate.fromJson(obj.get("face"));
        EnumPredicate<QBlock.Observation> observation = EnumPredicate.fromJson(obj.get("observation"), QBlock.Observation::valueOf);
        EnumPredicate<QBlock.Type> type = EnumPredicate.fromJson(obj.get("type"), QBlock.Type::valueOf);
        JsonPrimitive entangled = obj.getAsJsonPrimitive("entangled");
        return new Conditions(playerPredicate, face, observation, type, entangled != null && entangled.getAsBoolean());
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, BlockPos pos, QBlock.Observation observationType, QBlock.Type qBlockType, boolean entangled) {
        trigger(player, (Conditions conditions) -> conditions.matches(player.serverLevel(), pos, observationType, qBlockType, entangled));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {

        public BlockPredicate face;
        public EnumPredicate<QBlock.Observation> observation;
        public EnumPredicate<QBlock.Type> type;
        public boolean entangled;

        public Conditions(ContextAwarePredicate playerPredicate, BlockPredicate face, EnumPredicate<QBlock.Observation> observation, EnumPredicate<QBlock.Type> type, boolean entangled) {
            super(ID, playerPredicate);
            this.face = face;
            this.observation = observation;
            this.type = type;
            this.entangled = entangled;
        }

        public boolean matches(ServerLevel world, BlockPos pos, QBlock.Observation observation, QBlock.Type type, boolean entangled) {
            return face.matches(world, pos)
                    && this.observation.test(observation)
                    && this.type.test(type)
                    && this.entangled == entangled;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject obj = super.serializeToJson(predicateSerializer);
            obj.add("face", face.serializeToJson());
            obj.add("observation", observation.toJson());
            obj.add("type", type.toJson());
            obj.add("entangled", new JsonPrimitive(entangled));
            return obj;
        }
    }
}
