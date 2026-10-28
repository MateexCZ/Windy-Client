package Windy.util.combat;

import Windy.Windy;
import Windy.module.Module;
import Windy.module.impl.combat.KillAura;
import Windy.module.impl.movement.Scaffold;




public final class PortInput {
    
    private PortInput() {
    }
    
    public static boolean blockManualAction() {
        if (Windy.INSTANCE.getModuleManager() == null) return false;
        Module scaffold = Windy.INSTANCE.getModuleManager().getModule(Scaffold.class);
        if (scaffold != null && scaffold.isToggled()) return true;
        Module aura = Windy.INSTANCE.getModuleManager().getModule(KillAura.class);
        return aura instanceof KillAura && ((KillAura)aura).blocksManualAction();
    }
    
    public static net.minecraft.item.ItemStack renderStack(net.minecraft.item.ItemStack stack) {
        if (Windy.INSTANCE.getModuleManager() == null) return stack;
        Module module = Windy.INSTANCE.getModuleManager().getModule(Scaffold.class);
        return module instanceof Scaffold && module.isToggled() ? ((Scaffold)module).renderStack(stack) : stack;
    }
    
    public static boolean scroll(int delta) {
        Module module = Windy.INSTANCE.getModuleManager().getModule(Scaffold.class);
        return module instanceof Scaffold && module.isToggled() && ((Scaffold)module).scrollSlot(delta);
    }
    
    public static boolean blockCancelUse() {
        Module module = Windy.INSTANCE.getModuleManager().getModule(KillAura.class);
        return module instanceof KillAura && module.isToggled() && ((KillAura)module).isBlocking();
    }
}
