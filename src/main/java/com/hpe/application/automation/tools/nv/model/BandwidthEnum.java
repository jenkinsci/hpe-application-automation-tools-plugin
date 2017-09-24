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

package com.hpe.application.automation.tools.nv.model;

public enum BandwidthEnum {
    UNRESTRICTED(0, "Unrestricted"),
    KBPS14_4(14.4, "14.4"),
    KBPS28_8(28.8, "28.8"),
    KBPS33_6(33.6, "33.6"),
    KBPS56(56, "56"),
    KBPS64(64, "64"),
    KBPS128(128, "128"),
    KBPS256(256, "256"),
    KBPS512(512, "512"),
    KBPS768(768, "768"),
    KBPS1544(1544, "1544"),
    KBPS2048(2048, "2048"),
    KBPS4096(4096, "4096"),
    KBPS6144(6144, "6144"),
    KBPS8192(8192, "8192"),
    KBPS10240(10240, "10240"),
    KBPS34364(34364, "34364"),
    KBPS44736(44736, "44736"),
    KBPS54000(54000, "54000"),
    KBPS100000(100000, "100000");

    private double value;
    private String displayText;

    private BandwidthEnum(double value, String displayText) {
        this.value = value;
        this.displayText = displayText;
    }

    public double getValue() {
        return value;
    }

    public String getDisplayText() {
        return displayText;
    }
}
