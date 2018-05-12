package com.faforever.client.remote.io;

import com.google.common.primitives.Bytes;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;

public class QDataWriterTest {

  private QDataWriter instance;
  private ByteArrayOutputStream outputStream;

  @Before
  public void setUp() throws Exception {
    outputStream = new ByteArrayOutputStream();
    instance = new QDataWriter(outputStream);
  }

  @Test
  public void testWriteFullLength() throws Exception {
    String testString = "test string 123";

    char[] buffer = testString.toCharArray();
    instance.write(buffer, 0, buffer.length);

    byte[] expected = testString.getBytes(StandardCharsets.UTF_16BE);
    assertArrayEquals(expected, outputStream.toByteArray());
  }

  @Test(expected = NullPointerException.class)
  public void testWritePartial() throws Exception {
    instance.write((char[]) null, 0, 0);
  }

  @Test
  public void testAppend() throws Exception {
    instance.append("test string");

    byte[] stringLengthBytes = new byte[]{0x00, 0x00, 0x00, 0x16};
    byte[] stringBytes = "test string".getBytes(StandardCharsets.UTF_16BE);

    byte[] expectedBytes = Bytes.concat(stringLengthBytes, stringBytes);

    assertArrayEquals(expectedBytes, outputStream.toByteArray());
  }

  @Test
  public void testAppendNull() throws Exception {
    instance.append(null);

    byte[] expectedBytes = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    assertArrayEquals(expectedBytes, outputStream.toByteArray());
  }

  @Test
  public void testFlush() throws Exception {
    instance = new QDataWriter(new BufferedOutputStream(outputStream));
    instance.write("hello");

    assertArrayEquals(new byte[0], outputStream.toByteArray());

    instance.flush();

    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_16BE), outputStream.toByteArray());
  }

  @Test
  public void testClose() throws Exception {
    instance = new QDataWriter(new BufferedOutputStream(outputStream));
    instance.write("hello");

    assertArrayEquals(new byte[0], outputStream.toByteArray());

    instance.close();

    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_16BE), outputStream.toByteArray());
  }

  @Test
  public void testAppendWithSize() throws Exception {
    byte[] bytes = {0x11, 0x22, 0x33};
    instance.appendWithSize(bytes);

    byte[] lengthBytes = new byte[]{0x00, 0x00, 0x00, 0x03};
    byte[] expectedBytes = Bytes.concat(lengthBytes, bytes);

    assertArrayEquals(expectedBytes, outputStream.toByteArray());
  }
}
