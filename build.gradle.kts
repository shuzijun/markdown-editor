import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("de.undercouch.download") version "4.0.2"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = System.getenv("LD_VERSION") ?: providers.gradleProperty("pluginVersion").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/google")
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    api("io.sentry:sentry:1.7.9") {
        exclude(module = "slf4j-api")
    }
    implementation("com.alibaba:fastjson:1.2.69")
    api("org.apache.commons:commons-lang3:3.9")
    implementation("commons-lang:commons-lang:2.6")

    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins("com.intellij.modules.jcef")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = project.version.toString()

        val changelog = project.changelog
        changeNotes = with(changelog) {
            renderItem(
                (getOrNull(project.version.toString()) ?: getUnreleased())
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    buildSearchableOptions = false

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks.named("runIde") {
    (this as JavaForkOptions).jvmArgs = providers.gradleProperty("runIdeJvmArgs").map { it.split(",").toList() }.get()
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named("publishPlugin") {
    dependsOn("patchChangelog")
}
