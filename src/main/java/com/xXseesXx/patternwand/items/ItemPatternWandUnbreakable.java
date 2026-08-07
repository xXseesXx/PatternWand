package com.xXseesXx.patternwand.items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.xXseesXx.patternwand.PatternWandMod;
import com.xXseesXx.patternwand.palette.BlockMatcher;
import com.xXseesXx.patternwand.palette.PatternPalette;

import portablejim.bbw.basics.EnumFluidLock;
import portablejim.bbw.basics.EnumLock;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.core.items.ItemBasicWand;
import portablejim.bbw.shims.BasicPlayerShim;
import portablejim.bbw.shims.BasicWorldShim;
import portablejim.bbw.shims.CreativePlayerShim;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

/**
 * The Unbreakable Pattern Wand item - extends BetterBuildersWands with palette-based block matching.
 */
public class ItemPatternWandUnbreakable extends ItemBasicWand implements IPatternWandItem {

    public ItemPatternWandUnbreakable() {
        super();
        this.setUnlocalizedName("patternwand:patternWandUnbreakable");
        this.setTextureName("patternwand:patternWandUnbreakable");
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setMaxStackSize(1);
        this.setMaxDamage(0); // 0 means unbreakable
        this.wand = new PatternWandUnbreakable();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        // Open GUI on shift+right-click in air
        if (player.isSneaking()) {
            if (!world.isRemote) {
                player.openGui(
                    PatternWandMod.instance,
                    0,
                    world,
                    (int) player.posX,
                    (int) player.posY,
                    (int) player.posZ);
            }
            return itemStack;
        }
        return super.onItemRightClick(itemStack, world, player);
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        // Open GUI on shift+right-click on block
        if (player.isSneaking()) {
            if (!world.isRemote) {
                player.openGui(
                    PatternWandMod.instance,
                    0,
                    world,
                    (int) player.posX,
                    (int) player.posY,
                    (int) player.posZ);
            }
            return true;
        }

        // Use modified wand placement with palette matching
        if (wand == null || itemstack == null) {
            return false;
        }

        if (!world.isRemote) {
            IPlayerShim playerShim = player.capabilities.isCreativeMode ? new CreativePlayerShim(player)
                : new BasicPlayerShim(player);
            IWorldShim worldShim = new BasicWorldShim(world);

            Point3d clickedPos = new Point3d(x, y, z);

            // Get the palette for matching
            PatternPalette palette = getPalette(itemstack);

            // Create block matcher using palette
            BlockMatcher matcher = new BlockMatcher(palette);

            // Get blocks that match the palette at clicked position
            ItemStack sourceItems = getProperItemStack(worldShim, playerShim, clickedPos, matcher);

            if (sourceItems != null && sourceItems.getItem() instanceof ItemBlock) {
                int numBlocks = Math.min(wand.getMaxBlocks(itemstack), playerShim.countItems(sourceItems, false));

                // Use modified WandWorker that accepts block matcher
                PatternWandWorker worker = new PatternWandWorker(
                    this.wand,
                    playerShim,
                    worldShim,
                    palette,
                    matcher,
                    itemstack, // Pass wand item for pattern access
                    clickedPos // Pass origin for relative coordinates
                );

                java.util.LinkedList<Point3d> blocks = worker.getBlockPositionList(
                    clickedPos,
                    ForgeDirection.getOrientation(side),
                    numBlocks,
                    getMode(itemstack),
                    getFaceLock(itemstack),
                    getFluidMode(itemstack),
                    false); // isNBTSensitive - false for now, could be enhanced later

                List<Point3d> placedBlocks = worker
                    .placeBlocks(itemstack, blocks, clickedPos, sourceItems, playerShim, side, hitX, hitY, hitZ);

                // Save placed blocks to NBT for undo feature
                if (!placedBlocks.isEmpty()) {
                    int[] placedIntArray = new int[placedBlocks.size() * 3];
                    for (int i = 0; i < placedBlocks.size(); i++) {
                        Point3d currentPoint = placedBlocks.get(i);
                        placedIntArray[i * 3] = currentPoint.x;
                        placedIntArray[i * 3 + 1] = currentPoint.y;
                        placedIntArray[i * 3 + 2] = currentPoint.z;
                    }
                    NBTTagCompound itemNBT = itemstack.hasTagCompound() ? itemstack.getTagCompound()
                        : new NBTTagCompound();
                    NBTTagCompound bbwCompound = new NBTTagCompound();
                    if (itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND)) {
                        bbwCompound = itemNBT.getCompoundTag("bbw");
                    }
                    bbwCompound.setIntArray("lastPlaced", placedIntArray);
                    itemstack.setTagInfo("bbw", bbwCompound);
                }
            }
        }

