package com.faforever.client.map;

import com.google.common.io.LittleEndianDataInputStream;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.nocatch.NoCatch.noCatch;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.list;
import static org.luaj.vm2.lib.jse.JsePlatform.standardGlobals;

public final class PreviewGenerator {

  private static final double RESOURCE_ICON_RATIO = 0.01953125;
  private static final String MASS_IMAGE = "/images/map_markers/mass.png";
  private static final String HYDRO_IMAGE = "/images/map_markers/hydro.png";
  private static final String ARMY_IMAGE = "/images/map_markers/army.png";

  private PreviewGenerator() {
    throw new AssertionError("Not instantiatable");
  }

  public static javafx.scene.image.Image generatePreview(Path mapFolder, int width, int height) {
    Path mapPath = noCatch(() -> list(mapFolder))
        .filter(file -> file.getFileName().toString().endsWith(".scmap"))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No map file was found in: " + mapFolder.toAbsolutePath()));

    return noCatch(() -> {
      MapData mapData = parseMap(mapPath);

      BufferedImage previewImage = getDdsImage(mapData);
      previewImage = scale(previewImage, width, height);

      addMarkers(previewImage, mapData);

      return SwingFXUtils.toFXImage(previewImage, new WritableImage(width, height));
    });
  }

  private static MapData parseMap(Path mapPath) throws IOException {
    MapData mapData = new MapData();
    try (LittleEndianDataInputStream mapInput = new LittleEndianDataInputStream(Files.newInputStream(mapPath))) {
      mapInput.skip(16);
      mapData.setWidth((int) mapInput.readFloat());
      mapData.setHeight((int) mapInput.readFloat());
      mapInput.skip(6);

      int ddsSize = mapInput.readInt();
      // Skip DDS header
      mapInput.skipBytes(128);

      byte[] buffer = new byte[ddsSize - 128];
      mapInput.readFully(buffer);

      mapData.setDdsData(buffer);

      Path lua = Paths.get(mapPath.toAbsolutePath().toString().replace(".scmap", "_save.lua"));
      if (isRegularFile(lua)) {
        Globals globals = standardGlobals();

        globals.baselib.load(globals.load(new InputStreamReader(PreviewGenerator.class.getResourceAsStream("/lua/faf.lua")), "irrelevant"));

        globals.loadfile(lua.toAbsolutePath().toString()).invoke();

        LuaTable markers = globals.get("Scenario").get("MasterChain").get("_MASTERCHAIN_").get("Markers").checktable();
        mapData.setMarkers(markers);
      }
    }
    return mapData;
  }

  private static BufferedImage getDdsImage(MapData mapData) throws IOException {
    byte[] ddsData = mapData.getDdsData();
    int ddsDimension = (int) (Math.sqrt(ddsData.length) / 2);

    bgraToAbgr(ddsData);
    BufferedImage previewImage = new BufferedImage(ddsDimension, ddsDimension, BufferedImage.TYPE_4BYTE_ABGR);
    previewImage.setData(Raster.createRaster(previewImage.getSampleModel(), new DataBufferByte(ddsData, ddsData.length), new Point()));
    return previewImage;
  }

  private static BufferedImage scale(BufferedImage previewImage, double width, double height) {
    int targetWidth = width < 1 ? 1 : (int) width;
    int targetHeight = height < 1 ? 1 : (int) height;

    Image image = previewImage.getScaledInstance(targetWidth, targetHeight, SCALE_SMOOTH);
    BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_ARGB);

    Graphics graphics = scaledImage.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();

    return scaledImage;
  }

  private static void addMarkers(BufferedImage previewImage, MapData mapData) throws IOException {
    float width = previewImage.getWidth();
    float height = previewImage.getHeight();

    Image massImage = scale(readImage(MASS_IMAGE), RESOURCE_ICON_RATIO * width, RESOURCE_ICON_RATIO * height);
    Image hydroImage = scale(readImage(HYDRO_IMAGE), RESOURCE_ICON_RATIO * width, RESOURCE_ICON_RATIO * height);
    Image armyImage = scale(readImage(ARMY_IMAGE), RESOURCE_ICON_RATIO * width, RESOURCE_ICON_RATIO * height);

    LuaTable markers = mapData.getMarkers();
    for (LuaValue key : markers.keys()) {
      LuaTable markerData = markers.get(key).checktable();

      switch (markerData.get("type").toString()) {
        case "Mass":
          addMarker(massImage, mapData, markerData, previewImage);
          break;
        case "Hydrocarbon":
          addMarker(hydroImage, mapData, markerData, previewImage);
          break;
        case "Blank Marker":
          addMarker(armyImage, mapData, markerData, previewImage);
          break;
      }
    }
  }

  private static void bgraToAbgr(byte[] buffer) {
    for (int i = 0; i < buffer.length; i += 4) {
      byte a = buffer[i + 3];
      buffer[i + 3] = buffer[i + 2];
      buffer[i + 2] = buffer[i + 1];
      buffer[i + 1] = buffer[i];
      buffer[i] = a;
    }
  }

  private static BufferedImage readImage(String resource) throws IOException {
    try (InputStream inputStream = PreviewGenerator.class.getResourceAsStream(resource)) {
      return ImageIO.read(inputStream);
    }
  }

  private static void addMarker(Image source, MapData mapData, LuaTable markerData, BufferedImage target) throws IOException {
    LuaTable vector = markerData.get("position").checktable();
    float x = vector.get(1).tofloat() / mapData.getWidth();
    float y = vector.get(3).tofloat() / mapData.getHeight();

    paintOnImage(source, x, y, target);
  }

  private static void paintOnImage(Image overlay, float xPercent, float yPercent, BufferedImage baseImage) {
    int overlayWidth = overlay.getWidth(null);
    int overlayHeight = overlay.getHeight(null);
    int x = (int) (xPercent * baseImage.getWidth() - overlayWidth / 2);
    int y = (int) (yPercent * baseImage.getHeight() - overlayHeight / 2);

    x = Math.min(Math.max(0, x), baseImage.getWidth() - overlayWidth);
    y = Math.min(Math.max(0, y), baseImage.getHeight() - overlayHeight);

    baseImage.getGraphics().drawImage(overlay, x, y, null);
  }
}
