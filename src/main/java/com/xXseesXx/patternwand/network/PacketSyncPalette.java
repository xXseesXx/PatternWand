package com.xXseesXx.patternwand.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.xXseesXx.patternwand.items.IPatternWandItem;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Packet to sync palette data from client to server when GUI is closed.
 */
public class PacketSyncPalette implements IMessage {

    private NBTTagCompound paletteData;

    public PacketSyncPalette() {}

    public PacketSyncPalette(NBTTagCompound paletteData) {
        this.paletteData = paletteData;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, paletteData);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        paletteData = ByteBufUtils.readTag(buffer);
    }

    public static class Handler extends GenericHandler<PacketSyncPalette> {

        @Override
        public void processMessage(PacketSyncPalette message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ItemStack heldItem = player.getCurrentEquippedItem();

            if (heldItem != null && heldItem.getItem() instanceof IPatternWandItem) {
                // Apply the palette data to the wand
                NBTTagCompound itemNBT = heldItem.hasTagCompound() ? heldItem.getTagCompound() : new NBTTagCompound();
                NBTTagCompound bbwNBT = itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND)
                    ? itemNBT.getCompoundTag("bbw")
                    : new NBTTagCompound();

                // Copy the palette data
                if (message.paletteData.hasKey("palette", Constants.NBT.TAG_LIST)) {
                    bbwNBT.setTag("palette", message.paletteData.getTag("palette"));
                }

                // Increment palette version for TOCTOU protection
                short currentVersion = bbwNBT.hasKey("paletteVersion") ? bbwNBT.getShort("paletteVersion") : 0;
                bbwNBT.setShort("paletteVersion", (short) (currentVersion + 1));

                itemNBT.setTag("bbw", bbwNBT);
                heldItem.setTagCompound(itemNBT);
            }
        }
    }
}
