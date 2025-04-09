package com.microfocus.application.automation.tools.commonResultUpload.uploader;

import org.apache.commons.lang.StringUtils;

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
