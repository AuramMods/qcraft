package com.acikek.qcraft.advancement;

import com.acikek.qcraft.QCraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class QuantumTeleportationCriterion extends SimpleCriterionTrigger<QuantumTeleportationCriterion.Conditions> {

    public static ResourceLocation ID = QCraft.id("quantum_teleportation");

    @Override
    protected Conditions createInstance(JsonObject obj, ContextAwarePredicate playerPredicate, DeserializationContext predicateDeserializer) {
        JsonPrimitive width = obj.getAsJsonPrimitive("width");
        JsonPrimitive length = obj.getAsJsonPrimitive("length");
        JsonPrimitive height = obj.getAsJsonPrimitive("height");
        return new Conditions(playerPredicate, defaultInt(width), defaultInt(length), defaultInt(height));
    }

    public int defaultInt(JsonPrimitive value) {
        return value != null ? value.getAsInt() : 0;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, int[] dimensions) {
        trigger(player, dimensions[0], dimensions[1], dimensions[2]);
    }

    public void trigger(ServerPlayer player, int width, int length, int height) {
        trigger(player, (QuantumTeleportationCriterion.Conditions conditions) -> conditions.matches(width, length, height));
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {

        int width;
        int length;
        int height;

        public Conditions(ContextAwarePredicate playerPredicate, int width, int length, int height) {
            super(ID, playerPredicate);
            this.width = width;
            this.length = length;
            this.height = height;
        }

        public boolean matches(int width, int length, int height) {
            return width >= this.width
                    && length >= this.length
                    && height >= this.height;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject obj = super.serializeToJson(predicateSerializer);
            obj.add("width", new JsonPrimitive(width));
            obj.add("length", new JsonPrimitive(length));
            obj.add("height", new JsonPrimitive(height));
            return obj;
        }
    }
}
