package com.upc.face.utils;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
public class FaceUtil {

    private static CascadeClassifier faceDetector;

    static {
        try {
            // 加载OpenCV本地库
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            // 加载级联分类器
            String cascadePath = extractCascadeFile();
            faceDetector = new CascadeClassifier(cascadePath);
        } catch (Exception e) {
            log.error("初始化人脸识别失败", e);
        }
    }

    /**
     * 提取级联分类器文件
     */
    private static String extractCascadeFile() throws Exception {
        String cascadeFileName = "haarcascade_frontalface_default.xml";
        File tempFile = File.createTempFile("cascade", ".xml");

        try (InputStream is = FaceUtil.class.getClassLoader().getResourceAsStream(cascadeFileName);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            if (is == null) {
                // 如果资源文件不存在，创建一个简单的模拟实现
                log.warn("级联分类器文件不存在，使用模拟实现");
                return null;
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        tempFile.deleteOnExit();
        return tempFile.getAbsolutePath();
    }

    /**
     * 检测人脸
     */
    public static boolean detectFace(byte[] imageBytes) {
        try {
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                return false;
            }

            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayImage, grayImage);

            MatOfRect faces = new MatOfRect();
            if (faceDetector != null) {
                faceDetector.detectMultiScale(grayImage, faces, 1.1, 3, 0, new Size(30, 30), new Size());
            }

            image.release();
            grayImage.release();

            return faces.toArray().length > 0;
        } catch (Exception e) {
            log.error("人脸检测失败", e);
            return false;
        }
    }

    /**
     * 提取人脸特征（简化实现，实际应使用深度学习模型）
     */
    public static String extractFeature(byte[] imageBytes) {
        try {
            // 简化实现：对图像进行哈希处理作为特征
            // 实际项目中应使用FaceNet、ArcFace等深度学习模型
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_GRAYSCALE);
            if (image.empty()) {
                return null;
            }

            // 调整大小为固定尺寸
            Mat resized = new Mat();
            Imgproc.resize(image, resized, new Size(64, 64));

            // 计算简单的特征哈希
            StringBuilder feature = new StringBuilder();
            for (int i = 0; i < resized.rows(); i += 4) {
                for (int j = 0; j < resized.cols(); j += 4) {
                    double[] pixel = resized.get(i, j);
                    feature.append(String.format("%02x", (int) pixel[0]));
                }
            }

            image.release();
            resized.release();

            return Base64.getEncoder().encodeToString(feature.toString().getBytes());
        } catch (Exception e) {
            log.error("特征提取失败", e);
            return null;
        }
    }

    /**
     * 比对两张人脸图片
     */
    public static double compareFace(byte[] imageBytes1, byte[] imageBytes2) {
        String feature1 = extractFeature(imageBytes1);
        String feature2 = extractFeature(imageBytes2);

        if (feature1 == null || feature2 == null) {
            return 0.0;
        }

        return compareFeature(feature1, feature2);
    }

    /**
     * 比对两个人脸特征
     */
    public static double compareFeature(String feature1, String feature2) {
        try {
            // 解码特征
            String decoded1 = new String(Base64.getDecoder().decode(feature1));
            String decoded2 = new String(Base64.getDecoder().decode(feature2));

            // 计算汉明距离
            int distance = 0;
            int length = Math.min(decoded1.length(), decoded2.length());

            for (int i = 0; i < length; i++) {
                if (decoded1.charAt(i) != decoded2.charAt(i)) {
                    distance++;
                }
            }

            // 转换为相似度 (0-1)
            double similarity = 1.0 - (double) distance / length;
            return Math.max(0.0, similarity);
        } catch (Exception e) {
            log.error("特征比对失败", e);
            return 0.0;
        }
    }
}
