package com.zbkj.common.utils;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.vo.ImageMergeUtilVo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 * 图片工具类
 */
@Component
public class ImageMergeUtil {
    private static final String MODEL = "merge";
    private static final String EXT = "jpg";
    private static final int MAX_REMOTE_IMAGE_BYTES = 10 * 1024 * 1024;

    /**
     * 合并生成新的图片文件
     * @param list List<ImageMergeUtilVo> 图片集合
     * @author Mr.Zhang
     * @since 2020-05-06
     */
    public static String drawWordFile(List<ImageMergeUtilVo> list){
        BufferedImage mergedImage = buildImage(list);
        try {
            String newFileName = UploadUtil.fileName(EXT);
            String directory = UploadUtil.getRootPath() + MODEL + "/"
                    + CrmebDateUtil.nowDate("yyyy/MM/dd") + "/";
            String destPath = FilenameUtils.separatorsToSystem(directory + newFileName);
            File file = UploadUtil.createFile(destPath);
            if (!ImageIO.write(mergedImage, EXT, file)) {
                throw new CrmebException("不支持的图片输出格式");
            }
            return destPath;
        } catch (IOException ex) {
            throw new CrmebException("合成图片写入失败");
        }
    }

    /**
     * 合并生成新的图片流
     * @param list List<ImageMergeUtilVo> 图片集合
     * @author Mr.Zhang
     * @since 2020-05-06
     */
    private static BufferedImage buildImage(List<ImageMergeUtilVo> list){
        if (list == null || list.size() < 2) {
            throw new CrmebException("至少需要2张图片才可以做合并");
        }
        BufferedImage canvas = null;
        for (int i = 0; i < list.size(); i++) {
            ImageMergeUtilVo source = list.get(i);
            BufferedImage image = readImage(source.getPath());
            if (image == null) {
                throw new CrmebException("无法读取待合成图片");
            }
            if (canvas == null) {
                canvas = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_BGR);
            }
            Graphics2D graphics = canvas.createGraphics();
            try {
                int x = i == 0 ? 0 : source.getX();
                int y = i == 0 ? 0 : source.getY();
                graphics.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
            } finally {
                graphics.dispose();
            }
        }
        return canvas;
    }



    /**
     * 根据图片路径输出File流
     * @param url String 文件地址
     * @author Mr.Zhang
     * @since 2020-05-06
     */
    private static BufferedImage readImage(String path) {
        try {
            if (!path.startsWith("http://") && !path.startsWith("https://")) {
                return ImageIO.read(new File(path));
            }
            URLConnection connection = new URL(path).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            int contentLength = connection.getContentLength();
            if (contentLength > MAX_REMOTE_IMAGE_BYTES) {
                throw new CrmebException("远程图片不能超过10MB");
            }
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = readLimited(input);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("读取待合成图片失败");
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_REMOTE_IMAGE_BYTES) {
                    throw new CrmebException("远程图片不能超过10MB");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
