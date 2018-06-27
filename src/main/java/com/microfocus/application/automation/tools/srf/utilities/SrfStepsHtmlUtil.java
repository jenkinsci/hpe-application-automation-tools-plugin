/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.srf.utilities;

import com.microfocus.application.automation.tools.srf.model.SrfException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.logging.Logger;

public class SrfStepsHtmlUtil {
      private static final String STEP_STATUS_HTML_PATTERN = "<p>%1d  %1s\t<b>%1s</b></p>";
      private static final String STEP_ROLE_HTML_PATTERN = "<p>%1d  %1s</p>";
      private static final Logger systemLogger = Logger.getLogger(SrfStepsHtmlUtil.class.getName());

      interface SrfUftConsts {
            HashMap<String, String> steps = new HashMap<String, String>() {{
                  put("Iteration-begin", "Iteration %s Begin");
                  put("Iteration-end", "Iteration %s End");
            }};
      }

      interface SrfLeanFtConsts {
            HashMap<String, String> steps = new HashMap<String, String>() {{
                  put("Suite-begin", "Suite");
                  put("Spec-begin", "Spec");
                  put("Spec-end", "spec-end");
            }};
      }

      public static String getSrfStepsHtml(String sdk, JSONArray steps) throws SrfException {
            if (sdk == null || sdk.isEmpty())
                  throw new SrfException("Received empty sdk");

            switch (sdk.toLowerCase()) {
                  case "uft": return SrfStepsHtmlUtil.getUftStepsHtml(steps);
                  case "leanft": return SrfStepsHtmlUtil.getLeanFtStepsHtml(steps);
                  default: return SrfStepsHtmlUtil.getDefaultStepsHtml(steps);
            }
      }

      private static String getDefaultStepsHtml(JSONArray steps){
            StringBuilder allSteps = new StringBuilder();
            for (int k = 0; k < steps.size(); k++) {
                  JSONObject step = steps.getJSONObject(k);
                  allSteps.append(String.format(STEP_STATUS_HTML_PATTERN, k + 1, step.getString("description"), step.getString("status")));
            }
            return allSteps.toString();
      }

      private static String getUftStepsHtml(JSONArray steps){
            StringBuilder allSteps = new StringBuilder();
            for (int k = 0; k < steps.size(); k++) {
                  JSONObject step = steps.getJSONObject(k);
                  String stepDescription = step.getString("description");
                  String stepStatus = step.getString("status");

                  if (stepDescription != null && !stepDescription.isEmpty()) {
                        allSteps.append(String.format(STEP_STATUS_HTML_PATTERN, k + 1, stepDescription, stepStatus));
                        continue;
                  }

                  JSONObject stepRole = step.getJSONObject("role");
                  String stepRoleType = stepRole.getString("type");
                  if (!SrfUftConsts.steps.containsKey(stepRoleType)){
                        allSteps.append(String.format(STEP_ROLE_HTML_PATTERN, k + 1, stepRoleType));
                        continue;
                  }

                  stepDescription = SrfUftConsts.steps.get(stepRoleType);
                  String iterationIndex = stepRole.getString("index");

                  allSteps.append(String.format(STEP_ROLE_HTML_PATTERN, k + 1, String.format(stepDescription, iterationIndex)));
            }

            return allSteps.toString();
      }

      private static String getLeanFtStepsHtml(JSONArray steps) {
            StringBuilder allSteps = new StringBuilder();
            int stepIndex = 1;
            for (int k = 0; k < steps.size(); k++) {
                  JSONObject step = steps.getJSONObject(k);

                  String stepDescription = step.getString("description");
                  if (stepDescription == null || stepDescription.isEmpty()) {
                        String scriptRunId = step.getString("scriptRunId");
                        systemLogger.warning(String.format("Received step with no description, step index: %d, script run id: %s", k, scriptRunId));
                  }

                  String stepStatus = null;
                  JSONObject stepRole = step.getJSONObject("role");
                  String stepRoleType = stepRole.getString("type");
                  if (SrfLeanFtConsts.steps.containsKey(stepRoleType)){
                        if ("Spec-end".equals(stepRoleType)) // We're not reporting this in SRF result page
                              continue;

                        stepRoleType = String.format("<b>%s</b>", SrfLeanFtConsts.steps.get(stepRoleType));
                        allSteps.append(String.format(STEP_ROLE_HTML_PATTERN, stepIndex, String.format("%s %s", stepRoleType, stepDescription)));
                  } else {
                        stepStatus = step.getString("status");
                        allSteps.append(String.format(STEP_STATUS_HTML_PATTERN, stepIndex, stepDescription, stepStatus));
                  }

                  stepIndex++;
            }

            return allSteps.toString();
      }

}
