package Windy.util.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;


public final class ClientLogo {
    private static final String SOURCE = "/assets/minecraft/windy/logo.png";
    private static BufferedImage image;
    private static ResourceLocation texture;

    private ClientLogo() { }


    public static BufferedImage image() throws IOException {
        if (image != null) return image;
        try (InputStream stream = ClientLogo.class.getResourceAsStream(SOURCE)) {
            if (stream == null) throw new IOException("Missing client logo: " + SOURCE);
            BufferedImage original = ImageIO.read(stream);
            if (original == null) throw new IOException("Invalid client logo PNG");
            int left=original.getWidth(), top=original.getHeight(), right=-1, bottom=-1;
            for (int y=0; y<original.getHeight(); y++) for (int x=0; x<original.getWidth(); x++) {
                if ((original.getRGB(x,y) >>> 24) != 0) {
                    left=Math.min(left,x); top=Math.min(top,y);
                    right=Math.max(right,x); bottom=Math.max(bottom,y);
                }
            }
            if (right < left) throw new IOException("Client logo is transparent");
            left=Math.max(0,left-2); top=Math.max(0,top-2);
            right=Math.min(original.getWidth()-1,right+2); bottom=Math.min(original.getHeight()-1,bottom+2);
            image=original.getSubimage(left,top,right-left+1,bottom-top+1);
            return image;
        }
    }

    public static float aspect() {
        try { return image().getWidth() / (float) image().getHeight(); }
        catch (IOException e) { throw new IllegalStateException("Cannot load Windy logo",e); }
    }

    public static float nativeWidth() {
        try { return image().getWidth(); }
        catch (IOException e) { throw new IllegalStateException("Cannot load Windy logo",e); }
    }

    public static float height(float width) { return width / aspect(); }

    public static void draw(float x, float y, float width, int color, float opacity) {
        if (width <= 0 || opacity <= 0) return;
        Minecraft mc=Minecraft.getMinecraft();
        int previousTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND);
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int alphaFunc=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        try {
            if (texture == null) {
                try { texture=mc.getTextureManager().getDynamicTextureLocation("windy_client_logo",new DynamicTexture(image())); }
                catch (IOException e) { throw new IllegalStateException("Cannot load Windy logo",e); }
                mc.getTextureManager().bindTexture(texture);
                TextureSampling.finishUpload();
            } else mc.getTextureManager().bindTexture(texture);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770,771,1,0);
            GlStateManager.alphaFunc(GL11.GL_GREATER,0);
            GlStateManager.color((color >> 16 & 255)/255f,(color >> 8 & 255)/255f,(color & 255)/255f,
                    (color >>> 24)/255f * Math.min(1,opacity));
            float height=height(width);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0,0); GL11.glVertex2f(x,y);
            GL11.glTexCoord2f(0,1); GL11.glVertex2f(x,y+height);
            GL11.glTexCoord2f(1,1); GL11.glVertex2f(x+width,y+height);
            GL11.glTexCoord2f(1,0); GL11.glVertex2f(x+width,y);
            GL11.glEnd();
        } finally {
            GlStateManager.bindTexture(previousTexture);
            GlStateManager.alphaFunc(alphaFunc,alphaRef);
            GlStateManager.tryBlendFuncSeparate(src,dst,srcAlpha,dstAlpha);
            if (!blend) GlStateManager.disableBlend();
            GlStateManager.color(1,1,1,1);
        }
    }


    public static ByteBuffer[] windowIcons() throws IOException {
        BufferedImage source=image();
        int[] sizes={16,32,48,256};
        ByteBuffer[] buffers=new ByteBuffer[sizes.length];
        for (int i=0;i<sizes.length;i++) {
            int size=sizes[i];
            BufferedImage icon=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=icon.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            float ratio=(size-2f)/Math.max(source.getWidth(),source.getHeight());
            int w=Math.max(1,Math.round(source.getWidth()*ratio)), h=Math.max(1,Math.round(source.getHeight()*ratio));
            graphics.drawImage(source,(size-w)/2,(size-h)/2,w,h,null);
            graphics.dispose();
            ByteBuffer buffer=ByteBuffer.allocateDirect(size*size*4);
            for (int y=0;y<size;y++) for (int x=0;x<size;x++) {
                int pixel=icon.getRGB(x,y);
                buffer.put((byte)(pixel>>16)).put((byte)(pixel>>8)).put((byte)pixel).put((byte)(pixel>>>24));
            }
            buffer.flip(); buffers[i]=buffer;
        }
        return buffers;
    }
}