        return true;
    }

    /**
     * Get the proper item stack for placement using palette matching.
     */
    private ItemStack getProperItemStack(IWorldShim worldShim, IPlayerShim playerShim, Point3d clickedPos,
        BlockMatcher matcher) {
        Block worldBlock = worldShim.getBlock(clickedPos);
        int worldMeta = worldShim.getMetadata(clickedPos);

        // Check if clicked block is in palette
        if (matcher.matches(worldBlock, worldMeta)) {
            return new ItemStack(worldBlock, 1, worldMeta);
        }

        return null;
    }

    /**
     * Get the palette from wand NBT or create default.
     */
    public PatternPalette getPalette(ItemStack wand) {
        if (wand != null && wand.hasTagCompound()) {
            NBTTagCompound itemNBT = wand.getTagCompound();
            if (itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound bbwNBT = itemNBT.getCompoundTag("bbw");
                if (bbwNBT.hasKey("palette", Constants.NBT.TAG_LIST)) {
                    NBTTagList paletteList = bbwNBT.getTagList("palette", Constants.NBT.TAG_COMPOUND);
                    return PatternPalette.fromNBT(paletteList);
                }
            }
        }

        // Return empty palette by default
        return new PatternPalette();
    }

    /**
     * Save the palette to wand NBT.
     */
    public void savePalette(ItemStack wand, PatternPalette palette) {
        NBTTagCompound itemNBT = wand.hasTagCompound() ? wand.getTagCompound() : new NBTTagCompound();
        NBTTagCompound bbwNBT = itemNBT.hasKey("bbw", Constants.NBT.TAG_COMPOUND) ? itemNBT.getCompoundTag("bbw")
            : new NBTTagCompound();

        NBTTagList paletteNBT = palette.toNBT();
        bbwNBT.setTag("palette", paletteNBT);

        itemNBT.setTag("bbw", bbwNBT);
        wand.setTagCompound(itemNBT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack itemstack, EntityPlayer player, List lines, boolean extraInfo) {
        super.addInformation(itemstack, player, lines, extraInfo);

        // Add palette info
        PatternPalette palette = getPalette(itemstack);
        int paletteSize = palette.size();
        lines.add(StatCollector.translateToLocal("patternwand.palette") + ": " + paletteSize + " blocks");
    }

    @Override
    public EnumLock getFaceLock(ItemStack itemStack) {
        if (getMode(itemStack) == EnumLock.HORIZONTAL) {
            return EnumLock.HORIZONTAL;
        }
        return EnumLock.NOLOCK;
    }

    @Override
    public void nextMode(ItemStack itemStack, EntityPlayer player) {
        // Cycle through all lock modes
        switch (getMode(itemStack)) {
            case NORTHSOUTH:
                setMode(itemStack, EnumLock.EASTWEST);
                break;
            case VERTICAL:
                setMode(itemStack, EnumLock.NORTHSOUTH);
                break;
            case VERTICALEASTWEST:
                setMode(itemStack, EnumLock.NOLOCK);
                break;
            case EASTWEST:
                setMode(itemStack, EnumLock.VERTICALNORTHSOUTH);
                break;
            case HORIZONTAL:
                setMode(itemStack, EnumLock.VERTICAL);
                break;
            case VERTICALNORTHSOUTH:
                setMode(itemStack, EnumLock.VERTICALEASTWEST);
                break;
            case NOLOCK:
                setMode(itemStack, EnumLock.HORIZONTAL);
                break;
        }
    }

    @Override
    public void nextFluidMode(ItemStack itemStack, EntityPlayer player) {
        // Cycle through fluid lock modes
        switch (getFluidMode(itemStack)) {
            case STOPAT:
                setFluidMode(itemStack, EnumFluidLock.IGNORE);
                break;
            case IGNORE:
                setFluidMode(itemStack, EnumFluidLock.STOPAT);
                break;
        }
    }

    @Override
    public void getSubItems(Item item, CreativeTabs creativeTabs, java.util.List list) {
        list.add(new ItemStack(item, 1, 0));
    }
}
