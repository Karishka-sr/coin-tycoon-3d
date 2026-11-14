package com.tycoon.voxel;

/**
 * The hotbar: which blocks the player can place, and which one is selected.
 *
 * <p>Mined blocks are stored as counts, so building costs the blocks you dug up
 * rather than being free. Only blocks that can be placed ever enter the bar.
 */
public class VoxelInventory {

    /** The placeable blocks, in hotbar order. */
    public static final BlockType[] HOTBAR = {
        BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.COBBLESTONE,
        BlockType.SAND, BlockType.WOOD, BlockType.PLANK, BlockType.BRICK, BlockType.GLASS,
    };

    private final int[] counts = new int[HOTBAR.length];
    private int selected = 0;

    /** Blocks granted at the start so the player can build before mining. */
    public void fillStartingKit() {
        counts[slotOf(BlockType.COBBLESTONE)] = 32;
        counts[slotOf(BlockType.PLANK)] = 16;
        counts[slotOf(BlockType.GLASS)] = 8;
    }

    public int selectedIndex() {
        return selected;
    }

    public BlockType selectedBlock() {
        return HOTBAR[selected];
    }

    public void select(int index) {
        selected = Math.floorMod(index, HOTBAR.length);
    }

    public void selectNext() {
        select(selected + 1);
    }

    public void selectPrevious() {
        select(selected - 1);
    }

    /** Selects a hotbar slot by block type, used by the HUD buttons. */
    public void selectBlock(BlockType type) {
        int slot = slotOf(type);
        if (slot >= 0) {
            selected = slot;
        }
    }

    public int count(BlockType type) {
        int slot = slotOf(type);
        return slot < 0 ? 0 : counts[slot];
    }

    public int countAt(int slot) {
        return counts[slot];
    }

    public boolean has(BlockType type) {
        return count(type) > 0;
    }

    /** Adds mined blocks; returns how many were actually stored. */
    public int add(BlockType type, int amount) {
        int slot = slotOf(type);
        if (slot < 0 || amount <= 0) {
            return 0;
        }
        counts[slot] += amount;
        return amount;
    }

    /** Consumes one block of the selected type when available. */
    public boolean consumeSelected() {
        if (counts[selected] <= 0) {
            return false;
        }
        counts[selected]--;
        return true;
    }

    public int[] snapshot() {
        return counts.clone();
    }

    public void restore(int[] saved) {
        if (saved == null) {
            return;
        }
        System.arraycopy(saved, 0, counts, 0, Math.min(saved.length, counts.length));
    }

    private static int slotOf(BlockType type) {
        for (int i = 0; i < HOTBAR.length; i++) {
            if (HOTBAR[i] == type) {
                return i;
            }
        }
        return -1;
    }
}
