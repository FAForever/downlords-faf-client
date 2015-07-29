package com.faforever.client.legacy.io;

import com.google.common.primitives.Bytes;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class QDataOutputStreamTest {

  private QDataOutputStream instance;
  private ByteArrayOutputStream outputStream;

  @Before
  public void setUp() throws Exception {
    outputStream = new ByteArrayOutputStream();
    instance = new QDataOutputStream(outputStream);
  }

  @Test
  public void testWriteQByteArrayNull() throws Exception {
    instance.writeQByteArray(null);

    byte[] expectedBytes = {
        // 12 denotes QByteArray
        0, 0, 0, 12,
        // Delimiter
        0,
        // The actual bytes
        -1, -1, -1, -1};
    assertArrayEquals(expectedBytes, outputStream.toByteArray());
  }

  @Test
  public void testWriteQByteArray() throws Exception {
    byte[] bytes = "Content".getBytes(StandardCharsets.UTF_16BE);
    instance.writeQByteArray(bytes);

    byte[] prefixBytes = {
        // 12 denotes QByteArray
        0, 0, 0, 12,
        // Delimiter
        0,
        // Array size
        0, 0, 0, (byte) bytes.length
    };

    byte[] expectedBytes = Bytes.concat(prefixBytes, bytes);

    assertArrayEquals(expectedBytes, outputStream.toByteArray());
  }
}
