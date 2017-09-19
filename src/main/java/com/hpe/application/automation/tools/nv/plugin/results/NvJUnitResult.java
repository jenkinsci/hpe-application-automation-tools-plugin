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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hpe.application.automation.tools.nv.model.NvProfileDTO;

import java.io.Serializable;
import java.util.*;

public class NvJUnitResult implements Serializable {
    private static final long serialVersionUID = 74133989369447160L;

    private ArrayList<NvProfileDTO> profiles = new ArrayList<>();
    private Map<String, NvTestSuiteResult> results = new HashMap<>();

    public ArrayList<NvProfileDTO> getProfiles() {
        return profiles;
    }

    public void setProfiles(ArrayList<NvProfileDTO> profiles) {
        this.profiles = profiles;
    }

    @JsonGetter("suites")
    public List<NvTestSuiteResult> getResults() {
        Collection<NvTestSuiteResult> values = results.values();
        if(values instanceof List) {
            return (List)values;
        } else {
            return new ArrayList<>(values);
        }
    }

    @JsonSetter
    public void setResults(List<NvTestSuiteResult> results) {
        if (null != results) {
            for (NvTestSuiteResult result : results) {
                this.results.put(result.getName(), result);
            }
        }
    }

    protected NvTestSuiteResult getNvTestSuiteResult(String testSuiteName) {
        return results.get(testSuiteName);
    }

    protected void addNvTestSuiteResult(NvTestSuiteResult testSuiteResult) {
        results.put(testSuiteResult.getName(), testSuiteResult);
    }

    public void tally() {
        for (NvTestSuiteResult testSuiteResult : results.values()) {
            testSuiteResult.tally();
        }
    }
}
