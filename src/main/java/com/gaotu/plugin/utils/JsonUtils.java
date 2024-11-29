package com.gaotu.plugin.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonUtils {

    public static boolean isLottieAnimation(@NotNull VirtualFile file) {
        try {
            String jsonContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            return jsonObject.has("v") && jsonObject.has("fr") && jsonObject.has("ip") && jsonObject.has("op");
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }

    public static int getTotalFrames(@NotNull VirtualFile file) {
        try {
            String jsonContent = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            if (jsonObject.has("op") && jsonObject.has("ip")) {
                int endFrame = jsonObject.get("op").getAsInt();
                int startFrame = jsonObject.get("ip").getAsInt();
                return endFrame - startFrame;
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            e.printStackTrace();
        }
        return 100; // 默认值
    }

    public static String generateLottieCode(String fileName, float speed, int direction, boolean loop, float scale, int startFrame, int endFrame) {
        return  "        LottieAnimationView animationView = findViewById(R.id.animation_view);\n" +
                "        animationView.setAnimation(\"" + fileName + "\");\n" +
                "        animationView.setSpeed(" + speed + "f);\n" +
                "        animationView.setScale(" + scale + "f);\n" +
                "        animationView.setRepeatCount(" + (loop ? "LottieDrawable.INFINITE" : "0") + ");\n" +
                "        animationView.setRepeatMode(" + (direction == -1 ? "LottieDrawable.REVERSE" : "LottieDrawable.RESTART") + ");\n" +
                "        animationView.playAnimation(" + startFrame + ", " + endFrame + ");\n";
    }
}


