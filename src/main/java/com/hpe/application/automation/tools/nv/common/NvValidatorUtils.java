/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.common;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class NvValidatorUtils {
    private static final Pattern FQ_CLASS_NAME_PATTERN = Pattern.compile("" +
            "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    private static final Pattern POSITIVE_FLOATING_POINT_PATTERN = Pattern.compile("^[+]?[0-9]*\\.?[0-9]+$");
    private static final String DEFAULT_THRESHOLD = "default";

    public static boolean isValidHostIp(String address) {
        return InetAddressValidator.getInstance().isValidInet4Address(address);
    }

    public static boolean validateClassName(String identifier) {
        return FQ_CLASS_NAME_PATTERN.matcher(identifier).matches();
    }

    public static boolean validateFloatingPoint(String number) {
        return POSITIVE_FLOATING_POINT_PATTERN.matcher(number).matches();
    }

    public static boolean validateFloatingPoint(String number, double maxValue) {
        boolean isValid = validateFloatingPoint(number);
        if(isValid) {
            Double dVal = Double.parseDouble(number);
            if(dVal < 0 || dVal > maxValue) {
                isValid = false;
            }
        }
        return isValid;
    }

    public static boolean validateFile(String fileName) {
        File thresholdFile = new File(fileName);
        Path filePath = thresholdFile.toPath();

        return Files.isReadable(filePath);
    }

    public static Map<String, Float> readThresholdsFile(String fileName) throws IOException {
        Map<String, Float> result = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));
            String line;
            String[] parts;
            while (null != (line = br.readLine())) {
                if (!isComment(line)) {
                    parts = line.split(",");
                    // default threshold
                    if (line.startsWith(DEFAULT_THRESHOLD)) {
                        if (parts.length != 2 || !NvValidatorUtils.validateFloatingPoint(parts[1])) {
                            return null;
                        }
                        result.put(parts[0], Float.parseFloat(parts[1]));
                    } else { // threshold per test
                        if (parts.length != 3 || !NvValidatorUtils.validateClassName(parts[0]) || parts[1].isEmpty() || !NvValidatorUtils.validateFloatingPoint(parts[2])) {
                            return null;
                        }
                        result.put(parts[0] + "." + parts[1], Float.parseFloat(parts[2]));
                    }
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return result;
    }

    private static boolean isComment(String line) {
        return line.startsWith("//") || line.startsWith("#");
    }
}
