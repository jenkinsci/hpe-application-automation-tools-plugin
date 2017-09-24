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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.util.*;

public abstract class CountableResultParent<T extends CountableResult> extends CountableResult implements Serializable {
    private static final long serialVersionUID = -4125396948358407713L;

    @JsonIgnore
    private Map<String, T> results = new HashMap<>();
    
    protected CountableResultParent() {
    }

    protected CountableResultParent(String name) {
        super(name);
    }

    public List<T> getResults() {
        Collection<T> values = results.values();
        if (values instanceof List) {
            return (List) values;
        } else {
            return new ArrayList<>(values);
        }
    }

    @JsonSetter
    public void setResults(List<T> results) {
        if (null != results) {
            for (T result : results) {
                this.results.put(result.getName(), result);
            }
        }
    }

    protected T getResult(String name) {
        return results.get(name);
    }
    
    protected void addResult(T result) {
        results.put(result.getName(), result);
    }
    
    protected void addDuration(float duration) {
        setDuration(getDuration() + duration);
    }

    protected void addFailCount(int failCount) {
        setFailCount(getFailCount() + failCount);
    }

    protected void addErrorCount(int errorCount) {
        setErrorCount(getErrorCount() + errorCount);
    }

    protected void addSkipCount(int skipCount) {
        setSkipCount(getSkipCount() + skipCount);
    }

    protected void addPassCount(int passCount) {
        setPassCount(getPassCount() + passCount);
    }

    public void tally() {
        for (T result : results.values()) {
            result.tally();
            addDuration(result.getDuration());
            addFailCount(result.getFailCount());
            addErrorCount(result.getErrorCount());
            addSkipCount(result.getSkipCount());
            addPassCount(result.getPassCount());
        }
    }
    
}
