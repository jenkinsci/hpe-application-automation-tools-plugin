/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.commonResultUpload.uploader;

import com.microfocus.application.automation.tools.commonResultUpload.CommonUploadLogger;
import com.microfocus.application.automation.tools.commonResultUpload.service.CustomizationService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RestService;
import com.microfocus.application.automation.tools.commonResultUpload.service.RunStatusResolver;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.configloader.RunStatusMapLoader;
import com.microfocus.application.automation.tools.results.service.AttachmentUploadService;
import com.microfocus.application.automation.tools.results.service.almentities.AlmCommonProperties;
import com.microfocus.application.automation.tools.results.service.almentities.AlmRun;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestInstance;
import com.microfocus.application.automation.tools.results.service.almentities.IAlmConsts;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Base64Encoder;

import java.util.*;

import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.ACTUAL_USER;
import static com.microfocus.application.automation.tools.commonResultUpload.ParamConstant.RUN_STATUS_MAPPING;

public class RunUploader {

    public static final String RUN_PREFIX = "runs";
    public static final String RUN_STEP_PREFIX = "run-steps";
    public static final String DESSTEP_REST_PREFIX = "design-steps";
    private static final String RUN_VERSION_MAP_NAME = "udf|Run On Version";
    private static final String VC_VERSION_NUMBER = "vc-version-number";

    private CommonUploadLogger logger;
    private Map<String, String> params;
    private Map<String, String> runStatusMapping;
    private RestService restService;
    private CustomizationService customizationService;

    public RunUploader(CommonUploadLogger logger, Map<String, String> params,
                       RestService restService, CustomizationService customizationService,
                       Map<String, String> runStatusMapping) {
        this.logger = logger;
        this.params = params;
        this.restService = restService;
        this.customizationService = customizationService;
        this.runStatusMapping = runStatusMapping;
    }

