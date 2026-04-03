// IDEA 离线翻译插件 - Gradle 构建配置 (Windows 本地编译版)
plugins {
    idea
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.idea.tran"
version = "1.0.0"

repositories {
    mavenCentral()
}

// 依赖配置
dependencies {
    // Lombok (IDEA 内置)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // ONNX Runtime Java
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.1")
}

// IDEA SDK 配置
intellij {
    version.set("2024.2")
    type.set("IC")
    downloadSources.set(true)
}

// 打包配置
tasks.patchPluginXml {
    sinceBuild = "242"
}

// 修复: gradle-intellij-plugin 1.x 不会自动打包 implementation 依赖
// 使用 jar 保存所有依赖到一个文件中
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    // 包含 resources 目录下的所有文件（包括模型）
    from("src/main/resources") {
        into("") // 把 resources 下的内容放到根目录
    }
}

tasks {
    patchPluginXml {
        changeNotes = """
            **What's New**
            - Support offline translation with mBART-large-cnn model
            - Shortcuts: Alt+Shift+T (configurable in Settings)
            **Features**: Real-time text translation for code selection
            **Model Info**: ONNX Runtime Java 1.18.0 + mBART-large-cnn
            **Version**: 1.0.0
        """.trimIndent()
    }

    buildPlugin {
    }

    build {
        dependsOn(patchPluginXml, buildPlugin, compileJava, processResources)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadJavadoc = false
        isDownloadSources = false
    }
}
