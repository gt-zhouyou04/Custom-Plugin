package com.gaotu.plugin.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageUtils {
    // 计算图像哈希值示例
    public String computeImageHash(File imageFile) throws Exception {
        BufferedImage image = ImageIO.read(imageFile);
        Image scaledImage = image.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage buffered = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        buffered.getGraphics().drawImage(scaledImage, 0, 0, null);

        int[] grayValues = new int[64];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int argb = buffered.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                grayValues[x + y * 8] = (r + g + b) / 3;
            }
        }

        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            hash.append(grayValues[i] > grayValues[(i+1)%64] ? '1' : '0');
        }
        return hash.toString();
    }
}
