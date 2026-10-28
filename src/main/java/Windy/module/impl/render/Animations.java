package Windy.module.impl.render;


import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.time.TimerUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

@ModuleInfo(name="SwingAnimations",description="Changes animations of items",category=Category.Render)
public final class Animations extends Module {
    private static Animations instance;
    private final TimerUtil timerUtil = new TimerUtil();
    private final ModeSetting block = new ModeSetting("Block Style", "Smooth", "1.7", "Stella", "Exhibition", "Sigma", "Slide", "Vanilla");
    private final ModeSetting swing = new ModeSetting("Swing Style", "Vanilla", "Smooth", "Punch", "Spin", "Jello", "Swank", "Astolfo");
    private final NumberSetting x = new NumberSetting("Item X", 0, -2, 2, 0.05);
    private final NumberSetting y = new NumberSetting("Item Y", 0, -2, 2, 0.05);
    private final NumberSetting z = new NumberSetting("Item Z", 0, -2, 2, 0.05);
    private final NumberSetting size = new NumberSetting("Item Scale", 1, 0.2, 2, 0.05);
    private final NumberSetting swingSpeed = new NumberSetting("Swing Speed", 1, 0.1, 3, 0.05);

    public Animations() {
        instance = this;
        addSettings(block, swing, x, y, z, size, swingSpeed);
    }

    public ModeSetting getAnimationMode() {
        return block;
    }


    @Override
    public void onEnable() {
        super.onEnable();
    }
    @Override
    public void onDisable() {
        super.onDisable();
    }

    public static boolean finishSwing() {
        return instance != null && instance.isToggled();
    }

    public interface Transform {

        void apply(float equip, float swing);
    }

    private static boolean active(ItemStack item) {
        return instance != null && instance.isToggled() && item != null && item.getItem() instanceof ItemSword;
    }

    public static void viewmodel(ItemStack item) {
        if (!active(item)) return;
        GlStateManager.translate(instance.x.getVal(), instance.y.getVal(), instance.z.getVal());
        float scale = (float)instance.size.getVal();
        GlStateManager.scale(scale, scale, scale);
    }

    public static int swingDuration(net.minecraft.entity.EntityLivingBase entity, int vanillaTicks) {
        if (instance == null || !instance.isToggled() || entity != mc.thePlayer) return vanillaTicks;
        return Math.max(1, (int)Math.round(vanillaTicks / Math.max(0.1, instance.swingSpeed.getVal())));
    }

    public static float progress(ItemStack item, float progress) {
        return progress;
    }

    public static void swing(ItemStack item, float progress) {
        if (!active(item)) return;
        float wave = (float)Math.sin(Math.sqrt(progress) * Math.PI);
        switch (instance.swing.getCurrMode()) {
            case "Smooth":
                GlStateManager.translate(0, -wave * 0.12F, wave * 0.1F);
                break;

            case "Punch":
                GlStateManager.translate(-wave * 0.12F, 0, -wave * 0.4F);
                break;

            case "Spin":
                GlStateManager.rotate(wave * 180, 0, 0, 1);
                break;
            case "Jello":
                GlStateManager.translate(-wave * 0.08F, wave * 0.06F, -wave * 0.12F);
                GlStateManager.rotate(-wave * 35, 1, 0, 0);
                break;
            case "Swank":
                GlStateManager.rotate(wave * 45, 0, 0, 1);
                break;
            case "Astolfo":
                GlStateManager.rotate(wave * 120, 0, 1, 0);
                GlStateManager.translate(0, wave * 0.08F, 0);
                break;

            default:
                break;

        }
    }

    public static boolean block(ItemStack item, float equip, float progress, Transform transform, Runnable vanillaBlock) {
        if (!active(item)) return false;
        float swing = progress;
        float wave = (float)Math.sin(Math.sqrt(progress) * Math.PI);
        float waveSquared = (float)Math.sin(progress * progress * Math.PI);
        switch (instance.getAnimationMode().getCurrMode()) {
            case "Stella":
                transform.apply(-0.1F, swing);
                GlStateManager.translate(-0.5F, 0.4F, -0.2F);
                GlStateManager.rotate(32, 0, 1, 0);
                GlStateManager.rotate(-70, 1, 0, 0);
                GlStateManager.rotate(40, 0, 1, 0);
                break;

            case "1.7":
                transform.apply(equip, swing);
                GlStateManager.translate(0, 0.3F, 0);
                vanillaBlock.run();
                break;

            case "Exhibition":
                transform.apply(equip / 2, 0);
                GlStateManager.rotate(-wave * 20, wave / 2, 0, 9);
                GlStateManager.rotate(-wave * 30, 1, wave / 2, 0);
                vanillaBlock.run();
                GlStateManager.translate(-0.05F, 0, 0.1F);
                break;

            case "Sigma":
                transform.apply(equip * 0.5F, 0);
                GlStateManager.rotate(-waveSquared * 27.5F, -8, 0, 9);
                GlStateManager.rotate(-waveSquared * 45, 1, waveSquared / 2, 0);
                vanillaBlock.run();
                GlStateManager.translate(0.2F, 0.1F, 0.7F);
                break;

            case "Slide":
                transform.apply(equip / 2, 0);
                GlStateManager.translate(-wave * 0.25F, wave * 0.15F, 0);
                vanillaBlock.run();
                break;

            case "Vanilla":
                transform.apply(equip, 0);
                vanillaBlock.run();
                break;

            default:
                transform.apply(progress / 5, swing);
                GlStateManager.translate(0, 0.2F, 0);
                GlStateManager.rotate(-waveSquared, 4, -0.8F, -1);
                vanillaBlock.run();
                break;

        }
        return true;
    }


}
