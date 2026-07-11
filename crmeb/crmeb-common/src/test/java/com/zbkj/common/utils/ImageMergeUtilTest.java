package com.zbkj.common.utils;

import com.zbkj.common.vo.ImageMergeUtilVo;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageMergeUtilTest {

    @Test
    public void mergeUsesIndependentCoordinatesAndPreservesSourceFiles() throws Exception {
        File backgroundFile = File.createTempFile("merge-background", ".png");
        File overlayFile = File.createTempFile("merge-overlay", ".png");
        try {
            BufferedImage background = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    background.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
            BufferedImage overlay = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    overlay.setRGB(x, y, Color.RED.getRGB());
                }
            }
            ImageIO.write(background, "png", backgroundFile);
            ImageIO.write(overlay, "png", overlayFile);

            ImageMergeUtilVo first = image(backgroundFile, 0, 0);
            ImageMergeUtilVo second = image(overlayFile, 3, 5);
            Method buildImage = ImageMergeUtil.class.getDeclaredMethod("buildImage", java.util.List.class);
            buildImage.setAccessible(true);
            BufferedImage merged = (BufferedImage) buildImage.invoke(null, Arrays.asList(first, second));

            assertEquals(Color.RED.getRGB(), merged.getRGB(3, 5));
            assertEquals(Color.WHITE.getRGB(), merged.getRGB(3, 3));
            assertTrue(backgroundFile.exists());
            assertTrue(overlayFile.exists());
        } finally {
            backgroundFile.delete();
            overlayFile.delete();
        }
    }

    private ImageMergeUtilVo image(File file, int x, int y) {
        ImageMergeUtilVo value = new ImageMergeUtilVo();
        value.setPath(file.getAbsolutePath());
        value.setX(x);
        value.setY(y);
        return value;
    }
}
