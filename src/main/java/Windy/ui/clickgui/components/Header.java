package Windy.ui.clickgui.components;

import Windy.font.CustomFont;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import lombok.Getter;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public class Header {

    private final StringBuilder searchQuery = new StringBuilder();
    @Getter
    private boolean focused = false;

    public void draw(int searchX, int searchY, int searchW, int searchH, float alpha) {
        int bg = focused ? 0xFF20232E : 0xFF1A1C24;
        RenderUtil.drawRoundedRect(searchX, searchY, searchW, searchH, 6, AnimationUtil.multiplyAlpha(bg, alpha));

        if (focused) {
            RenderUtil.drawRoundedRect(searchX, searchY, searchW, searchH, 6,  AnimationUtil.multiplyAlpha(bg, alpha));
        }

        boolean hasQuery = searchQuery.length() > 0;
        String textToShow = hasQuery ? searchQuery.toString() : (focused ? "" : "Search modules...");
        int textColor = hasQuery ? 0xFFFFFFFF : (focused ? 0xFFA0A6B8 : 0xFF5A6072);

        CustomFont.LIGHT_18.drawString(textToShow, searchX + 10, searchY + 7, AnimationUtil.multiplyAlpha(textColor, alpha));

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorOffset = (int) CustomFont.getWidth(searchQuery.toString());
            RenderUtil.drawRect(searchX + cursorOffset + 9, searchY + 6, 1, 10, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int searchX, int searchY, int searchW, int searchH) {
        if (mouseButton == 0) {
            focused = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH;
            return focused;
        }
        return false;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            focused = false;
            return true;
        }


        if (keyCode == Keyboard.KEY_BACK) {
            if (searchQuery.length() > 0) {
                searchQuery.deleteCharAt(searchQuery.length() - 1);
            }
            return true;
        } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && searchQuery.length() < 30) {
            searchQuery.append(typedChar);
            return true;
        }
        return false;
    }

    public String getSearchQuery() {
        return searchQuery.toString().trim();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
