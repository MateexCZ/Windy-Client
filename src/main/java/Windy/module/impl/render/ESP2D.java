package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.util.render.ModuleGeometry;
import Windy.util.render.VisualTheme;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;


@ModuleInfo(name = "ESP", description = "Interpolated player boxes", category = Category.Render)
public final class ESP2D extends Module {
    private final BooleanSetting nameTags = new BooleanSetting("Name Tags", true);
    private final BooleanSetting self = new BooleanSetting("Self", false);
    private final BooleanSetting invisible = new BooleanSetting("Invisibles", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", true);
    private final BooleanSetting theme = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", VisualTheme.DEFAULT_ACCENT);

    public ESP2D() {
        addSettings(nameTags, self, invisible, throughWalls, theme, color);
    }

    @Override
    public void on3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.isDead || player.getHealth() <= 0) continue;
            if (player == mc.thePlayer && (!self.isState() || mc.gameSettings.thirdPersonView == 0)) continue;
            if (player.isInvisible() && !invisible.isState()) continue;
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            AxisAlignedBB box = player.getEntityBoundingBox().offset(x - player.posX, y - player.posY, z - player.posZ);
            if (nameTags.isState()) Windy.util.render.EspNameTags.draw(player, x, y + player.height + .35, z);
            ModuleGeometry.box(box, theme.isState() ? VisualTheme.accent() : color.getColor(), throughWalls.isState());
        }
    }
}
