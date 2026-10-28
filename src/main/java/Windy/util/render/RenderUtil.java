package Windy.util.render;

import java.awt.*;
import java.util.ConcurrentModificationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import static org.lwjgl.opengl.GL11.*;

public class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();


    public static void enable(final int glTarget) {
        GL11.glEnable(glTarget);
    }

    public static void disable(final int glTarget) {
        GL11.glDisable(glTarget);
    }


    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return framebuffer;
    }

    public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
        return framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight;
    }


    public static void start() {
        enable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disable(GL11.GL_TEXTURE_2D);
        disable(GL11.GL_CULL_FACE);
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void stop() {
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        enable(GL11.GL_CULL_FACE);
        enable(GL11.GL_TEXTURE_2D);
        disable(GL11.GL_BLEND);
        color(Color.white);
    }

    public static void renderBreadCrumb(final Vec3 vec3) {

        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        try {

            final double x = vec3.xCoord - (mc.getRenderManager()).renderPosX;
            final double y = vec3.yCoord - (mc.getRenderManager()).renderPosY;
            final double z = vec3.zCoord - (mc.getRenderManager()).renderPosZ;

            final double distanceFromPlayer = mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord - 1, vec3.zCoord);
            int quality = (int) (distanceFromPlayer * 4 + 10);

            if (quality > 350)
                quality = 350;

            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);

            final float scale = 0.04f;
            GL11.glScalef(-scale, -scale, -scale);

            GL11.glRotated(-(mc.getRenderManager()).playerViewY, 0.0D, 1.0D, 0.0D);
            GL11.glRotated((mc.getRenderManager()).playerViewX, 1.0D, 0.0D, 0.0D);

            final Color c = new Color(159, 24, 242);

            RenderUtil.drawFilledCircleNoGL(0, 0, 0.7, c.hashCode(), quality);

            if (distanceFromPlayer < 4)
                RenderUtil.drawFilledCircleNoGL(0, 0, 1.4, new Color(c.getRed(), c.getGreen(), c.getBlue(), 50).hashCode(), quality);

            if (distanceFromPlayer < 20)
                RenderUtil.drawFilledCircleNoGL(0, 0, 2.3, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30).hashCode(), quality);


            GL11.glScalef(0.8f, 0.8f, 0.8f);

            GL11.glPopMatrix();


        } catch (final ConcurrentModificationException ignored) {
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.enableDepth();

        GL11.glColor3d(255, 255, 255);
    }

    public static void drawFilledCircleNoGL(final int x, final int y, final double r, final int c, final int quality) {
        final float f = ((c >> 24) & 0xff) / 255F;
        final float f1 = ((c >> 16) & 0xff) / 255F;
        final float f2 = ((c >> 8) & 0xff) / 255F;
        final float f3 = (c & 0xff) / 255F;

        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        for (int i = 0; i <= 360 / quality; i++) {
            final double x2 = Math.sin(((i * quality * Math.PI) / 180)) * r;
            final double y2 = Math.cos(((i * quality * Math.PI) / 180)) * r;
            GL11.glVertex2d(x + x2, y + y2);
        }

        GL11.glEnd();
    }


    public static void color(final double red, final double green, final double blue, final double alpha) {
        GL11.glColor4d(red, green, blue, alpha);
    }

    public static void polygon(final double x,
            final double y,
            double sideLength,
            final double amountOfSides,
            final boolean filled,
            final Color color) {
        sideLength /= 2;
        start();
        if (color != null)
            color(color);
        if (!filled) GL11.glLineWidth(2);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        begin(filled ? GL11.GL_TRIANGLE_FAN : GL11.GL_LINE_STRIP);
        {
            for (double i = 0; i <= amountOfSides / 4; i++) {
                final double angle = i * 4 * (Math.PI * 2) / 360;
                vertex(x + (sideLength * Math.cos(angle)) + sideLength, y + (sideLength * Math.sin(angle)) + sideLength);
            }
        }
        end();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        stop();
    }

    public static void image(final ResourceLocation imageLocation, final float x, final float y, final float width, final float height) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        enable(GL11.GL_BLEND);
        GlStateManager.disableAlpha();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Minecraft.getMinecraft().getTextureManager().bindTexture(imageLocation);
        Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, (float) 0, (float) 0, (int) width, (int) height, width, height);
        GlStateManager.enableAlpha();
        disable(GL11.GL_BLEND);
    }


    public void polygon(final double x, final double y, final double sideLength, final int amountOfSides, final boolean filled) {
        polygon(x, y, sideLength, amountOfSides, filled, null);
    }

    public static void polygon(final double x, final double y, final double sideLength, final int amountOfSides, final Color color) {
        polygon(x, y, sideLength, amountOfSides, true, color);
    }

    public void polygon(final double x, final double y, final double sideLength, final int amountOfSides) {
        polygon(x, y, sideLength, amountOfSides, true, null);
    }


    public void circle(final double x, final double y, final double radius, final boolean filled, final Color color) {
        polygon(x, y, radius, 360, filled, color);
    }

    public void circle(final double x, final double y, final double radius, final boolean filled) {
        polygon(x, y, radius, 360, filled);
    }

    public static void circle(final double x, final double y, final double radius, final Color color) {
        polygon(x, y, radius, 360, color);
    }

    public void circle(final double x, final double y, final double radius) {
        polygon(x, y, radius, 360);
    }

    public void color(final double red, final double green, final double blue) {
        color(red, green, blue, 1);
    }

    public static void color(Color color) {
        if (color == null)
            color = Color.white;
        color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
    }

    public void color(Color color, final int alpha) {
        if (color == null)
            color = Color.white;
        color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 0.5);
    }

    public static void begin(final int glMode) {
        GL11.glBegin(glMode);
    }

    public static void end() {
        GL11.glEnd();
    }

    public static void vertex(final double x, final double y) {
        GL11.glVertex2d(x, y);
    }


    public static void drawRect(double x, double y, double width, double height, int color) {
        drawGradient((float) x, (float) y, (float) width, (float) height, color, color, color, color);
    }

    public static void resetColor() {
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawHollowRect(int x, int y, int width, int height, int lineW, int color) {
        Gui.drawRect(x, y,x + width,  y + lineW, color); // Top
        Gui.drawRect( x + width, y,  x + width + lineW,  y + height + 1, color); // Right
        Gui.drawRect(x, y,  x + lineW,  y + height, color); // Left
        Gui.drawRect(x, y + height, x + width,y + height + lineW, color); // Bottom
    }

    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        drawRoundedRect((float) x, y, width, height, radius, color);
    }


    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        if (radius <= 0) { drawRect(x, y, width, height, color); return; }
        RoundedShader.initialize();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GL20.glUseProgram(RoundedShader.program);
            GL20.glUniform2f(RoundedShader.size, width, height);
            GL20.glUniform1f(RoundedShader.radius, Math.min(radius, Math.min(width, height) / 2));
            GL20.glUniform4f(RoundedShader.tint, (color >> 16 & 255) / 255f,
                    (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
            GL11.glEnd();
        } finally {
            GL20.glUseProgram(previousProgram);
            GlStateManager.alphaFunc(alphaFunction, alphaReference);
            GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (!blend) GlStateManager.disableBlend();
            if (alpha) GlStateManager.enableAlpha();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    public static void drawPanel(float x, float y, float width, float height, float opacity) {
        drawRoundedRect(x, y, width, height, 10, ColorUtil.applyOpacity(0xFF323C4C, opacity * .65f));
        drawRoundedRect(x + 1, y + 1, width - 2, height - 2, 9,
                ColorUtil.applyOpacity(0xE8141922, opacity));
    }

    public static void drawBorder(float x, float y, float width, float height, int color) {
        drawRect(x, y, width, 1, color);
        drawRect(x, y + height - 1, width, 1, color);
        drawRect(x, y, 1, height, color);
        drawRect(x + width - 1, y, 1, height, color);
    }

    public static void drawCheckerboard(float x, float y, float width, float height) {
        drawRect(x, y, width, height, 0xFF343943);
        for (int row = 0; row * 4 < height; row++) {
            for (int column = 0; column * 4 < width; column++) {
                if ((row + column) % 2 == 0) drawRect(x + column * 4, y + row * 4,
                        Math.min(4, width - column * 4), Math.min(4, height - row * 4), 0xFF656B76);
            }
        }
    }

    public static boolean contains(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }


    public static void scissor(float x, float y, float width, float height,
                               float scale, float originX, float originY) {
        int factor = new ScaledResolution(mc).getScaleFactor();
        int left = Math.round((originX + x * scale) * factor);
        int top = Math.round((originY + y * scale) * factor);
        int right = Math.round((originX + (x + width) * scale) * factor);
        int bottom = Math.round((originY + (y + height) * scale) * factor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, mc.displayHeight - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
    }




    public static void drawCircle(float cx, float cy, float radius, int color) {
        if (radius <= 0f) return;
        beginShape(color);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();

        int segments = 40;

        for (int i = segments; i >= 0; i--) {
            double angle = Math.PI * 2.0 * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tessellator.draw();

        endShape();
    }

    public static void drawHorizontalGradient(int x, int y, int w, int h, int colorLeft, int colorRight) {
        drawGradient(x, y, w, h, colorLeft, colorRight, colorLeft, colorRight);
    }

    public static void drawVerticalGradient(int x, int y, int w, int h, int colorTop, int colorBottom) {
        drawGradient(x, y, w, h, colorTop, colorTop, colorBottom, colorBottom);
    }





    public static void drawChevron(float cx, float cy, float size, float rotationDeg, float lineWidthPx, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidthPx);

        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0f);
        GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(-size * 0.5f, -size);
        GL11.glVertex2f(size * 0.5f, 0f);
        GL11.glVertex2f(-size * 0.5f, size);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }





    private static void beginShape(int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
    }

    private static void endShape() {
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }


    public static void drawGradient(float x, float y, float width, float height,
                                    int topLeft, int topRight, int bottomLeft, int bottomRight) {
        if (width <= 0 || height <= 0) return;
        int shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer wr = tessellator.getWorldRenderer();
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            vertexColor(wr, x, y, topLeft);
            vertexColor(wr, x, y + height, bottomLeft);
            vertexColor(wr, x + width, y + height, bottomRight);
            vertexColor(wr, x + width, y, topRight);
            tessellator.draw();
        } finally {
            GlStateManager.shadeModel(shade);
            GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (texture) GlStateManager.enableTexture2D();
            if (!blend) GlStateManager.disableBlend();
            if (alpha) GlStateManager.enableAlpha();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    private static void vertexColor(WorldRenderer wr, double x, double y, int c) {
        wr.pos(x, y, 0).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).endVertex();
    }

    public static void color(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, alpha);
    }

    // Colors the next texture without a specified alpha value
    public static void color(int color) {
        color(color, (float) (color >> 24 & 255) / 255.0F);
    }

    /**
     * Bind a texture using the specified integer refrence to the texture.
     *
     * @see org.lwjgl.opengl.GL13 for more information about texture bindings
     */
    public static void bindTexture(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
    }


    public static void roundedRect(double x, double y, double width, double height, double radius, Color color) {
        drawRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius,
                color == null ? 0xFFFFFFFF : color.getRGB());
    }

    public static void rect(double x, double y, double width, double height, boolean filled, Color color) {
        int tint = color == null ? 0xFFFFFFFF : color.getRGB();
        if (filled) drawRect(x, y, width, height, tint);
        else drawBorder((float) x, (float) y, (float) width, (float) height, tint);
    }

    public void rect(final double x, final double y, final double width, final double height, final boolean filled) {
        rect(x, y, width, height, filled, null);
    }

    public static void rect(final double x, final double y, final double width, final double height, final Color color) {
        rect(x, y, width, height, true, color);
    }

    public void rect(final double x, final double y, final double width, final double height) {
        rect(x, y, width, height, true, null);
    }

    public static void scissor(double x, double y, double width, double height) {
        final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        final double scale = sr.getScaleFactor();

        y = sr.getScaledHeight() - y;

        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;

        GL11.glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }


    public static void renderGradientRectLeftRight(int left, int top, int right, int bottom,
                                                    int startColor, int endColor) {
        drawHorizontalGradient(left, top, right - left, bottom - top, startColor, endColor);
    }

    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }
    private static final class RoundedShader {
        private static int program, size, radius, tint;
    
        private static int compile(int type, String source) {
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetShaderInfoLog(shader, 4096));
            }
            return shader;
        }
    
        private static void initialize() {
            if (program != 0) return;
            int vertex = compile(GL20.GL_VERTEX_SHADER,
                    "#version 120\nvoid main(){gl_Position=ftransform();gl_TexCoord[0]=gl_MultiTexCoord0;}");
            int fragment = compile(GL20.GL_FRAGMENT_SHADER,
                    "#version 120\nuniform vec2 size; uniform float radius; uniform vec4 tint;"
                    + "void main(){vec2 p=gl_TexCoord[0].xy*size-size*.5;"
                    + "vec2 q=abs(p)-(size*.5-vec2(radius));"
                    + "float d=length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;"
                    + "float aa=max(fwidth(d),.25);"
                    + "float coverage=1.-smoothstep(-aa,aa,d);if(coverage<.001)discard;gl_FragColor=vec4(tint.rgb,tint.a*coverage);}");
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            size = GL20.glGetUniformLocation(program, "size");
            radius = GL20.glGetUniformLocation(program, "radius");
            tint = GL20.glGetUniformLocation(program, "tint");
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(program, 4096));
            }
        }
    
    }
}
