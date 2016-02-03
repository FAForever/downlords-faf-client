package com.faforever.client.map;

import com.faforever.client.game.MapBean;
import com.faforever.client.preferences.gson.PropertyTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.beans.property.Property;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class MapInfoBeanIterator implements InputIterator {

  private final Gson gson;
  private MapBean currentMapBean;
  private Iterator<MapBean> mapIterator;

  public MapInfoBeanIterator(Iterator<MapBean> mapIterator) {
    this.mapIterator = mapIterator;
    this.gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(Property.class, PropertyTypeAdapter.INSTANCE)
        .create();
  }

  @Override
  public long weight() {
    return (long) (currentMapBean.getDownloads() * currentMapBean.getPlays() * currentMapBean.getRating());
  }

  @Override
  public BytesRef payload() {
    try {
      return new BytesRef(serialize(currentMapBean));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasPayloads() {
    return true;
  }

  @Override
  public Set<BytesRef> contexts() {
    return null;
  }

  @Override
  public boolean hasContexts() {
    return false;
  }

  private byte[] serialize(MapBean mapBean) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)) {
      gson.toJson(mapBean, writer);
    }
    return byteArrayOutputStream.toByteArray();
  }

  public BytesRef next() {
    if (!mapIterator.hasNext()) {
      return null;
    }
    currentMapBean = mapIterator.next();
    return new BytesRef((currentMapBean.getDisplayName() + currentMapBean.getTechnicalName()).getBytes(StandardCharsets.UTF_8));
  }

  public MapBean deserialize(byte[] bytes) {
    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, MapBean.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
