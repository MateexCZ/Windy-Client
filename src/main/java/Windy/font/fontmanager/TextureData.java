package Windy.font.fontmanager;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class TextureData {
    private final int textureId;
    private final int width, height;
    private final ByteBuffer buffer;
}
