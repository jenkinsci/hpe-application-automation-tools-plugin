/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2026 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.commonResultUpload.uploader;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UploaderHelper {
    private String stepMessage;
    private String regEx;
    private Map<String,String> stepStatus;

    public UploaderHelper(String stepMessage, String regEx, Map<String,String> stepStatus) {
        this.stepMessage = stepMessage;
        this.regEx = regEx;
        this.stepStatus = stepStatus;
    }

    /*public static void main(String[] args) {
        new UploaderHelper(null,null,null).parseMessage();
    }*/

    public List<StepBean> parseMessage() {
        Map<String, String> stepToActualValue = new LinkedHashMap<>();
        Map<String, String> stepToStatus = new HashMap<>();
        Pattern pattern = Pattern.compile(regEx, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(stepMessage);
        while (matcher.find()) {
            String status = matcher.group(1);
            String step = matcher.group(2);
            if (StringUtils.isEmpty(status) || StringUtils.isEmpty(step)) {
                throw new RuntimeException("Invalid inputs. Can't determine step or step status.");
            }
            String actualValue = matcher.group(3);
            if (StringUtils.isEmpty(actualValue)) {
                actualValue = "";
            }
            stepToActualValue.put(step, actualValue);
            stepToStatus.put(step, parseStepStatus(status,stepStatus));
        }
        return buildStepBeans(stepToActualValue, stepToStatus);
    }

    private List<StepBean> buildStepBeans(Map<String, String> stepToActualValue,Map<String, String> stepToStatus) {
        if (stepToActualValue==null
                || stepToActualValue.isEmpty()
                || stepToStatus == null
                || stepToStatus.isEmpty()) {
            return null;
        }
        List<StepBean> stepBeans = new ArrayList<>();
        int order = 1;
        for (Map.Entry<String, String> entry : stepToActualValue.entrySet()) {
            StepBean stepBean = new StepBean();
            stepBean.setStepName(entry.getKey());
            stepBean.setStatus(stepToStatus.get(entry.getKey()));
            stepBean.setOrder(order++);
            stepBean.setActualValue(entry.getValue());
            stepBeans.add(stepBean);
        }
        return stepBeans;
    }

    private static String parseStepStatus(String status, Map<String,String> stepStatus) {
        if (StringUtils.isEmpty(stepStatus.get(status))) {
            throw new RuntimeException("Can't find relevant step status mapping:->" + status);
        }
        return stepStatus.get(status);
    }
}