    public void upload(Map<String, String> testset,
                       Map<String, String> test,
                       Map<String, String> testconfig,
                       Map<String, String> testinstance,
                       Map<String, String> run,
                       boolean isCreateDesStep) {

        // Get attachment info and remove
        String attachment = run.get("attachment");
        run.remove("attachment");

        boolean containsStep = false;
        if (run.containsKey("stepMessage")) {
            containsStep = true;
        }

        String stepMessage = run.get("stepMessage");
        run.remove("stepMessage");

        String stepRegEx = run.get("stepRegEx");
        run.remove("stepRegEx");

        // Set relations
        run.put(AlmRun.RUN_CONFIG_ID, testconfig.get(AlmCommonProperties.ID));
        run.put(AlmRun.RUN_CYCLE_ID, testset.get(AlmCommonProperties.ID));
        run.put(AlmRun.RUN_TEST_ID, test.get(AlmCommonProperties.ID));
        run.put(AlmRun.RUN_TESTCYCL_UNIQUE_ID, testinstance.get(AlmCommonProperties.ID));
        // Set calculated values
        run.put(AlmCommonProperties.OWNER, params.get(ACTUAL_USER));
        run.put(AlmCommonProperties.NAME, generateImportRunName());
        run.put(AlmRun.RUN_DURATION, convertDuration(run.get(AlmRun.RUN_DURATION)));
        run.put(AlmRun.RUN_SUBTYPE_ID,
                customizationService.getRunSubtypeIdByTestInstance(
                        testinstance.get(AlmTestInstance.TEST_INSTANCE_SUBTYPE_ID)));
        // Set values for external test
        if (StringUtils.isNotEmpty(run.get(AlmRun.RUN_DETAIL))) {
            run.put(AlmRun.RUN_DETAIL, convertDetail(run.get(AlmRun.RUN_DETAIL)));
        }

        if (!shouldProceedVersionForRun(test, run)) {
            return;
        }

        if (isCreateDesStep) {//delete existing design steps then create new ones.
            String query = String.format("fields=id,name&query={parent-id[%s]}",test.get(AlmCommonProperties.ID));
            List<Map<String, String>> steps = restService.get(null, DESSTEP_REST_PREFIX, query);
            if (steps!=null && steps.size()>0) {
                List<String> ids = new ArrayList<>();
                for (Map<String, String> step : steps) {
                    ids.add(step.get("id"));
                }
                query = "ids-to-delete=" + String.join(",",ids);
                restService.bulkDelete(DESSTEP_REST_PREFIX, query);
            }
        }

        // Update test instance status
        String runstatus = RunStatusResolver.getRunStatus(run.get(AlmRun.RUN_STATUS), runStatusMapping);

        String runId = null;

        if (StringUtils.isNotEmpty(runstatus)) {
            // Create a run without status
            run.remove(AlmRun.RUN_STATUS);
            Map<String, String> createdRun = restService.create(RUN_PREFIX, run);
            runId = createdRun.get(AlmCommonProperties.ID);
            // Update status of the run
            Map<String, String> updateRun = new HashMap<>();
            updateRun.put(AlmCommonProperties.ID, createdRun.get(AlmCommonProperties.ID));
            updateRun.put(AlmRun.RUN_STATUS, runstatus);

            // Retry update run status 3 times. For some ALM server may has limited DB connections then the update may fail.
            // Added here because only here uses RestService.update.
            // Otherwise the retry could be in RestService.update or UpdateAlmEntityEntityRequest.perform
            // depends on whether the result would be changed if update multiple times.
            // But this should be fixed at ALM server side to larger the connection number I think.

            Map<String, String> updateResult = restService.update(RUN_PREFIX, updateRun);
            if (updateResult == null) {
                for (int i = 0; i < 3; i++) {
                    if (updateResult == null) {
                        updateResult = restService.update(RUN_PREFIX, updateRun);
                    } else {
                        break;
                    }
                }
            }
            if (StringUtils.isNotEmpty(attachment) && updateResult != null) {
                AttachmentUploadService.getInstance().upload(attachment, RUN_PREFIX, updateResult.get("id"));
            }

        } else {
            Map<String, String> createdRun = restService.create(RUN_PREFIX, run);
            runId = createdRun.get(AlmCommonProperties.ID);
            if (StringUtils.isNotEmpty(attachment)) {
                AttachmentUploadService.getInstance().upload(attachment, RUN_PREFIX, createdRun.get("id"));
            }
        }
        if (containsStep) {
            Map<String,String> stepStatus = RunStatusMapLoader.load(this.params.get(RUN_STATUS_MAPPING),logger).getStepStatus();
            if (stepStatus == null || stepStatus.size() == 0) {
                throw new RuntimeException("Please configure 'stepstatus' mapping.");
            }
            if (StringUtils.isEmpty(stepRegEx)) {
                throw new RuntimeException("Please configure 'stepRegEx' mapping.");
            }
            List<StepBean> stepBeans = new UploaderHelper(stepMessage, stepRegEx, stepStatus).parseMessage();

            if (stepBeans ==null || stepBeans.size()==0) {
                logger.info("No any new run steps to be detected.");
            } else {
                restService.bulkCreate(RUN_STEP_PREFIX, buildRunStepBody(stepBeans,runId));
                if (isCreateDesStep) {
                    restService.bulkCreate(DESSTEP_REST_PREFIX, buildDesStepBody(stepBeans,test.get(AlmCommonProperties.ID)));
                }
            }
        }
    }

    private Map<String, Map<String, String>> buildDesStepBody(List<StepBean> stepBeans, String testId) {
        Map<String, Map<String, String>> stepBody = new LinkedHashMap<String, Map<String, String>>();
        for (StepBean stepBean : stepBeans) {
            Map<String,String> step = new HashMap<>();
            step.put("step-order","" + stepBean.getOrder());
            step.put("name","Step " + stepBean.getOrder());
            step.put("parent-id",testId);
            step.put("expected","n/a");
            step.put("description",stepBean.getStepName());
            stepBody.put(stepBean.getStepName(), step);
        }
        return stepBody;
    }

    private Map<String, Map<String, String>> buildRunStepBody(List<StepBean> stepBeans, String runId) {
        Map<String, Map<String, String>> stepBody = new LinkedHashMap<String, Map<String, String>>();
        for (StepBean stepBean : stepBeans) {
            Map<String,String> step = new HashMap<>();
            step.put("step-order","" + stepBean.getOrder());
            step.put("name","Step " + stepBean.getOrder());
            step.put("parent-id",runId);
            step.put("expected","n/a");
            step.put("description",stepBean.getStepName());
            step.put("status",stepBean.getStatus());
            step.put("actual",escapeHTML(stepBean.getActualValue()));
            stepBody.put(stepBean.getStepName(), step);
        }
        return stepBody;
    }

