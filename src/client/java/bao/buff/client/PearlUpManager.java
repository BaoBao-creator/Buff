package bao.buff.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PearlUpManager {
    private static final float STRAIGHT_UP_PITCH = -90.0F;

    private static int ticksUntilWindCharge;
    private static int windChargeSlot = -1;
    private static int originalSlot = -1;

    private PearlUpManager() {
    }

    public static void onClientTick(Minecraft client) {
        if (ticksUntilWindCharge <= 0) {
            return;
        }

        ticksUntilWindCharge--;
        if (ticksUntilWindCharge > 0) {
            return;
        }

        usePendingWindCharge(client);
    }

    public static ActivationResult tryActivate(Minecraft client) {
        if (!Config.pearlUpEnabled || client == null || client.player == null || client.gameMode == null || client.screen != null) {
            return ActivationResult.NOT_READY;
        }

        Inventory inventory = client.player.getInventory();
        int pearlSlot = findHotbarSlot(inventory, Items.ENDER_PEARL);
        int foundWindChargeSlot = findHotbarSlot(inventory, Items.WIND_CHARGE);

        if (pearlSlot == -1 && foundWindChargeSlot == -1) {
            return ActivationResult.MISSING_BOTH;
        }
        if (pearlSlot == -1) {
            return ActivationResult.MISSING_PEARL;
        }
        if (foundWindChargeSlot == -1) {
            return ActivationResult.MISSING_WIND_CHARGE;
        }

        originalSlot = inventory.getSelectedSlot();
        windChargeSlot = foundWindChargeSlot;
        lookStraightUp(client);
        useHotbarSlot(client, inventory, pearlSlot);
        restoreSlot(client, inventory, originalSlot);
        ticksUntilWindCharge = 1;
        return ActivationResult.SUCCESS;
    }

    private static void usePendingWindCharge(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null) {
            resetPendingUse();
            return;
        }

        Inventory inventory = client.player.getInventory();
        int slotToUse = windChargeSlot;
        int slotToRestore = originalSlot;

        resetPendingUse();

        if (slotToUse < 0 || slotToUse >= 9 || inventory.getItem(slotToUse).isEmpty() || !inventory.getItem(slotToUse).is(Items.WIND_CHARGE)) {
            restoreSlot(client, inventory, slotToRestore);
            return;
        }

        lookStraightUp(client);
        useHotbarSlot(client, inventory, slotToUse);
        restoreSlot(client, inventory, slotToRestore);
    }

    private static int findHotbarSlot(Inventory inventory, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return slot;
            }
        }

        return -1;
    }

    private static void lookStraightUp(Minecraft client) {
        client.player.setXRot(STRAIGHT_UP_PITCH);
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                client.player.getYRot(),
                STRAIGHT_UP_PITCH,
                client.player.onGround(),
                client.player.horizontalCollision
            ));
        }
    }

    private static void useHotbarSlot(Minecraft client, Inventory inventory, int slot) {
        selectSlot(client, inventory, slot);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void restoreSlot(Minecraft client, Inventory inventory, int slot) {
        if (slot >= 0 && slot < 9 && inventory.getSelectedSlot() != slot) {
            selectSlot(client, inventory, slot);
        }
    }

    private static void selectSlot(Minecraft client, Inventory inventory, int slot) {
        if (inventory.getSelectedSlot() == slot) {
            return;
        }

        inventory.setSelectedSlot(slot);
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private static void resetPendingUse() {
        ticksUntilWindCharge = 0;
        windChargeSlot = -1;
        originalSlot = -1;
    }

    public enum ActivationResult {
        SUCCESS,
        NOT_READY,
        MISSING_PEARL,
        MISSING_WIND_CHARGE,
        MISSING_BOTH
    }
}
