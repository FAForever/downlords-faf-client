package com.faforever.client.mod;

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

public class ModInfoBeanIterator implements InputIterator {

  private final Gson gson;
  private ModInfoBean currentMod;
  private Iterator<ModInfoBean> modIterator;

  public ModInfoBeanIterator(Iterator<ModInfoBean> modIterator) {
    this.modIterator = modIterator;
    this.gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(Property.class, PropertyTypeAdapter.INSTANCE)
        .create();
  }

  @Override
  public long weight() {
    return currentMod.getDownloads() * currentMod.getLikes();
  }

  @Override
  public BytesRef payload() {
    try {
      return new BytesRef(serialize(currentMod));
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

  private byte[] serialize(ModInfoBean modInfoBean) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)) {
      gson.toJson(modInfoBean, writer);
    }
    return byteArrayOutputStream.toByteArray();
  }

  public BytesRef next() {
    if (!modIterator.hasNext()) {
      return null;
    }
    currentMod = modIterator.next();
    return new BytesRef(currentMod.getName().getBytes(StandardCharsets.UTF_8));
  }

  public ModInfoBean deserialize(byte[] bytes) {
    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, ModInfoBean.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
