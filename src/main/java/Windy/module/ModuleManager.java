package Windy.module;

import java.util.HashMap;
import java.util.Set;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
public class ModuleManager {
    private final HashMap<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        this.modules = new HashMap<>();
        register();
    }

    public Module getModule(Class<? extends Module> module) {
        return modules.get(module);
    }

    public Module getModule(String name) {
        for(Module module: modules.values()) {
            if(module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }

    public Module[] getModules(Category category) {
        return getModules().values().stream().filter(module -> module.getCategory() == category).toArray(Module[]::new);
    }

    public void register() {
        final Reflections refl = new Reflections("Windy.module.impl");
        final Set<Class<? extends Module>> classes = refl.getSubTypesOf(Module.class);

        for(Class<? extends Module> c : classes) {
            try {
                final Module module = c.newInstance();
                modules.put(c, module);
            } catch (InstantiationException | IllegalAccessException exception) {}
        }
    }

    public void unregister(Module... module){
        for(Module mod: module) {
            modules.remove(mod.getClass());
        }
    }
}
