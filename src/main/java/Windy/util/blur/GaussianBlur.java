package Windy.util.blur;

import Windy.util.math.MathUtils;
import Windy.util.render.RenderUtil;
import Windy.util.render.ShaderUtil;
import Windy.util.render.StencilUtil;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.glUniform1;

public class GaussianBlur {

    private static final ShaderUtil gaussianBlur = new ShaderUtil("windy/shader/gaussian.frag");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static final FloatBuffer weights = BufferUtils.createFloatBuffer(256);
    private static float cachedRadius = -1;

    private static void setupUniforms(float dir1, float dir2, float radius) {
        radius = Math.max(1, Math.min(127, radius));
        gaussianBlur.setUniformi("textureIn", 0);
        gaussianBlur.setUniformf("texelSize", 1.0F / (float) mc.displayWidth, 1.0F / (float) mc.displayHeight);
        gaussianBlur.setUniformf("direction", dir1, dir2);
        gaussianBlur.setUniformf("radius", radius);

        if (cachedRadius != radius) {
            weights.clear();
            for (int i = 0; i <= radius; i++) {
                weights.put(MathUtils.calculateGaussianValue(i, radius / 2));
            }

            while (weights.position() < weights.capacity()) {
                weights.put(0);
            }
            weights.flip();
            cachedRadius = radius;
        }
        weights.rewind();
        glUniform1(gaussianBlur.getUniform("weights"), weights);
    }
    public static void startBlur(){
        StencilUtil.initStencilToWrite();
    }
    public static void endBlur(float radius, float compression) {
        StencilUtil.readStencilBuffer(1);

        framebuffer = RenderUtil.createFrameBuffer(framebuffer);

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(compression, 0, radius);

        RenderUtil.bindTexture(mc.getFramebuffer().framebufferTexture);
        ShaderUtil.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.unload();

        mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.init();
        setupUniforms(0, compression, radius);

        RenderUtil.bindTexture(framebuffer.framebufferTexture);
        ShaderUtil.drawQuads();
        gaussianBlur.unload();

        StencilUtil.uninitStencilBuffer();
        RenderUtil.resetColor();
        GlStateManager.bindTexture(0);

    }

}
