/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.results.lrscriptresultparser;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by kazaky on 27/03/2017.
 */
public class LrScriptResultsSanitizer extends FilterReader {
    /**
     * Creates a new filtered reader.
     *
     * @param in a Reader object providing the underlying stream.
     * @throws NullPointerException if <code>in</code> is <code>null</code>
     */
    public LrScriptResultsSanitizer(Reader in) {
        super(in);
    }

    /**
     * This is another no-op read() method we have to implement. We implement it
     * in terms of the method above. Our superclass implements the remaining
     * read() methods in terms of these two.
     */
    @Override
    public int read() throws IOException {
        char[] buf = new char[1];
        int result = read(buf, 0, 1);
        if (result == -1) {
            return -1;
        } else {
            return (int) buf[0];
        }
    }

    @Override
    public int read(char[] buf, int from, int len) throws IOException {
        int numchars = 0; // how many characters have been read
        // Loop, because we might read a bunch of characters, then strip them
        // all out, leaving us with zero characters to return.
        while (numchars == 0) {
            numchars = in.read(buf, from, len); // Read characters
            if (numchars == -1)
                return -1; // Check for EOF and handle it.

            // Loop through the characters we read, stripping out HTML tags.
            // Characters not in tags are copied over previous tags
            int last = from; // Index of last non-HTML char
            for (int i = from; i < from + numchars; i++) {

                if(!isBadXMLChar(buf[i]))
                {
                    buf[last] = buf[i];
                    last++;
                }
            }


            numchars = last - from; // Figure out how many characters remain
        } // And if it is more than zero characters
        return numchars; // Then return that number.
    }

    @SuppressWarnings("squid:S109")
    private static boolean isBadXMLChar(char current)
    {
        switch(current){
            case 9:
            case 10:
            case 13:
                return false;
            default:
                break;
        }

        return !(((current >= 57344) && (current <= 0xfffd)) || ((current >= 32) && (current <= 55295)));
    }

}
