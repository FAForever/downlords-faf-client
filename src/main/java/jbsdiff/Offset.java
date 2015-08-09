/*
Copyright (c) 2013, Colorado State University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

This software is provided by the copyright holders and contributors "as is" and
any express or implied warranties, including, but not limited to, the implied
warranties of merchantability and fitness for a particular purpose are
disclaimed. In no event shall the copyright holder or contributors be liable for
any direct, indirect, incidental, special, exemplary, or consequential damages
(including, but not limited to, procurement of substitute goods or services;
loss of use, data, or profits; or business interruption) however caused and on
any theory of liability, whether in contract, strict liability, or tort
(including negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.
*/

package jbsdiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * bsdiff encodes offsets (represented by the C off_t type) as 64-bit chunks. In this implementation only 32-bit signed
 * integers are supported, but the additional encoding steps are left to illustrate the process (which, in Java, would
 * encode/decode a long primitive data type).
 *
 * @author malensek
 */
class Offset {

  /**
   * Size of a bsdiff-encoded offset, in bytes.
   */
  public static final int OFFSET_SIZE = 8;

  /**
   * Reads a bsdiff-encoded offset (based on the C off_t type) from an {@link InputStream}.
   */
  public static int readOffset(InputStream in) throws IOException {
    byte[] buf = new byte[OFFSET_SIZE];
    int bytesRead = in.read(buf);
    if (bytesRead < OFFSET_SIZE) {
      throw new IOException("Could not read offset.");
    }

    int y = 0;
    y = buf[7] & 0x7F;
    y *= 256;
    y += buf[6] & 0xFF;
    y *= 256;
    y += buf[5] & 0xFF;
    y *= 256;
    y += buf[4] & 0xFF;
    y *= 256;
    y += buf[3] & 0xFF;
    y *= 256;
    y += buf[2] & 0xFF;
    y *= 256;
    y += buf[1] & 0xFF;
    y *= 256;
    y += buf[0] & 0xFF;

        /* An integer overflow occurred */
    if (y < 0) {
      throw new IOException(
          "Integer overflow: 64-bit offsets not supported.");
    }

    if ((buf[7] & 0x80) != 0) {
      y = -y;
    }

    return y;
  }

  /**
   * Writes a bsdiff-encoded offset to an {@link OutputStream}.
   *
   * @param value Integer value to encode and write
   */
  public static void writeOffset(int value, OutputStream out)
      throws IOException {
    byte[] buf = new byte[OFFSET_SIZE];
    int y = 0;

    if (value < 0) {
      y = -value;
            /* Set the sign bit */
      buf[7] |= 0x80;
    } else {
      y = value;
    }

    buf[0] |= y % 256;
    y -= buf[0] & 0xFF;
    y /= 256;
    buf[1] |= y % 256;
    y -= buf[1] & 0xFF;
    y /= 256;
    buf[2] |= y % 256;
    y -= buf[2] & 0xFF;
    y /= 256;
    buf[3] |= y % 256;
    y -= buf[3] & 0xFF;
    y /= 256;
    buf[4] |= y % 256;
    y -= buf[4] & 0xFF;
    y /= 256;
    buf[5] |= y % 256;
    y -= buf[5] & 0xFF;
    y /= 256;
    buf[6] |= y % 256;
    y -= buf[6] & 0xFF;
    y /= 256;
    buf[7] |= y % 256;

    out.write(buf);
  }
}
