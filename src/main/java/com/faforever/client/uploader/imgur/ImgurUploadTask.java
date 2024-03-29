package com.faforever.client.uploader.imgur;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Imgur.Upload;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.commons.io.ByteCopier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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

import static com.faforever.commons.io.Bytes.formatSize;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImgurUploadTask extends CompletableTask<String> implements InitializingBean {

  private final ObjectMapper objectMapper;

  private final I18n i18n;
  private final ClientProperties clientProperties;

  private Image image;
  private int maxUploadSize;
  private String baseUrl;
  private String clientId;

  @Autowired
  public ImgurUploadTask(I18n i18n, ClientProperties clientProperties, ObjectMapper objectMapper) {
    super(Priority.HIGH);

    this.i18n = i18n;
    this.clientProperties = clientProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void afterPropertiesSet() {
    updateTitle(i18n.get("chat.imageUploadTask.title"));
    Upload uploadProperties = clientProperties.getImgur().getUpload();
    maxUploadSize = uploadProperties.getMaxSize();
    baseUrl = uploadProperties.getBaseUrl();
    clientId = uploadProperties.getClientId();
  }

  @Override
  protected String call() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
    ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

    Assert.state(
        byteArrayOutputStream.size() <= maxUploadSize,
        () -> "Image exceeds max upload size of " + formatSize(maxUploadSize, i18n.getUserSpecificLocale())
    );

    String dataImage = BaseEncoding.base64().encode(byteArrayOutputStream.toByteArray());
    String data = URLEncoder.encode("image", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(dataImage, StandardCharsets.UTF_8);

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

    ImgurRestResponse imgurRestResponse = objectMapper.readValue(stringBuilder.toString(), ImgurRestResponse.class);

    if (!imgurRestResponse.success) {
      throw new RuntimeException("Image upload failed, status code: " + imgurRestResponse.status);
    }

    return imgurRestResponse.data.link;
  }

  public void setImage(Image image) {
    this.image = image;
  }
}
