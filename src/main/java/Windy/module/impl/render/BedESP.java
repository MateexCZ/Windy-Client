package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.inventory.BedSearch;
import Windy.util.render.ModuleGeometry;
import Windy.util.render.VisualTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;


@ModuleInfo(name = "BedESP", description = "Highlights both halves of nearby beds", category = Category.Render)
public final class BedESP extends Module {
    private final NumberSetting radius = new NumberSetting("Scan Radius", 16, 4, 32, 1);
    private final BooleanSetting full = new BooleanSetting("Full Block Height", false);
    private final BooleanSetting theme = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", VisualTheme.DEFAULT_ACCENT);
    private List<BlockPos> beds = new ArrayList<>();
    private long nextScan;

    public BedESP() {
        addSettings(radius, full, theme, color);
    }

    @Override
    public void onUpdate() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            beds.clear();
            return;
        }
        if (System.nanoTime() < nextScan) return;
        nextScan = System.nanoTime() + 500_000_000L;
        beds = BedSearch.heads((int) radius.getVal());
    }

    @Override
    public void on3D(float partialTicks) {
        if (mc.theWorld == null) return;
        for (BlockPos head : beds) {
            IBlockState state = mc.theWorld.getBlockState(head);
            if (!(state.getBlock() instanceof BlockBed)) continue;
            BlockPos foot = head.offset(state.getValue(BlockBed.FACING).getOpposite());
            if (!(mc.theWorld.getBlockState(foot).getBlock() instanceof BlockBed)) continue;
            AxisAlignedBB box = new AxisAlignedBB(Math.min(head.getX(), foot.getX()), head.getY(),
                    Math.min(head.getZ(), foot.getZ()), Math.max(head.getX(), foot.getX()) + 1,
                    head.getY() + (full.isState() ? 1 : .5625), Math.max(head.getZ(), foot.getZ()) + 1);
            ModuleGeometry.box(box, theme.isState() ? VisualTheme.accent() : color.getColor(), true);
        }
    }

    @Override
    public void onDisable() {
        beds.clear();
        nextScan = 0;
    }
}
