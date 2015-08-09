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
 * Represents a bsdiff Control Block.  Control blocks consist of a set of triples (x, y, z) meaning: <ul> <li>Add x
 * bytes from the old file to x bytes from the diff block</li> <li>Copy y bytes from the extra block</li> <li>Seek
 * forwards in the old file by z bytes</li> </ul>
 *
 * @author malensek
 */
class ControlBlock {

  /**
   * Length of the patch diff block
   */
  private int diffLength;

  /**
   * Length of the patch extra block
   */
  private int extraLength;

  /**
   * Bytes to seek forward after completing the control block directives.
   */
  private int seekLength;

  /**
   * Read a bsdiff control block from an input stream.
   */
  public ControlBlock(InputStream in) throws IOException {
    diffLength = Offset.readOffset(in);
    extraLength = Offset.readOffset(in);
    seekLength = Offset.readOffset(in);
  }

  /**
   * Writes a ControlBlock to an OutputStream.
   */
  public void write(OutputStream out) throws IOException {
    Offset.writeOffset(diffLength, out);
    Offset.writeOffset(extraLength, out);
    Offset.writeOffset(seekLength, out);
  }

  @Override
  public String toString() {
    return diffLength + ", " + extraLength + ", " + seekLength;
  }

  public int getDiffLength() {
    return diffLength;
  }

  public int getExtraLength() {
    return extraLength;
  }

  public int getSeekLength() {
    return seekLength;
  }
}
