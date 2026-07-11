package com.zbkj.common.utils;

import com.alibaba.druid.util.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.security.SecureRandom;

/**
 * 验证码生成工具类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
public class ValidateCodeUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RAND_STRING = "0123456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final int WIDTH = 80;
    private static final int HEIGHT = 34;
    private static final int STRING_NUM = 4;
    private static final int LINE_SIZE = 40;

    /**
     * 将构造函数私有化 禁止new创建
     * @author Mr.Zhang
     * @since 2020-04-16
     */
    private ValidateCodeUtil() {
        super();
    }

    /**
     * 获取随机字符
     * @author Mr.Zhang
     * @since 2020-04-16
     * @return String
     */
    private static String getRandomChar(int index) {
        //获取指定位置index的字符，并转换成字符串表示形式
        return String.valueOf(RAND_STRING.charAt(index));
    }

    /**
     * 获取随机指定区间的随机数
     * @param min (指定最小数)
     * @param max (指定最大数)
     * @author Mr.Zhang
     * @since 2020-04-16
     * @return String
     */
    private static int getRandomNum(int min,int max) {
        return RANDOM.nextInt(max - min) + min;
    }

    /**
     * 获得字体
     */
    private static Font getFont() {
        return new Font("Fixedsys", Font.CENTER_BASELINE, 25);  //名称、样式、磅值
    }

    /**
     * 获得颜色
     * @param frontColor 覆盖颜色
     * @param backColor 背景色
     * @author Mr.Zhang
     * @since 2020-04-16
     * @return Color
     */
    private static Color getRandColor(int frontColor, int backColor) {
        if(frontColor > 255)
            frontColor = 255;
        if(backColor > 255)
            backColor = 255;

        int red = frontColor + RANDOM.nextInt(backColor - frontColor - 16);
        int green = frontColor + RANDOM.nextInt(backColor - frontColor -14);
        int blue = frontColor + RANDOM.nextInt(backColor - frontColor -18);
        return new Color(red, green, blue);
    }

    /**
     * 绘制字符串,返回绘制的字符串
     * @param graphics 获得BufferedImage对象的Graphics对象
     * @param randomString 随机字符串
     * @param i 坐标倍数
     * @author Mr.Zhang
     * @since 2020-04-16
     * @return string
     */
    private static String drawString(Graphics graphics, String randomString, int i) {
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setFont(getFont());   //设置字体
        g2d.setColor(new Color(RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()));//设置颜色
        String randChar = getRandomChar(RANDOM.nextInt(RAND_STRING.length()));
        randomString += randChar;   //组装
        int rot = getRandomNum(1,10);
        g2d.translate(RANDOM.nextInt(3), RANDOM.nextInt(3));
        g2d.rotate(rot * Math.PI / 180);
        g2d.drawString(randChar, 13*i, 20);
        g2d.rotate(-rot * Math.PI / 180);
        return randomString;
    }

    /**
     * 绘制干扰线
     * @param graphics 获得BufferedImage对象的Graphics对象
     * @author Mr.Zhang
     * @since 2020-04-16
     */
    private static void drawLine(Graphics graphics) {
        //起点(x,y)  偏移量x1、y1
        int x = RANDOM.nextInt(WIDTH);
        int y = RANDOM.nextInt(HEIGHT);
        int xl = RANDOM.nextInt(13);
        int yl = RANDOM.nextInt(15);
        graphics.setColor(new Color(RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()));
        graphics.drawLine(x, y, x + xl, y + yl);
    }

    /**
     * 生成Base64图片验证码
     * @author Mr.Zhang
     * @since 2020-04-16
     * @return String
     */
    public static Validate getRandomCode() {
        Validate result = new Validate();

        // BufferedImage类是具有缓冲区的Image类,Image类是用于描述图像信息的类
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_BGR);
        Graphics graphics = image.getGraphics();// 获得BufferedImage对象的Graphics对象
        graphics.fillRect(0, 0, WIDTH, HEIGHT);//填充矩形
        graphics.setFont(new Font("Times New Roman", Font.ROMAN_BASELINE, 18));//设置字体
        graphics.setColor(getRandColor(110, 133));//设置颜色
        //绘制干扰线
        for(int i = 0; i <= LINE_SIZE; i++) {
            drawLine(graphics);
        }
        //绘制字符
        String randomString = "";
        for(int i = 1; i <= STRING_NUM; i++) {
            randomString = drawString(graphics, randomString, i);
            result.setValue(randomString);
        }

        graphics.dispose();//释放绘图资源
        try (ByteArrayOutputStream bs = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bs);//将绘制得图片输出到流
            String imgSrc = Base64.byteArrayToBase64(bs.toByteArray());
            result.setBase64Str(imgSrc);
        } catch (Exception e) {
            throw new IllegalStateException("生成图片验证码失败", e);
        }
        return result;
    }

    /**
     * 验证码类
     * @author Mr.Zhang
     * @since 2020-04-16
     */
    public static class Validate implements Serializable{
        private static final long serialVersionUID = 1L;
        private String Base64Str;		//Base64 值
        private String value;			//验证码值

        public String getBase64Str() {
            return Base64Str;
        }
        public void setBase64Str(String base64Str) {
            Base64Str = base64Str;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }

}
