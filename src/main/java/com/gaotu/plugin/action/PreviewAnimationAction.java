package com.gaotu.plugin.action;

import com.gaotu.plugin.Dialog.JsonAnimationPreviewDialog;
import com.gaotu.plugin.utils.JsonUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class PreviewAnimationAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null && file.getName().endsWith(".json")) {
            // 打开预览窗口
            JsonAnimationPreviewDialog.show(file);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isEnabled = false;

        // 判断是否选中了JSON文件，并且文件内容是有效的 Lottie 动画
        if (file != null && file.getExtension() != null && file.getExtension().equals("json")) {
            isEnabled = JsonUtils.isLottieAnimation(file);
        }

        event.getPresentation().setEnabledAndVisible(isEnabled);
    }
}
