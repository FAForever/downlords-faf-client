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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Data structure that encapsulates a bsdiff header.  The header is composed of
 * 8-byte fields, starting with the magic number "BSDIFF40."
 *
 * 0: BSDIFF40
 * 8: length of control block
 * 16: length of the diff block
 * 24: size of the output file
 *
 * @author malensek
 */

class Header {

    /** Size of the Header, in bytes.  4 fields * 8 bytes = 32 bytes */
    public static final int HEADER_SIZE = 32;

    /** Magic number to mark the start of a bsdiff header. */
    public static final String HEADER_MAGIC = "BSDIFF40";

    private String magic;
    private int controlLength;
    private int diffLength;
    private int outLength;

    public Header() { }

    /**
     * Read a bsdiff header from an InputStream.
     */
    public Header(InputStream in) throws IOException, InvalidHeaderException {
        InputStream headerIn = new DataInputStream(in);
        byte[] buf = new byte[8];

        headerIn.read(buf);
        magic = new String(buf);
        if (!magic.equals("BSDIFF40")) {
            throw new InvalidHeaderException("Header missing magic number");
        }

        controlLength = Offset.readOffset(headerIn);
        diffLength = Offset.readOffset(headerIn);
        outLength = Offset.readOffset(headerIn);

        verify();
    }

    public Header(int controlLength, int diffLength, int outLength)
    throws InvalidHeaderException {
        this.controlLength = controlLength;
        this.diffLength = diffLength;
        this.outLength = outLength;

        verify();
    }

    /**
     * Writes the Header to an OutputStream.
     */
    public void write(OutputStream out) throws IOException {
        out.write(HEADER_MAGIC.getBytes());
        Offset.writeOffset(controlLength, out);
        Offset.writeOffset(diffLength, out);
        Offset.writeOffset(outLength, out);
    }

    /**
     * Verifies the values of the header fields.
     */
    private void verify() throws InvalidHeaderException {
        if (controlLength < 0) {
            throw new InvalidHeaderException("control block length",
                    controlLength);
        }

        if (diffLength < 0) {
            throw new InvalidHeaderException("diff block length", diffLength);
        }

        if (outLength < 0) {
            throw new InvalidHeaderException("output file length", outLength);
        }
    }

    @Override
    public String toString() {
        String s = "";

        s += magic + "\n";
        s += "control bytes = " + controlLength + "\n";
        s += "diff bytes = " + diffLength + "\n";
        s += "output size = " + outLength;

        return s;
    }

    public int getControlLength() {
        return controlLength;
    }

    public void setControlLength(int length) throws InvalidHeaderException {
        controlLength = length;
        verify();
    }

    public int getDiffLength() {
        return diffLength;
    }

    public void setDiffLength(int length) throws InvalidHeaderException {
        diffLength = length;
        verify();
    }

    public int getOutputLength() {
        return outLength;
    }

    public void setOutputLength(int length) throws InvalidHeaderException {
        outLength = length;
        verify();
    }
}
