package com.faforever.client;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.update4j.Configuration;
import org.update4j.Configuration.Builder;
import org.update4j.FileMetadata;
import org.update4j.OS;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Update4jConfigTask extends DefaultTask {

  @Input
  Path update4jXml = getProject().getBuildDir().toPath().resolve("update4j/update4j.xml");

  @Input
  Path targetDir = getProject().getBuildDir().toPath().resolve("update4j");

  @Input
  Path sourceDir;

  @Input
  String baseUrl;

  public Update4jConfigTask() {
    setGroup("update4j");
  }

  @TaskAction
  public void build() throws Exception {
    Files.createDirectories(targetDir);

    String version = String.valueOf(getProject().getVersion());
    Builder builder = Configuration.builder()
        .basePath("${app.dir}")
        .baseUri(baseUrl)
        .updateHandler("com.faforever.client.updater.${update.handler.class}")
        .property("default.launcher.main.class", "com.faforever.client.Main")
        .property("default.launcher.system.externalToolsDir", "${app.dir}/tools")
        // Must match with install4j configuration
        .property("app.name", "FAF Client")
        .property("app.dir", "${LOCALAPPDATA}/${app.name}", OS.WINDOWS)
        .property("app.dir", "${user.dir}/${app.name}")
        .property("version", version)
        .property("update.handler.class", "DefaultUpdateHandler");

    builder.files(FileMetadata.streamDirectory(sourceDir)
        .peek(reference -> reference.osFromFilename().ignoreBootConflict()));

    Configuration configuration = builder.build();
    try (BufferedWriter writer = Files.newBufferedWriter(update4jXml, StandardCharsets.UTF_8)) {
      configuration.write(writer);
    }
  }
}
