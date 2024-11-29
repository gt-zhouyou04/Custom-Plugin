# Animation Preview Plugin

## 环境要求

- **JDK**: 与Android Studio架构一致（测试时发现Android Studio为x86结构，JDK需与其一致，否则运行`runIde`时会报错）
- **Gradle**: 配置可能需要更改以下部分内容

### 使用方法：
./gradlew buildPlugin后，在./build/distribution下找到Animation Preview-1.1.zip
在IDE中点击Android Studio->setting->plugins->Installed右侧设置图标->Install plugin from disk；
添加该zip文件后即可使用。

### 目前功能：
- **1**:右键点击可预览的动画json文件，点击最下方Preview Animation选项，即可预览动画资源(待优化，目前不能预览assets不为空的json文件)；
- **2**:Refactor->Find Same Pictures:查找所有相同图片(待优化)
- **3**:.Refactor->Find Similar Pictures:查找所有相似的图片(待优化)

## 配置说明

在 `build.gradle` 中，请检查并修改以下内容：

```groovy
intellij {
    version.set("2023.2.5") // 设置成自己的IDE版本，后续可动态获取
    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin"))
    pluginName.set("Animation Preview")
}

// 修改为自己的JDK版本
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

// 检查Android Studio路径
tasks.runIde {
    ideDir.set(file("/Applications/Android Studio.app/Contents"))
}

// 检查IDE版本号
patchPluginXml {
    sinceBuild.set("231")
    untilBuild.set("242.*")
}
