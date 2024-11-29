package com.gaotu.plugin.action;

import com.gaotu.plugin.Dialog.SimilarImagesDialog;
import com.gaotu.plugin.utils.ImageUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_features2d.BFMatcher;
import org.bytedeco.opencv.opencv_features2d.SIFT;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FindSimilarPictureAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        String basePath = project.getBasePath();
        if (basePath == null) {
            Messages.showMessageDialog(project, "Cannot determine the project base path.", "Error", Messages.getErrorIcon());
            return;
        }

        List<File> imageFiles = findAllImageFiles(new File(basePath));
        if (imageFiles.isEmpty()) {
            Messages.showMessageDialog(project, "No image files found in the project directory.", "Info", Messages.getInformationIcon());
            return;
        }

        // 使用SwingWorker异步处理图像比较
        new ImageComparisonTask(imageFiles, project).execute();
    }

    private List<File> findAllImageFiles(File baseDir) {
        List<File> imageFiles = new ArrayList<>();
        walk(baseDir, imageFiles);
        return imageFiles;
    }

    private void walk(File dir, List<File> imageFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    walk(file, imageFiles);
                } else {
                    if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) {
                        imageFiles.add(file);
                    }
                }
            }
        }
    }

    private static class ImageComparisonTask extends SwingWorker<List<List<File>>, Void> {
        private final List<File> imageFiles;
        private final Project project;

        public ImageComparisonTask(List<File> imageFiles, Project project) {
            this.imageFiles = imageFiles;
            this.project = project;
        }

        @Override
        protected List<List<File>> doInBackground() throws Exception {
            List<List<File>> similarImageGroups = new ArrayList<>();
            SIFT sift = SIFT.create();
            List<Mat> descriptorsList = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            List<Mat> images = new ArrayList<>();
            List<File> matToFileList = new ArrayList<>(); // 新增：保存每个Mat对应的源文件

            // 提取所有图像的特征点与描述符
            for (File file : imageFiles) {
                Mat image = opencv_imgcodecs.imread(file.getAbsolutePath(), opencv_imgcodecs.IMREAD_COLOR);
                images.add(image);
                fileNames.add(file.getName());
                matToFileList.add(file); // 新增：记录Mat对应的文件
                if (!image.empty()) {
                    Mat grayImage = new Mat();
                    opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);

                    Mat descriptors = new Mat();
                    KeyPointVector keyPoints = new KeyPointVector();
                    sift.detectAndCompute(grayImage, new Mat(), keyPoints, descriptors);

                    descriptorsList.add(descriptors);
                    System.out.println("Extracted " + keyPoints.size() + " keypoints from " + file.getName());
                } else {
                    descriptorsList.add(new Mat());
                    System.out.println("Failed to read image: " + file.getName());
                }
            }

            // 打印每个图像的特征点数量
            for (int idx = 0; idx < fileNames.size(); idx++) {
                System.out.println("Image: " + fileNames.get(idx) + ", Keypoints: " + descriptorsList.get(idx).size());
            }

            // 初始化 BFMatcher
            BFMatcher bfMatcher = new BFMatcher(opencv_core.NORM_L2, false);

            // 比较每个图像的特征描述符
            for (int i = 0; i < descriptorsList.size(); i++) {
                List<File> group = new ArrayList<>();
                group.add(imageFiles.get(i));

                for (int j = i + 1; j < descriptorsList.size(); j++) {
                    Mat descriptors1 = descriptorsList.get(i);
                    Mat descriptors2 = descriptorsList.get(j);

                    boolean isSimilar = false;
                    double avgDistance = Double.MAX_VALUE;

                    if (descriptors1.empty() && descriptors2.empty()) {
                        ImageUtils imageUtils = new ImageUtils();

                        File file1 = matToFileList.get(i); // 获取对应的源文件
                        File file2 = matToFileList.get(j); // 获取对应的源文件

                        if (imageUtils.computeImageHash(file1).equals(imageUtils.computeImageHash(file2))) {
                            isSimilar = true;
                        }
                    } else {
                        DMatchVector goodMatches = new DMatchVector();
                        int goodMatchesCount = 0;
                        if (!descriptors1.empty() && !descriptors2.empty()) {
                            // 使用 KNN 双向匹配
                            DMatchVectorVector knnMatches = new DMatchVectorVector();
                            bfMatcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);

                            // 比率测试
                            double ratioThresh = 0.75;

                            for (long k = 0; k < knnMatches.size(); k++) {
                                if (knnMatches.get(k).size() >= 2) {
                                    DMatch bestMatch = knnMatches.get(k).get(0);
                                    DMatch secondBestMatch = knnMatches.get(k).get(1);

                                    if (bestMatch.distance() < ratioThresh * secondBestMatch.distance()) {
                                        goodMatches.push_back(bestMatch);
                                    }
                                }
                            }

                            goodMatchesCount = (int) goodMatches.size();

                            // 计算平均距离
                            if (goodMatchesCount > 0) {
                                double totalDistance = 0;
                                for (long k = 0; k < goodMatches.size(); k++) {
                                    totalDistance += goodMatches.get(k).distance();
                                }

                                avgDistance = totalDistance / goodMatches.size();

                                // 根据距离和匹配数量阈值判定为相似图片
                                if (avgDistance < 50) { // 阈值更加严格
                                    isSimilar = true;
                                }
                            }
                        }
                    }

                    if (isSimilar) {
                        group.add(imageFiles.get(j));
                        // 使用标记矩阵来标记图片已被处理，可以避免重复计算
                        descriptorsList.set(j, new Mat());
                        System.out.println("  -> Images are similar.");
                    } else {
                        System.out.println("  -> Images are not similar.");
                    }
                }

                if (group.size() > 1) {
                    similarImageGroups.add(group);
                }
            }

            return similarImageGroups;
        }

        @Override
        protected void done() {
            try {
                List<List<File>> similarImageGroups = get();
                displayResults(similarImageGroups, project);
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }

        private void displayResults(List<List<File>> similarImageGroups, Project project) {
            if (similarImageGroups.isEmpty()) {
                Messages.showMessageDialog(project, "No similar images found.", "Similar Images Found", Messages.getInformationIcon());
            } else {
                new SimilarImagesDialog(similarImageGroups, project, " Similar").show();
            }
        }
    }
}
