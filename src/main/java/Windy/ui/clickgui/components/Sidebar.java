package Windy.ui.clickgui.components;

import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.HashMap;
import java.util.Map;

public class Sidebar {

    private final Map<Integer, Animation> categoryHovers = new HashMap<>();
    private final Animation categoryIndicatorY = new Animation(220, Easing.EASE_OUT_CUBIC, 0f);


    private static int selectedCategoryIndex = -1;

    public void init(int panelY) {
        categoryIndicatorY.snap(panelY + 54 + ((selectedCategoryIndex + 1) * 26));
    }

    public void draw(int panelX, int panelY, int sidebarWidth, int panelHeight, int mouseX, int mouseY, float alpha) {
        RenderUtil.drawRoundedRect(panelX, panelY, sidebarWidth, panelHeight, 10, AnimationUtil.multiplyAlpha(0xFF111216, alpha));
        RenderUtil.drawRect(panelX + sidebarWidth - 1, panelY, 1, panelHeight, AnimationUtil.multiplyAlpha(0x10FFFFFF, alpha));

        CustomFont.LIGHT_72.drawStringWithShadow("Windy", panelX + 12, panelY + 8, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        int startY = panelY + 54;
        int itemH = 22;
        int targetY = startY + (selectedCategoryIndex + 1) * (itemH + 4);

        categoryIndicatorY.animateTo(targetY);
        float animIndY = categoryIndicatorY.get();

        RenderUtil.drawRoundedRect(panelX + 8, (int) animIndY, sidebarWidth - 16, itemH, 6, AnimationUtil.multiplyAlpha(0xFF3A3F50, alpha));
        RenderUtil.drawRoundedRect(panelX + 8,
                (int) animIndY + 3,
                3,
                itemH - 6,
                2,
                AnimationUtil.multiplyAlpha(VisualTheme.accent(), alpha));

        int totalItems = Category.values().length + 2;
        int itemY = startY;

        for (int i = -1; i < totalItems - 1; i++) {
            boolean isSelected = (selectedCategoryIndex == i);
            boolean hovered = mouseX >= panelX + 8 && mouseX <= panelX + sidebarWidth - 8 && mouseY >= itemY && mouseY < itemY + itemH;

            String label = i == -1 ? "All" : i == Category.values().length ? "Themes" : formatName(Category.values()[i].name());

            Animation hAnim = categoryHovers.computeIfAbsent(i, k -> new Animation(150, Easing.EASE_OUT_CUBIC, 0f));
            hAnim.animateTo((hovered && !isSelected) ? 1f : 0f);

            if (hAnim.get() > 0.01f) {
                RenderUtil.drawRoundedRect(panelX + 8,
                        itemY,
                        sidebarWidth - 16,
                        itemH,
                        6,
                        AnimationUtil.multiplyAlpha(0x0FFFFFFF, alpha * hAnim.get()));
            }

            int textColor = isSelected ? 0xFFFFFFFF : AnimationUtil.lerpColor(0xFFA0A6B8, 0xFFFFFFFF, hAnim.get());
            CustomFont.LIGHT_22.drawString(label, panelX + 18, itemY + 6, AnimationUtil.multiplyAlpha(textColor, alpha));

            itemY += itemH + 4;
        }
    }

    public boolean mouseClicked(int panelX, int panelY, int sidebarWidth, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;

        int itemY = panelY + 54;
        int itemH = 22;
        int totalItems = Category.values().length + 2;

        for (int i = -1; i < totalItems - 1; i++) {
            if (mouseX >= panelX + 8 && mouseX <= panelX + sidebarWidth - 8 && mouseY >= itemY && mouseY < itemY + itemH) {
                selectedCategoryIndex = i;
                return true;
            }
            itemY += itemH + 4;
        }
        return false;
    }

    public int getSelectedCategoryIndex() {
        return selectedCategoryIndex;
    }

    public void setSelectedCategoryIndex(int index) {
        selectedCategoryIndex = index;
    }

    private String formatName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }
}
