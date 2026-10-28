package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.render.VisualTheme;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.src.Config;


@ModuleInfo(name="Atmosphere",description="Soft colored distance fog with smooth transitions",category=Category.Render)
public final class Atmosphere extends Module {
    private static Atmosphere instance;
    private final ModeSetting palette=new ModeSetting("Palette","Theme","Custom","Soft Blue","Lavender","Warm","Neutral");
    private final NumberSetting start=new NumberSetting("Fog Start",18,0,128,1);
    private final NumberSetting end=new NumberSetting("Fog End",90,16,256,1);
    private final NumberSetting tint=new NumberSetting("Tint",.35,0,1,.05);
    private final Windy.setting.impl.ColorSetting customColor = new Windy.setting.impl.ColorSetting("Mist Color", Windy.util.render.VisualTheme.DEFAULT_ACCENT);
    private float weight;
    private long lastFrame;
    public Atmosphere(){instance=this;addSettings(palette,start,end,tint,customColor);}
    public static void update(float partialTicks){
        if(instance==null)return;
        boolean eligible=mc.theWorld!=null&&mc.getRenderViewEntity()!=null&&mc.theWorld.provider.getDimensionId()==0&&!Config.isShaders();
        Entity e=mc.getRenderViewEntity();
        if(eligible){
            Material material=ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld,e,partialTicks).getMaterial();
            eligible=material!=Material.water&&material!=Material.lava&&!(e instanceof EntityLivingBase&&((EntityLivingBase)e).isPotionActive(Potion.blindness));
        }
        long now=System.nanoTime();float dt=instance.lastFrame==0?0:Math.min(.1f,(now-instance.lastFrame)/1_000_000_000f);instance.lastFrame=now;
        if(!eligible){instance.weight=0;return;}
        instance.weight+=((instance.isToggled()?1:0)-instance.weight)*(1-(float)Math.exp(-5*dt));
    }
    private static int color(){
        if(instance==null)return 0xFFFFFF;
        switch(instance.palette.getCurrMode()){
            case "Theme": return Windy.util.render.VisualTheme.accent();
            case "Custom": return instance.customColor.getColor();
            case "Lavender":return 0xC6BCDD;
            case "Warm":return 0xE1C8B7;
            case "Neutral":return 0xCCD0D5;
            default:return 0xB8CCE0;
        }
    }
    public static float channel(float vanilla,int shift){
        if(instance==null||instance.weight<.001f)return vanilla;
        float target=((color()>>shift)&255)/255f,amount=instance.weight*(float)instance.tint.getVal();
        return vanilla+(target-vanilla)*amount;
    }
    public static float fogStart(float vanilla, float farPlane){
        if(instance==null)return vanilla;
        float targetEnd=Math.min(farPlane,(float)Math.max(instance.start.getVal()+1,instance.end.getVal()));
        float targetStart=Math.min((float)instance.start.getVal(),Math.max(0,targetEnd-1));
        return vanilla+(targetStart-vanilla)*instance.weight;
    }
    public static float fogEnd(float vanilla){
        if(instance==null)return vanilla;
        float target=Math.min(vanilla,(float)Math.max(instance.start.getVal()+1,instance.end.getVal()));
        return vanilla+(target-vanilla)*instance.weight;
    }
}
