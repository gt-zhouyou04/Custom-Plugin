package com.gaotu.plugin.Dialog;

import com.gaotu.plugin.utils.JsonUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class JsonAnimationPreviewDialog extends DialogWrapper {

    private final VirtualFile file;
    private static JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private JPanel animationPanel;
    private static JsonAnimationPreviewDialog dialog; // 静态变量存储对话框实例

    // 控制属性组件
    private JSlider speedSlider;
    private JSlider scaleSlider;
    private JToggleButton directionToggleButton;
    private JCheckBox loopCheckBox;
    private JTextField speedTextField;
    private JTextField scaleTextField;
    private JTextField startFrameTextField;
    private JTextField endFrameTextField;
    private JButton pauseButton;
    private final int totalFrames;

    protected JsonAnimationPreviewDialog(@Nullable VirtualFile file) {
        super(true); // 使用当前窗口作为父窗口
        this.file = file;

        // 获取总帧数
        assert file != null;
        this.totalFrames = JsonUtils.getTotalFrames(file);

        setTitle("JSON Animation Preview");
        setSize(1000, 800);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        saveAllFiles(); // 打开弹窗前先执行file->save all操作，否则由于IDEA的文件缓存机制，若修改json文件内容后，再次打开该文件显示仍为修改前的动画
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建控制面板
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel speedLabel = new JLabel("Speed:");
        controlsPanel.add(speedLabel, gbc);

        gbc.gridx = 1;
        speedSlider = new JSlider(50, 200, 100); // 0.5x 到 2.0x 速度
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(e -> syncTextFieldWithSlider(speedSlider, speedTextField));
        controlsPanel.add(speedSlider, gbc);

        gbc.gridx = 2;
        speedTextField = new JTextField("1.0", 5);
        speedTextField.setHorizontalAlignment(JTextField.RIGHT);
        speedTextField.addActionListener(e -> syncSliderWithTextField(speedTextField, speedSlider));
        controlsPanel.add(speedTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel directionLabel = new JLabel("Direction:");
        controlsPanel.add(directionLabel, gbc);

        gbc.gridx = 1;
        directionToggleButton = new JToggleButton("Normal");
        directionToggleButton.addActionListener(e -> updateDirection());
        controlsPanel.add(directionToggleButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel loopLabel = new JLabel("Loop:");
        controlsPanel.add(loopLabel, gbc);

        gbc.gridx = 1;
        loopCheckBox = new JCheckBox();
        loopCheckBox.setSelected(true); // 默认选中循环播放
        loopCheckBox.addActionListener(e -> updateAnimationProperties());
        controlsPanel.add(loopCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel scaleLabel = new JLabel("Scale:");
        controlsPanel.add(scaleLabel, gbc);

        gbc.gridx = 1;
        scaleSlider = new JSlider(50, 200, 100); // 50% 到 200% 缩放
        scaleSlider.setMajorTickSpacing(50);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        scaleSlider.addChangeListener(e -> syncTextFieldWithSlider(scaleSlider, scaleTextField));
        controlsPanel.add(scaleSlider, gbc);

        gbc.gridx = 2;
        scaleTextField = new JTextField("1.0", 5);
        scaleTextField.setHorizontalAlignment(JTextField.RIGHT);
        scaleTextField.addActionListener(e -> syncSliderWithTextField(scaleTextField, scaleSlider));
        controlsPanel.add(scaleTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel startFrameLabel = new JLabel("Start Frame:");
        controlsPanel.add(startFrameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        startFrameTextField = new JTextField("0", 10);
        controlsPanel.add(startFrameTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel endFrameLabel = new JLabel("End Frame:");
        controlsPanel.add(endFrameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        endFrameTextField = new JTextField(String.valueOf(totalFrames), 10);
        controlsPanel.add(endFrameTextField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.gridheight = 2;
        JButton applySegmentsButton = new JButton("Apply Segments");
        applySegmentsButton.setPreferredSize(new Dimension(150, 40));
        applySegmentsButton.addActionListener(e -> {
            playSegments(); // 应用播放段
        });
        controlsPanel.add(applySegmentsButton, gbc);
        gbc.gridheight = 1;

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> togglePause());
        controlsPanel.add(pauseButton, gbc);

        gbc.gridx = 1;
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> stopAnimation());
        controlsPanel.add(stopButton, gbc);

        mainPanel.add(controlsPanel, BorderLayout.SOUTH);

        // 创建动画展示面板
        animationPanel = new JPanel(new BorderLayout());
        mainPanel.add(animationPanel, BorderLayout.CENTER);

        // 初始化动画面板
        initializeAnimationPanel();

        return mainPanel;
    }

    private void syncTextFieldWithSlider(JSlider slider, JTextField textField) {
        double value = slider.getValue() / 100.0;
        textField.setText(String.valueOf(value));
        updateAnimationProperties();
    }

    private void syncSliderWithTextField(JTextField textField, JSlider slider) {
        try {
            double value = Double.parseDouble(textField.getText());
            value = Math.max(0.5, Math.min(2.0, value)); // 限制范围在0.5到2之间
            int intValue = (int) (value * 100.0);
            slider.setValue(intValue);
            updateAnimationProperties();
        } catch (NumberFormatException e) {
            // Handle invalid number format
        }
    }

    private void initializeAnimationPanel() {
        System.out.println("Initialize animation panel");
        clearAnimationPanel();
        jfxPanel = createJFXPanel(); // 必须在 EDT 上创建
        animationPanel.add(jfxPanel, BorderLayout.CENTER);
        animationPanel.revalidate();
        animationPanel.repaint();

        Platform.runLater(this::initializeWebView);
    }

    private JFXPanel createJFXPanel() {
        JFXPanel panel = new JFXPanel(); // JavaFX Panel 必须在事件调度线程上创建
        // 确保 JavaFX 平台已启动
        Platform.setImplicitExit(false);
        return panel;
    }

    private void initializeWebView() {
        System.out.println("Initialize WebView");
        if (webView != null) {
            webView = null; // 确保 WebView 重新初始化
        }
        webView = new WebView();
        webEngine = webView.getEngine();
        jfxPanel.setScene(new Scene(webView));
        updateAnimationPanel();
    }

    private void clearAnimationPanel() {
        if (animationPanel != null) {
            animationPanel.removeAll();
            animationPanel.revalidate();
            animationPanel.repaint();
        }
    }

    private void updateAnimationPanel() {
        System.out.println("Updating Animation Panel");
        Platform.runLater(() -> {
            System.out.println("Inside Platform.runLater");

            if (webEngine == null) {
                System.out.println("WebEngine is null");
                return;
            }

            String jsonContent;
            try {
                jsonContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return; // 如果读取文件失败，返回
            }

            // 创建 HTML 内容以包含 Lottie 动画，并包含时间戳以避免缓存
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"utf-8\">\n" +
                    "    <title>JSON Animation Preview</title>\n" +
                    "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.7.3/lottie.min.js\"></script>\n" +
                    "    <style>\n" +
                    "        body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; }\n" +
                    "        #animation { width: 100%; height: 100vh; background-color: #f0f0f0; display: flex; justify-content: center; align-items: center; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div id=\"animation\"></div>\n" +
                    "    <script>\n" +
                    "        var animation = null;\n" +
                    "        function loadAnimation() {\n" +
                    "            try {\n" +
                    "                var animationData = " + jsonContent + ";\n" +
                    "                animation = lottie.loadAnimation({\n" +
                    "                    container: document.getElementById('animation'),\n" +
                    "                    renderer: 'svg',\n" +
                    "                    loop: true,\n" +
                    "                    autoplay: true,\n" +
                    "                    animationData: animationData\n" +
                    "                });\n" +
                    "                animation.addEventListener('data_failed', function() {\n" +
                    "                    document.getElementById('animation').style.display = 'none';\n" +
                    "                });\n" +
                    "            } catch (e) {\n" +
                    "                document.getElementById('animation').style.display = 'none';\n" +
                    "            }\n" +
                    "        }\n" +
                    "        loadAnimation();\n" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";

            // 清除之前的内容加载
            webEngine.getLoadWorker().cancel();

            // 加载新的内容并强制刷新
            webEngine.loadContent(htmlContent);

            // 更新动画属性
            updateAnimationProperties();
        });
    }

    private void updateAnimationProperties() {
        Platform.runLater(() -> {
            if (webEngine != null) {
                double speed = speedSlider.getValue() / 100.0;
                int direction = directionToggleButton.isSelected() ? -1 : 1;
                double scale = scaleSlider.getValue() / 100.0;
                boolean loop = loopCheckBox.isSelected();
                // 无需在此处更新播放段，播放段单独处理
                System.out.println("Applying properties:");
                System.out.println("Speed: " + speed);
                System.out.println("Direction: " + direction);
                System.out.println("Scale: " + scale);
                System.out.println("Loop: " + loop);

                String jsCode =
                        "if (animation) {" +
                                "    animation.setSpeed(" + speed + ");" +
                                "    animation.setDirection(" + direction + ");" +
                                "    document.getElementById('animation').style.transform = 'scale(' + " + scale + " + ')';" +
                                "    animation.loop = " + loop + ";" +
                                "}";
                System.out.println("Executing JavaScript: " + jsCode);
                webEngine.executeScript(jsCode);
            }
        });
    }

    private void updateDirection() {
        // 更新按钮文本
        if (directionToggleButton.isSelected()) {
            directionToggleButton.setText("Reverse");
        } else {
            directionToggleButton.setText("Normal");
        }
        // 更新动画方向
        updateAnimationProperties();
    }

    private void playSegments() {
        Platform.runLater(() -> {
            if (webEngine != null) {
                int startFrame = parseIntOrDefault(startFrameTextField.getText(), 0);
                int endFrame = parseIntOrDefault(endFrameTextField.getText(), totalFrames);

                String jsCode = "if (animation) {" +
                        "    animation.stop();" +
                        "    animation.playSegments([[" + startFrame + ", " + endFrame + "]], true);" +
                        "}";
                System.out.println("Executing JavaScript for playSegments: " + jsCode);
                webEngine.executeScript(jsCode);
            }
        });
    }

    private int parseIntOrDefault(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void togglePause() {
        Platform.runLater(() -> {
            if (webEngine != null) {
                String jsCode = "if (animation) { if (animation.isPaused) { animation.play(); } else { animation.pause(); } }";
                System.out.println("Executing JavaScript: " + jsCode);
                webEngine.executeScript(jsCode);
                pauseButton.setText(pauseButton.getText().equals("Pause") ? "Resume" : "Pause");
            }
        });
    }

    private void stopAnimation() {
        pauseButton.setText("Resume");
        Platform.runLater(() -> {
            if (webEngine != null) {
                String jsCode = "if (animation) { animation.stop(); }";
                System.out.println("Executing JavaScript: " + jsCode);
                webEngine.executeScript(jsCode);
            }
        });
    }

    private void generateCode() {
        String fileName = file.getName();
        float speed = speedSlider.getValue() / 100.0f;
        int direction = directionToggleButton.isSelected() ? -1 : 1;
        boolean loop = loopCheckBox.isSelected();
        float scale = scaleSlider.getValue() / 100.0f;
        int startFrame = parseIntOrDefault(startFrameTextField.getText(), 0);
        int endFrame = parseIntOrDefault(endFrameTextField.getText(), totalFrames);

        String code = JsonUtils.generateLottieCode(fileName, speed, direction, loop, scale, startFrame, endFrame);

        JTextArea textArea = new JTextArea(code);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);  // 设置为可编辑

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            StringSelection stringSelection = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            // 添加弹出小窗口提示
            JOptionPane.showMessageDialog(this.getContentPane(), "Code copied to clipboard!", "Copy Successful", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // 新增一个顶部面板
        topPanel.add(copyButton);  // 将copy按钮添加到顶部面板
        panel.add(topPanel, BorderLayout.NORTH); // 将顶部面板添加到总面板的北部（顶部）
        panel.add(scrollPane, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this.getContentPane(), panel, "Generated Code", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        myCancelAction.putValue(Action.NAME, "Close");
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void init() {
        super.init();
        getOKAction().putValue(Action.NAME, "Generate Code");
        getOKAction().putValue(Action.SHORT_DESCRIPTION, "Generate code for the animation");
    }

    @Override
    protected void doOKAction() {
        generateCode();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        dispose();
    }

    private void saveAllFiles() {
        ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
    }

    @Override
    protected void dispose() {
        System.out.println("Disposing dialog");
        super.dispose();
        // 销毁 JavaFX 组件
        Platform.runLater(() -> {
            if (webEngine != null) {
                webEngine.load(null); // 清空 WebEngine 内容
                webEngine = null;
            }
            if (webView != null) {
                webView = null;
            }
            if (jfxPanel != null) {
                jfxPanel.setScene(null);
                jfxPanel = null;
            }
        });
    }

    public static void show(VirtualFile file) {
        // 如果已经有一个对话框实例存在，先关闭它
        if (dialog != null && dialog.isShowing()) {
            dialog.dispose();
            dialog = null;
        }
        dialog = new JsonAnimationPreviewDialog(file);
        dialog.show();
    }
}
