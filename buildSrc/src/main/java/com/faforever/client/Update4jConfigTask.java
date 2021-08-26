package com.faforever.client;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
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

  @OutputFile
  Path update4jXml = getProject().getBuildDir().toPath().resolve("update4j/update4j.xml");

  @InputDirectory
  Path sourceDir;

  @Input
  String baseUrl;

  public void setUpdate4jXml(Path update4jXml) {
    this.update4jXml = update4jXml;
  }

  public void setSourceDir(Path sourceDir) {
    this.sourceDir = sourceDir;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Path getUpdate4jXml() {
    return update4jXml;
  }

  public Path getSourceDir() {
    return sourceDir;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Update4jConfigTask() {
    setGroup("update4j");
  }

  @TaskAction
  public void build() throws Exception {
    Files.createDirectories(update4jXml.getParent());

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

    System.out.println("Writing " + update4jXml);
    Configuration configuration = builder.build();
    try (BufferedWriter writer = Files.newBufferedWriter(update4jXml, StandardCharsets.UTF_8)) {
      configuration.write(writer);
    }
  }
}
