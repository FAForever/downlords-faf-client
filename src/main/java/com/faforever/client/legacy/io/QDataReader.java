package com.faforever.client.legacy.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class QDataReader extends Reader {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DataInput dataInput;
  private Charset charset;

  public QDataReader(DataInput dataInput) {
    this(dataInput, StandardCharsets.UTF_16BE);
  }

  public QDataReader(DataInput dataInput, Charset charset) {
    this.dataInput = dataInput;
    this.charset = charset;
  }

  public String readQString() throws IOException {
    int stringSize = dataInput.readInt();
    byte[] buffer = new byte[stringSize];
    dataInput.readFully(buffer);
    return new String(buffer, charset);
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws IOException {
    if (dataInput instanceof Closeable) {
      ((Closeable) dataInput).close();
    }
  }

  public int readInt32() throws IOException {
    return dataInput.readInt();
  }

  /**
   * Skip the "block size" bytes, since we just don't care.
   */
  public void skipBlockSize() throws IOException {
    dataInput.skipBytes(Integer.SIZE / Byte.SIZE);
  }

  public int readShort() throws IOException {
    return dataInput.readUnsignedShort();
  }

  /**
   * @return the number of bytes read
   */
  public int readQByteArray(byte[] buffer) throws IOException {
    // Skip first 4 bytes that tell us this is a QByteArray as well as the 1-byte null flag
    dataInput.skipBytes(5);
    int arraySize = dataInput.readInt();

    logger.trace("Trying to read {} bytes", arraySize);

    if (arraySize == 0xffffffff) {
      // 0xffffffff means the array is null
      return 0;
    }
    dataInput.readFully(buffer, 0, arraySize);
    return arraySize;
  }
}
