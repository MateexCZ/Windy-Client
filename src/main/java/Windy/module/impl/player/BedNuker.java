package Windy.module.impl.player;

import Windy.event.impl.MotionEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.inventory.BedSearch;
import Windy.util.render.ModuleGeometry;
import net.minecraft.block.BlockBed;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;


@ModuleInfo(name = "BedNuker", description = "Breaks nearby beds using normal mining progress", category = Category.Player)
public final class BedNuker extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4, 1, 5, .1);
    private final BooleanSetting rotations = new BooleanSetting("Rotations", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private BlockPos target;
    private long nextScan;

    public BedNuker() {
        addSettings(range, rotations, throughWalls);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (event.isPre()) {
            if (target != null && (!(mc.theWorld.getBlockState(target).getBlock() instanceof BlockBed) ||
                    mc.thePlayer.getDistanceSqToCenter(target) > range.getVal() * range.getVal())) reset();
            if (target == null && System.nanoTime() >= nextScan) {
                nextScan = System.nanoTime() + 200_000_000L;
                double closest = range.getVal() * range.getVal();
                for (BlockPos bed : BedSearch.heads((int) Math.ceil(range.getVal()))) {
                    double distance = mc.thePlayer.getDistanceSqToCenter(bed);
                    if (distance >= closest) continue;
                    Vec3 center = new Vec3(bed).addVector(.5, .3, .5);
                    net.minecraft.util.MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1), center);
                    if (!throughWalls.isState() && hit != null && !bed.equals(hit.getBlockPos())) continue;
                    target = bed;
                    closest = distance;
                }
            }
            if (target != null && rotations.isState()) {
                float[] rotation = ModuleGeometry.rotations(new Vec3(target).addVector(.5, .3, .5));
                event.setYaw(rotation[0]);
                event.setPitch(rotation[1]);
            }
        } else if (target != null) {
            mc.playerController.onPlayerDamageBlock(target, EnumFacing.UP);
            mc.thePlayer.swingItem();
        }
    }

    private void reset() {
        if (target != null && mc.playerController != null) mc.playerController.resetBlockRemoving();
        target = null;
    }

    @Override
    public void onDisable() {
        reset();
        nextScan = 0;
    }
}
