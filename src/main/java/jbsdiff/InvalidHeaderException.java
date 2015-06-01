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

/**
 * An exception that indicates a malformed bsdiff header.
 *
 * @author malensek
 */

public class InvalidHeaderException extends Exception {

    private static final long serialVersionUID = -3712364093810940826L;

    public InvalidHeaderException() {
        super();
    }

    public InvalidHeaderException(String detail) {
        super(detail);
    }

    /**
     * Creates an InvalidHeaderException with details about the invalid field
     * that was set, and its value.
     */
    public InvalidHeaderException(String fieldName, int value) {
        super("Invalid header field; " + fieldName + " = " + value);
    }
}

