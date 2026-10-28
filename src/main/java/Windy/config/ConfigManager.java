package Windy.config;

import Windy.Windy;
import Windy.module.Module;
import Windy.module.ModuleManager;
import Windy.setting.Setting;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.KeybindSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.chat.ChatUtil;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;

public class ConfigManager {
    public static File configFile;
    private final Gson gson;
    private final ModuleManager moduleManager;

    public ConfigManager() {
        configFile = new File(Windy.INSTANCE.getMc().mcDataDir, "windy_config.json");
        gson = new GsonBuilder().setPrettyPrinting().create();
        moduleManager = Windy.INSTANCE.getModuleManager();
    }

    public void saveConfig() {
        try {
            ConfigData configData = new ConfigData();
            configData.modules = new ArrayList<>();

            for (Module module : moduleManager.getModules().values()){
                ModuleConfig moduleConfig = new ModuleConfig();
                moduleConfig.name = module.getName();
                moduleConfig.toggled = module.isToggled();
                moduleConfig.settings = new ArrayList<>();

                for (Setting setting : module.getSettingList()) {
                    SettingConfig settingConfig = new SettingConfig();
                    settingConfig.name = setting.getName();
                    settingConfig.type = setting.getClass().getSimpleName();

                    if (setting instanceof ColorSetting) {
                        settingConfig.value = ((ColorSetting) setting).toHex();
                    } else if (setting instanceof NumberSetting) {
                        settingConfig.value = ((NumberSetting) setting).getVal();
                    } else if (setting instanceof BooleanSetting) {
                        settingConfig.value = ((BooleanSetting) setting).isState();
                    } else if (setting instanceof ModeSetting) {
                        settingConfig.value = ((ModeSetting) setting).getCurrMode();
                    } else if (setting instanceof KeybindSetting) {
                        settingConfig.value = ((KeybindSetting) setting).getCode();
                    }

                    moduleConfig.settings.add(settingConfig);
                }

                configData.modules.add(moduleConfig);
            }

            FileWriter writer = new FileWriter(configFile);
            gson.toJson(configData, writer);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String resolveSettingName(Module module, String name) {
        if (!"Scaffold".equalsIgnoreCase(module.getName()) || name == null) return name;
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "telly-start-rotation-min-speed" -> "Fast Turn Min";
            case "telly-start-rotation-max-speed" -> "Fast Turn Max";
            case "telly-normal-rotation-min-speed" -> "Slow Turn Min";
            case "telly-normal-rotation-max-speed" -> "Slow Turn Max";
            case "ground-motion" -> "Ground Speed %";
            case "air-motion" -> "Air Speed %";
            case "speed-motion" -> "Potion Speed %";
            case "item-spoof" -> "Keep Held Item";
            default -> name;
        };
    }

    public void loadConfig () {
        try {
            if (!configFile.exists()) {
                saveConfig();
                return;
            }

            ConfigData configData;
            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<ConfigData>() {
                }.getType();
                configData = gson.fromJson(reader, type);
            }

            if (configData == null || configData.modules == null) return;

            for (ModuleConfig moduleConfig : configData.modules) {
                Module module = moduleManager.getModule(moduleConfig.name);
                if (module == null && "Ambience".equalsIgnoreCase(moduleConfig.name)) module = moduleManager.getModule("TimeChange");
                if (module == null && "Animations".equalsIgnoreCase(moduleConfig.name)) module = moduleManager.getModule("SwingAnimations");
                if (module != null) {

                    // 1. NEJPRVE načteme všechna nastavení modulu
                    if (moduleConfig.settings != null) {
                        for (SettingConfig settingConfig : moduleConfig.settings) {
                            String settingName = resolveSettingName(module, settingConfig.name);
                            final String resolvedName = "Step".equals(module.getName()) && "Rise height".equalsIgnoreCase(settingName) ? "Extended Height"
                                    : "SwingAnimations".equals(module.getName()) && "Swing Amount".equalsIgnoreCase(settingName) ? "Swing Speed" : settingName;
                            Setting setting = module.getSettingList().stream()
                                    .filter(s -> s.getName().equalsIgnoreCase(resolvedName))
                                    .findFirst()
                                    .orElse(null);

                            if (setting != null && settingConfig.value != null) {
                                try {
                                    String rawVal = String.valueOf(settingConfig.value);

                                    if (setting instanceof ColorSetting) {
                                        ((ColorSetting) setting).fromHex(rawVal);
                                    } else if (setting instanceof NumberSetting) {
                                        ((NumberSetting) setting).setVal(Double.parseDouble(rawVal));
                                    } else if (setting instanceof BooleanSetting) {
                                        ((BooleanSetting) setting).setState(Boolean.parseBoolean(rawVal));
                                    } else if (setting instanceof ModeSetting) {
                                        if ("Step".equals(module.getName()) && "Mode".equals(setting.getName())) {
                                            if (rawVal.equals("NCP Rise")) rawVal = "NCP Extended";
                                            else if (rawVal.equals("Vulcan Rise")) rawVal = "Vulcan";
                                        }
                                        ((ModeSetting) setting).setCurrMode(rawVal);
                                    } else if (setting instanceof KeybindSetting) {
                                        int code = (int) Double.parseDouble(rawVal);
                                        ((KeybindSetting) setting).setCode(code);
                                    }
                                } catch (Exception e) {
                                    if (Minecraft.getMinecraft().thePlayer != null) {
                                            ChatUtil.warning("Cannot load setting: " + settingConfig.name);
                                        } else {
                                            System.err.println("[Windy Config] Cannot load setting: " + settingConfig.name);
                                        }
                                }
                            }
                        }
                    }

                    // 2. AŽ PO NAČTENÍ Nastavení modul zapneme/vypneme
                    module.setEnabled(moduleConfig.toggled);
                    if ("Ambience".equalsIgnoreCase(moduleConfig.name) && moduleConfig.settings != null) {
                        Module weatherModule = moduleManager.getModule("Weather");
                        if (weatherModule != null) for (SettingConfig oldSetting : moduleConfig.settings) {
                            if (!"Weather".equalsIgnoreCase(oldSetting.name) || oldSetting.value == null) continue;
                            for (Setting weatherSetting : weatherModule.getSettingList()) {
                                if (weatherSetting instanceof ModeSetting && "Weather".equalsIgnoreCase(weatherSetting.getName())) {
                                    ModeSetting weatherMode = (ModeSetting) weatherSetting;
                                    String value = String.valueOf(oldSetting.value);
                                    int index = weatherMode.getModes().indexOf(value);
                                    if (index >= 0) {
                                        weatherMode.setCurrMode(value);
                                        weatherMode.setModeIndex(index);
                                        weatherModule.setEnabled(moduleConfig.toggled && !"Unchanged".equals(value));
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
