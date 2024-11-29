package com.gaotu.plugin.Dialog;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class SimilarImagesDialog extends DialogWrapper {
    private static final int IMAGES_PER_PAGE = 50;
    private List<List<File>> similarImageGroups;
    private Project project;
    private JPanel mainPanel;
    private JPanel groupsPanel;
    private int currentPage;

    public SimilarImagesDialog(List<List<File>> similarImageGroups, Project project, String type) {
        super(project);
        this.similarImageGroups = similarImageGroups;
        this.project = project;
        this.currentPage = 0;
        setTitle(type + " Images Found");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        mainPanel = new JPanel(new BorderLayout());

        groupsPanel = new JPanel();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        updateGroupsPanel();

        JScrollPane scrollPane = new JScrollPane(groupsPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createNavigationPanel(), BorderLayout.SOUTH);

        return mainPanel;
    }

    private void updateGroupsPanel() {
        groupsPanel.removeAll();

        int startIndex = currentPage * IMAGES_PER_PAGE;
        int endIndex = Math.min(startIndex + IMAGES_PER_PAGE, similarImageGroups.size());

        for (int i = startIndex; i < endIndex; i++) {
            List<File> group = similarImageGroups.get(i);

            JPanel groupPanel = new JPanel(new BorderLayout());
            groupPanel.setBorder(BorderFactory.createTitledBorder("Group " + (i + 1)));
            groupPanel.setBackground(new Color(220, 220, 220));

            JPanel imagePanel = new JPanel(new GridLayout(0, 1));
            for (File file : group) {
                JPanel filePanel = new JPanel(new BorderLayout());

                // File path label with clickable link
                JLabel pathLabel = new JLabel("<html><a href=''>" + file.getAbsolutePath().replace(project.getBasePath(), "") + "</a></html>");
                pathLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                pathLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
                        if (virtualFile != null) {
                            FileEditorManager.getInstance(project).openFile(virtualFile, true);
                        }
                    }
                });

                // Image preview
                JLabel imageLabel = new JLabel();
                ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());
                Image image = imageIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(image));

                filePanel.add(pathLabel, BorderLayout.CENTER);
                filePanel.add(imageLabel, BorderLayout.WEST);

                imagePanel.add(filePanel);
            }

            groupPanel.add(imagePanel, BorderLayout.CENTER);
            groupsPanel.add(groupPanel);
        }

        groupsPanel.revalidate();
        groupsPanel.repaint();
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel(new FlowLayout());

        int totalPages = (int) Math.ceil((double) similarImageGroups.size() / IMAGES_PER_PAGE);

        JButton prevButton = new JButton("Previous");
        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateGroupsPanel();
                updateNavigationPanel(totalPages);
            }
        });

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> {
            if ((currentPage + 1) * IMAGES_PER_PAGE < similarImageGroups.size()) {
                currentPage++;
                updateGroupsPanel();
                updateNavigationPanel(totalPages);
            }
        });

        JLabel pageLabel = new JLabel("Page: " + (currentPage + 1) + " / " + totalPages);

        JTextField pageNumberField = new JTextField(3);
        pageNumberField.setText(String.valueOf(currentPage + 1));
        JButton goButton = new JButton("Go");
        goButton.addActionListener(e -> {
            int pageNumber;
            try {
                pageNumber = Integer.parseInt(pageNumberField.getText());
                if (pageNumber > 0 && pageNumber <= totalPages) {
                    currentPage = pageNumber - 1;
                    updateGroupsPanel();
                    updateNavigationPanel(totalPages);
                }
            } catch (NumberFormatException ignored) {
            }
        });

        navigationPanel.add(prevButton);
        navigationPanel.add(nextButton);
        navigationPanel.add(new JLabel("Page: "));
        navigationPanel.add(pageNumberField);
        navigationPanel.add(goButton);
        navigationPanel.add(pageLabel);

        return navigationPanel;
    }

    private void updateNavigationPanel(int totalPages) {
        JPanel navigationPanel = (JPanel) mainPanel.getComponent(1);
        JLabel pageLabel = (JLabel) navigationPanel.getComponent(5);
        pageLabel.setText("Page: " + (currentPage + 1) + " / " + totalPages);

        JTextField pageNumberField = (JTextField) navigationPanel.getComponent(3);
        pageNumberField.setText(String.valueOf(currentPage + 1));
    }
}