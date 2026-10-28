package Windy.util.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;


public final class BedSearch {
    private BedSearch() { }

    public static List<BlockPos> heads(int radius) {
        Minecraft mc = Minecraft.getMinecraft();
        List<BlockPos> result = new ArrayList<>();
        if (mc.theWorld == null || mc.thePlayer == null) return result;
        BlockPos center = new BlockPos(mc.thePlayer);
        for (int y = -4; y <= 4; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos position = center.add(x, y, z);
                    if (!mc.theWorld.isBlockLoaded(position)) continue;
                    net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(position);
                    if (state.getBlock() instanceof BlockBed &&
                            state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) result.add(position);
                }
            }
        }
        return result;
    }
}