    private String escapeHTML(String actualValue) {
        if (StringUtils.isEmpty(actualValue)) {
            return "";
        }
        actualValue = StringEscapeUtils.escapeHtml(actualValue);
        String [] lines = actualValue.split("(\\r?\\n)");
        StringBuilder htmlBodyBuilder = new StringBuilder();
        for (String line : lines) {
            String replacedLine = line.replaceAll(" ", "&nbsp;");
            htmlBodyBuilder.append(generateHtmlBody(replacedLine));
        }
        return "<html>\n<body>\n" + htmlBodyBuilder + "</body>\n</html>";
    }

    private String generateHtmlBody(String body) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<div align=\"left\" style=\"min-height: 9pt;\">");
        htmlBuilder.append("<font face=\"Arial\">");
        htmlBuilder.append("<span dir=\"ltr\" style=\"font-size:8pt;\">");
        htmlBuilder.append(body);
        htmlBuilder.append("</span>");
        htmlBuilder.append("</font>");
        htmlBuilder.append("</div>");
        htmlBuilder.append("\n");
        return htmlBuilder.toString();
    }

    private String convertDetail(String detail) {
        if (StringUtils.isNotEmpty(detail)) {
            detail = detail.replaceAll("<", "&lt;");
            detail = detail.replaceAll(">", "&gt;");
            return Base64Encoder.encode(detail.getBytes());
        }
        return detail;
    }

    private String convertDuration(String duration) {
        if (duration != null && duration.length() > 0) {
            Float durationTime = 0.0f;
            try {
                durationTime = Float.valueOf(duration);
            } catch (NumberFormatException e) {
                return String.valueOf(0);
            }
            return String.valueOf(durationTime.intValue());
        } else {
            return String.valueOf(0);
        }
    }

    private String generateImportRunName() {
        Calendar cal = new GregorianCalendar();
        cal.setTime(new java.sql.Date(System.currentTimeMillis()));
        return String.format(
                IAlmConsts.IMPORT_RUN_NAME_TEMPLATE,
                // java.util.Calendar represents months from 0 to 11 instead of from 1 to 12.
                // That's why it should be incremented.
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND));
    }

    /**
     * Project without versioning, proceed
     * No "udf|run on version" in Run, proceed
     * No "vc-version-number" value in Test, proceed
     * Run version should be no bigger than latest test version
     * Set latest test version to run if run version has no value.
     * @param test
     * @param run
     * @return
     */
    private boolean shouldProceedVersionForRun(Map<String, String> test, Map<String, String> run) {
        // Some test type doesn't have version support
        for (String noVersionTest : TestUploader.NO_VERSION_TESTS) {
            if (test.get("subtype-id").equals(noVersionTest)) {
                return true;
            }
        }

        boolean shouldProceed = true;
        boolean versioningEnabled = customizationService.isVersioningEnabled(
                CustomizationService.TEST_ENTITY_NAME);
        if (versioningEnabled && run.containsKey(RUN_VERSION_MAP_NAME)
                && StringUtils.isNotEmpty(test.get(VC_VERSION_NUMBER))) {

            if (StringUtils.isEmpty(run.get(RUN_VERSION_MAP_NAME))) {
                run.put(RUN_VERSION_MAP_NAME, test.get(VC_VERSION_NUMBER));
                logger.info("Run on version not found. Set it as the latest test version.");
            } else {
                try {
                    int testLatestVersion = Integer.parseInt(test.get(VC_VERSION_NUMBER));
                    int runVersion = Integer.parseInt(run.get(RUN_VERSION_MAP_NAME));
                    if (runVersion > testLatestVersion) {
                        logger.error("Run version larger than test latest version.");
                        shouldProceed = false;
                    }
                    if (runVersion < 1) {
                        run.put(RUN_VERSION_MAP_NAME, "1");
                        logger.info("Run on version is " + runVersion + ". Minimum version should be 1. Set it to 1.");
                    }
                } catch (NumberFormatException e) {
                    logger.error("Version number illegal. " + e.getMessage());
                    shouldProceed = false;
                }
            }
        }
        return shouldProceed;
    }
}
