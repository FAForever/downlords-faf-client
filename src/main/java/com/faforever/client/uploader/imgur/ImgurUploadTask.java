package com.faforever.client.uploader.imgur;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.faforever.client.io.Bytes.formatSize;

public class ImgurUploadTask extends AbstractPrioritizedTask<String> {

  private final Gson gson;
  @Resource
  I18n i18n;
  @Resource
  Environment environment;
  private Image image;
  private int maxUploadSize;
  private String baseUrl;
  private String clientId;

  public ImgurUploadTask() {
    super(Priority.HIGH);
    gson = new GsonBuilder().create();
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("chat.imageUploadTask.title"));
    maxUploadSize = environment.getProperty("imgur.upload.maxSize", int.class);
    baseUrl = environment.getProperty("imgur.upload.baseUrl");
    clientId = environment.getProperty("imgur.upload.clientId");
  }

  @Override
  protected String call() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
    ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

    if (byteArrayOutputStream.size() > maxUploadSize) {
      throw new IllegalArgumentException("Image exceeds max upload size of " + formatSize(maxUploadSize, i18n.getLocale()));
    }

    String dataImage = BaseEncoding.base64().encode(byteArrayOutputStream.toByteArray());
    String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(dataImage, "UTF-8");

    URL url = new URL(baseUrl);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    urlConnection.setDoOutput(true);
    urlConnection.setDoInput(true);
    urlConnection.setRequestMethod("POST");
    urlConnection.setRequestProperty("Authorization", "Client-ID " + clientId);
    urlConnection.setRequestMethod("POST");
    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    urlConnection.connect();

    ResourceLocks.acquireUploadLock();
    try (OutputStream outputStream = urlConnection.getOutputStream()) {
      byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
      ByteCopier.from(new ByteArrayInputStream(bytes))
          .to(outputStream)
          .totalBytes(bytes.length)
          .listener(this::updateProgress)
          .copy();
    } finally {
      ResourceLocks.freeUploadLock();
    }

    StringBuilder stringBuilder = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }
    }

    ImgurRestResponse imgurRestResponse = gson.fromJson(stringBuilder.toString(), ImgurRestResponse.class);

    if (!imgurRestResponse.success) {
      throw new RuntimeException("Image upload failed, status code: " + imgurRestResponse.status);
    }

    return imgurRestResponse.data.link;
  }

  public void setImage(Image image) {
    this.image = image;
  }
}
