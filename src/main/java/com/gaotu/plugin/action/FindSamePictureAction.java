package com.gaotu.plugin.action;

import com.gaotu.plugin.Dialog.SimilarImagesDialog;
import com.gaotu.plugin.utils.ImageUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FindSamePictureAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        String basePath = project.getBasePath();
        if (basePath == null) {
            Messages.showMessageDialog(project, "Cannot determine the project base path.", "Error", Messages.getErrorIcon());
            return;
        } else {
            System.out.println("Base path: " + basePath);
        }

        List<File> imageFiles = findAllImageFiles(new File(basePath));
        if (imageFiles.isEmpty()) {
            Messages.showMessageDialog(project, "No image files found in the project directory.", "Info", Messages.getInformationIcon());
            return;
        } else {
            System.out.println("Found " + imageFiles.size() + " image files.");
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

    private class ImageComparisonTask extends SwingWorker<List<List<File>>, Void> {
        private List<File> imageFiles;
        private Project project;

        public ImageComparisonTask(List<File> imageFiles, Project project) {
            this.imageFiles = imageFiles;
            this.project = project;
        }

        @Override
        protected List<List<File>> doInBackground() {
            ImageUtils imageUtils = new ImageUtils();
            List<List<File>> similarImageGroups = new ArrayList<>();
            Map<String, List<File>> hashFileMap = new HashMap<>();

            for (File file : imageFiles) {
                try {
                    String imageHash = imageUtils.computeImageHash(file); // 计算图像哈希
                    hashFileMap.computeIfAbsent(imageHash, k -> new ArrayList<>()).add(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            for (Map.Entry<String, List<File>> entry : hashFileMap.entrySet()) {
                List<File> group = entry.getValue();
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
                Messages.showMessageDialog(project, "No Same images found.", "Same Images Found", Messages.getInformationIcon());
            } else {
                new SimilarImagesDialog(similarImageGroups, project, "Same").show();
            }
        }
    }
}
