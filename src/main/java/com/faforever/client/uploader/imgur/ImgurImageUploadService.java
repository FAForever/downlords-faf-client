package com.faforever.client.uploader.imgur;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.util.Callback;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ImgurImageUploadService implements ImageUploadService {

  private static final String IMGUR_CLIENT_ID = "141ee8a7030f16d";
  private static final String IMGUR_BASE_URL = "https://api.imgur.com/3/image";
  private final Gson gson;

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  public ImgurImageUploadService() {
    gson = new GsonBuilder().create();
  }

  @Override
  public void uploadImage(Image image, Callback<String> callback) {
    taskService.submitTask(TaskGroup.NET_LIGHT, new PrioritizedTask<String>(i18n.get("chat.imageUploadTask.title")) {
      @Override
      protected String call() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);

        String dataImage = BaseEncoding.base64().encode(byteArrayOutputStream.toByteArray());
        String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(dataImage, "UTF-8");

        URL url = new URL(IMGUR_BASE_URL);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Authorization", "Client-ID " + IMGUR_CLIENT_ID);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.connect();

        try (OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream())) {
          writer.write(data);
          writer.flush();
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
    }, callback);
  }
}
