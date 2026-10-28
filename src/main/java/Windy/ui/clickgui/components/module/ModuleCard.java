package Windy.ui.clickgui.components.module;

import Windy.font.CustomFont;
import Windy.module.Module;
import Windy.setting.Setting;
import Windy.setting.impl.*;
import Windy.ui.clickgui.components.setting.*;
import Windy.ui.clickgui.components.setting.Boolean;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil.Easing;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;

public class ModuleCard {

    private final Module module;
    private final List<Comp<?>> settingComponents = new ArrayList<>();
    private final Map<Comp<?>, Setting> compSettingMap = new HashMap<>(); // Mapa pro sledování přiřazených settingů
    private final Animation hoverAnim = new Animation(150, Easing.EASE_OUT_CUBIC, 0f);
    private final Animation expandAnim = new Animation(200, Easing.EASE_OUT_CUBIC, 0f);

    @Setter
    private boolean expanded = false;

    public ModuleCard(Module module) {
        this.module = module;

        if (module.getSettingList() != null) {
            for (Setting s : module.getSettingList()) {
                Comp<?> comp = null;
                if (s instanceof ColorSetting) {
                    comp = new ColorComp((ColorSetting) s);
                } else if (s instanceof BooleanSetting) {
                    comp = new Boolean((BooleanSetting) s);
                } else if (s instanceof ModeSetting) {
                    comp = new Mode((ModeSetting) s);
                } else if (s instanceof NumberSetting) {
                    comp = new Slider((NumberSetting) s);
                } else if (s instanceof KeybindSetting) {
                    comp = new Keybind((KeybindSetting) s);
                }

                if (comp != null) {
                    settingComponents.add(comp);
                    compSettingMap.put(comp, s);
                }
            }
        }
    }

    // Pomocná metoda pro získání pouze aktuálně viditelných komponent
    private List<Comp<?>> getVisibleComponents() {
        List<Comp<?>> visible = new ArrayList<>();
        for (Comp<?> comp : settingComponents) {
            Setting s = compSettingMap.get(comp);
            if (s != null && s.isVisible()) {
                visible.add(comp);
            }
        }
        return visible;
    }

    public int draw(int x, int y, int width, int mouseX, int mouseY, float alpha, int minY, int maxY) {
        expandAnim.animateTo(expanded ? 1f : 0f);
        float expVal = expandAnim.get();

        int settingsH = calculateSettingsGridHeight();
        int currentSettingsH = (int) (settingsH * expVal);
        int cardH = 44 + currentSettingsH;

        if (y + cardH > minY && y < maxY) {
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + 44;
            hoverAnim.animateTo(hovered ? 1f : 0f);

            int bg = AnimationUtil.lerpColor(0xFF161820, 0xFF1E212B, hoverAnim.get());
            RenderUtil.drawRoundedRect(x, y, width, cardH, 8, AnimationUtil.multiplyAlpha(bg, alpha));

            // Indikátor zapnutého modulu
            if (module.isToggled()) {
                RenderUtil.drawRoundedRect(x + 2, y + 8, 3, 28, 2, AnimationUtil.multiplyAlpha(VisualTheme.accent(), alpha));
            }

            // Název modulu
            int titleColor = module.isToggled() ? 0xFFFFFFFF : 0xFFA0A6B8;
            CustomFont.LIGHT_22.drawString(module.getName(), x + 14, y + 9, AnimationUtil.multiplyAlpha(titleColor, alpha));

            // Popis
            String desc = (module.getDescription() != null && !module.getDescription().isEmpty()) ? module.getDescription() : "No description.";
            CustomFont.LIGHT_18.drawString(desc, x + 14, y + 24, AnimationUtil.multiplyAlpha(0xFF5A6072, alpha));

            // Šipka pro rozbalení (zobrazí se jen pokud má modul nějaké viditelné nastavení)
            List<Comp<?>> visibleComps = getVisibleComponents();
            if (!visibleComps.isEmpty()) {
                RenderUtil.drawChevron(x + width - 16, y + 20, 4, expVal * 90f, 1.8f, AnimationUtil.multiplyAlpha(0xFFA0A6B8, alpha));
            }

            // Vykreslení nastavení s elegantním odsazením
            if (currentSettingsH > 2 && !visibleComps.isEmpty()) {
                drawSettingsGrid(x + 12, y + 48, width - 24, mouseX, mouseY, alpha * expVal, minY, maxY);
            }
        }

        return cardH;
    }

    private void drawSettingsGrid(int startX, int startY, int totalW, int mouseX, int mouseY, float alpha, int minY, int maxY) {
        List<Comp<?>> visibleComps = getVisibleComponents();
        int colW = (totalW - 10) / 2; // Dva úhledné sloupce
        int curY = startY;

        for (int i = 0; i < visibleComps.size(); i += 2) {
            Comp<?> left = visibleComps.get(i);
            Comp<?> right = (i + 1 < visibleComps.size()) ? visibleComps.get(i + 1) : null;

            int leftH = left.getHeight();
            int rightH = (right != null) ? right.getHeight() : 0;
            int rowH = Math.max(leftH, rightH);

            if (curY + rowH > minY && curY < maxY) {
                left.setPosition(startX, curY, colW, leftH);
                left.draw(mouseX, mouseY, alpha);

                if (right != null) {
                    right.setPosition(startX + colW + 10, curY, colW, rightH);
                    right.draw(mouseX, mouseY, alpha);
                }
            }
            curY += rowH + 8; // Větší mezery mezi řádky pro vzdušnější vzhled
        }
    }

    public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY, int mouseButton) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + 44) {
            clearBindings();
            if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == 1) {
                expanded = !expanded;
            }
            return true;
        }

        if (expanded) {
            for (Comp<?> comp : getVisibleComponents()) {
                if (comp.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (expanded) {
            for (Comp<?> comp : getVisibleComponents()) {
                comp.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Comp<?> comp : getVisibleComponents()) {
                comp.keyTyped(typedChar, keyCode);
            }
        }
    }

    public void clearBindings() {
        for (Comp<?> c : getVisibleComponents()) {
            if (c instanceof Keybind) {
                ((Keybind) c).setBinding(false);
            }
        }
    }

    public int calculateSettingsGridHeight() {
        List<Comp<?>> visibleComps = getVisibleComponents();
        if (visibleComps.isEmpty()) return 0;
        int totalH = 0;
        for (int i = 0; i < visibleComps.size(); i += 2) {
            Comp<?> left = visibleComps.get(i);
            Comp<?> right = (i + 1 < visibleComps.size()) ? visibleComps.get(i + 1) : null;

            int leftH = left.getHeight();
            int rightH = (right != null) ? right.getHeight() : 0;

            totalH += Math.max(leftH, rightH) + 8;
        }
        return totalH + 12; // Větší padding nahoře a dole
    }

    public int getHeight() {
        return 44 + (int) (calculateSettingsGridHeight() * expandAnim.get());
    }

    public Module getModule() {
        return module;
    }

    private String formatName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

}
