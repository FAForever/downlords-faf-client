package com.faforever.client.remote.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class QDataOutputStream extends DataOutputStream {

  /**
   * FAF currently ready/writes QByteArrays as QVariant, which is like the "object writer" for QT. To know what data
   * type is being read, a "type of the data" number prefixes the data. This constant holds this value.
   * <p>
   * See <a href="http://doc.qt.io/qt-4.8/datastreamformat.html">http://doc.qt.io/qt-4.8/datastreamformat.html</a>
   */
  private static final int Q_BYTE_ARRAY_DATA_TYPE = 12;

  public QDataOutputStream(OutputStream outputStream) {
    super(outputStream);
  }

  public void writeQByteArray(byte[] data) throws IOException {
    writeInt(Q_BYTE_ARRAY_DATA_TYPE);
    writeByte(0);

    if (data == null) {
      writeInt(-1);
    } else {
      writeInt(data.length);
      write(data);
    }
  }
}
