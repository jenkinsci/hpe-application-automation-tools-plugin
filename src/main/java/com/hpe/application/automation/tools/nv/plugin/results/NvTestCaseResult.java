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

package com.hpe.application.automation.tools.nv.plugin.results;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class NvTestCaseResult extends CountableResult implements Serializable {
    private static final long serialVersionUID = -5125059428661210112L;

    private float threshold;
    private String errorMessage;
    private String errorStackTrace;

    @JsonCreator
    public NvTestCaseResult(@JsonProperty("name") String name) {
        super(name);
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    @JsonIgnore
    public boolean isSkipped() {
        return getSkipCount() > 0;
    }

    @JsonIgnore
    public void setSkipped() {
        setSkipCount(1);
        setFailCount(0);
        setErrorCount(0);
        setPassCount(0);
    }

    @JsonIgnore
    public boolean isFail() {
        return getFailCount() > 0;
    }

    @JsonIgnore
    public void setFailed() {
        setSkipCount(0);
        setFailCount(1);
        setErrorCount(0);
        setPassCount(0);
    }

    @JsonIgnore
    public boolean isError() {
        return getErrorCount() > 0;
    }

    @JsonIgnore
    public void setError() {
        setSkipCount(0);
        setFailCount(0);
        setErrorCount(1);
        setPassCount(0);
    }

    @JsonIgnore
    public boolean isPass() {
        return getPassCount() > 0;
    }

    @JsonIgnore
    public void setPassed() {
        setSkipCount(0);
        setFailCount(0);
        setErrorCount(0);
        setPassCount(1);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
}
