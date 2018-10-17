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

package com.microfocus.application.automation.tools.srf.results;

import com.microfocus.application.automation.tools.srf.model.SrfException;
import com.microfocus.application.automation.tools.srf.model.SrfScriptRunModel;
import com.microfocus.application.automation.tools.srf.utilities.SrfStepsHtmlUtil;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

public class SrfResultFileWriter {
      private static final Logger systemLogger = Logger.getLogger(SrfResultFileWriter.class.getName());

      public static void writeJsonReport(String dirPath, String report) throws IOException {
            FileOutputStream fs = null;
            try {
                  String path = dirPath.concat("/report.json");
                  File jsonReportFile = new File(path);
                  Boolean fileCreated = jsonReportFile.createNewFile();

                  if (!fileCreated)
                        throw new IOException(String.format("Failed to create file: %s", path));

                  fs = new FileOutputStream(jsonReportFile);
                  fs.write(report.getBytes());
            } catch (IOException e) {
                  e.printStackTrace();
                  throw e;
            } finally {
                  cleanUp(fs);
            }

      }

      public static void writeOctaneResultsUrlFile(JSONArray tests, String dirPath, String tenant, String srfAddress) throws IOException, ParserConfigurationException {
            FileOutputStream fs = null;
            try {
                  String path = dirPath.concat("/srf-test-result-urls");
                  File srfTestResultUrls = new File(path);
                  Boolean fileCreated = srfTestResultUrls.createNewFile();
                  fs = new FileOutputStream(srfTestResultUrls);

                  if (!fileCreated)
                        throw new IOException(String.format("Failed to create file: %s", path));

                  for (Object testObject : tests) {
                        JSONObject test = (JSONObject) testObject;
                        String testRunYac = test.getString("yac");
                        String srfTestRunResultUrl = String.format("%s;%s/results/%s/details?TENANTID=%s\n", testRunYac, srfAddress, testRunYac, tenant);
                        fs.write(srfTestRunResultUrl.getBytes());
                  }

            } catch (IOException e) {
                  e.printStackTrace();
                  throw e;
            } finally {
                  cleanUp(fs);
            }
      }


      public static void writeXmlReport(final AbstractBuild<?, ?> build, JSONArray report, String tenant) throws IOException, ParserConfigurationException {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element root = doc.createElement("testsuites");
            try {
                  root.setAttribute("tenant", tenant);
                  int testsCnt = report.size();
                  //          root.setAttribute("tests", String.format("%1d", testsCnt));
                  int testRunErrors = 0;
                  int testRunCancellations = 0;
                  int successfulTestRun = 0;

                  for (int i = 0; i < testsCnt; i++) {

                        JSONObject test = (JSONObject) (report.get(i));
                        String status = test.getString("status");
                        if (status == null)
                              status = "errored";

                        switch (status) {
                              case "success":
                              case "completed":
                                    successfulTestRun++;
                                    break;
                              case "cancelled":
                                    testRunCancellations++;
                                    break;
                              default:
                                    testRunErrors++;
                                    break;
                        }


                        if (successfulTestRun == testsCnt) {
                              build.setResult(Result.SUCCESS);
                        } else if (testRunErrors > 0) {
                              build.setResult(Result.FAILURE);
                        } else if (testRunCancellations > 0) {
                              if (testRunCancellations == testsCnt)
                                    build.setResult(Result.ABORTED);
                              else
                                    build.setResult(Result.UNSTABLE);
                        }

                        Element testSuite = createTestSuite(test, doc);
                        root.appendChild(testSuite);
                  }
            }
            catch (Exception e){
                  systemLogger.severe(String.format("ERROR: %s", e.getMessage()));
            }

            doc.appendChild(root);
            String xmlReport = getStringFromDocument(doc);

            if (xmlReport == null) {
                  throw new IOException("Cannot convert xml to string");
            }

            String name = String.format("report%1d.xml", build.number); //build.getWorkspace().getParent().child("builds").child(name)
            File xmlReportFile = new File(build.getWorkspace().child(name).toString());
            Boolean fileCreated = xmlReportFile.createNewFile();

            if (!fileCreated)
                  throw new IOException(String.format("Failed to create file: %s", name));

            FileOutputStream fs = null;
            try {
                  fs = new FileOutputStream(xmlReportFile);
                  fs.write(xmlReport.getBytes());
            } catch (IOException e) {
                  systemLogger.severe(e.toString());
                  throw e;
            } finally {
                  cleanUp(fs);
            }
      }

      private static Element createTestSuite(JSONObject test, Document doc) throws SrfException {
            Element testSuite = doc.createElement("testsuite");
            String timestamp = test.getString("start");
            String testDuration = test.getString("durationMs");
            if(testDuration == null || testDuration.length() == 0)
                  testDuration = "0";
            int duration_i = Integer .parseInt(testDuration, 10)/1000;

            int errorsTestSute = 0;
            int failuresTestSute = 0;

            testSuite.setAttribute("time", String.format("%1d.0",duration_i ));
            String testRunYac = test.getString("yac");
            testSuite.setAttribute("yac", testRunYac);
            String name = test.getString("name");
            String uniqueName = String.format("%s_%s", name, testRunYac);
            testSuite.setAttribute("id", uniqueName);
            testSuite.setAttribute("name", SrfScriptRunModel.normalizeName(uniqueName));

            JSONArray scriptRuns = (JSONArray) (test.get("scriptRuns"));
            int scriptCnt = scriptRuns.size();
            testSuite.setAttribute("timestamp", timestamp);
            testSuite.setAttribute("tests", String.format("%1d", scriptCnt));
            //root.appendChild(testSuite);
            for (int j = 0; j < scriptCnt; j++) {
                  JSONObject scriptRun = scriptRuns.getJSONObject(j);
                  String status = scriptRun.getString("status");
                  if((status != null) && (status.compareTo("success") != 0)) {
                        if ("failed".compareTo(status) == 0) {
                              failuresTestSute++;
                        } else
                              errorsTestSute++;
                  }

                  Element testCase = createTestCase(scriptRun, doc);
                  testSuite.appendChild(testCase);

            }

            testSuite.setAttribute("errors", String.format("%1d", errorsTestSute));
            testSuite.setAttribute("failures", String.format("%1d", failuresTestSute));

            return testSuite;
      }

      private static Element createTestCase(JSONObject scriptRun, Document doc) throws SrfException {

            JSONObject assetInfo = scriptRun.getJSONObject("assetInfo");
            String scriptName = assetInfo.getString("name");
            Element testCase = doc.createElement("testcase");

            String scriptStatus = scriptRun.getString("status");
            if(scriptStatus.compareTo("failed") == 0 || scriptStatus.compareTo("errored") == 0){
                  Element failure = doc.createElement("failure");
                  testCase.appendChild(failure);
            }
            Element script = doc.createElement("system-out");

            JSONArray steps = scriptRun.getJSONArray("scriptSteps");
            String sdk = assetInfo.getString("sdk");
            String stepsHtml = SrfStepsHtmlUtil.getSrfStepsHtml(sdk, steps);
            script.setTextContent(stepsHtml);

            testCase.appendChild(script);
            //     testCase.setAttribute("classname", scriptName);
            testCase.setAttribute("name", String.format("%s_%s", scriptName, scriptRun.getString("yac")));
            String duration =scriptRun.getString("durationMs");
            if(duration == null)
                  duration = "N/A";
            int duration_i = Integer.parseInt(duration, 10)/1000;
            testCase.setAttribute("time", String.format("%1d.0",duration_i ));

            String status = scriptRun.getString("status");
            if(status != null && status.compareTo("success") != 0 && scriptRun.containsKey("errors")){

                  JSONArray errorsAr = new JSONArray();
                  Object errors = "";
                  try {
                        errorsAr = scriptRun.getJSONArray("errors");
                  }
                  catch (Exception e){
                        try {
                              errors = scriptRun.getJSONObject("errors");
                              errorsAr.add(errors);
                        }
                        catch (Exception e1) {
                              JSONObject jErr = new JSONObject();
                              jErr.put("error", errors.toString());
                              errorsAr.add(jErr);
                        }
                  }

                  int errCnt = errorsAr.size();
                  for (int k = 0; k < errCnt; k++) {
                        Element error = doc.createElement("error");
                        if(errorsAr.get(k) == JSONNull.getInstance())
                              continue;
                        error.setAttribute("message", ((JSONObject)(errorsAr.get(k))).getString("message"));
                        testCase.appendChild(error);
                  }
            }

            return testCase;
      }

      private static String getStringFromDocument(Document doc) {
            try
            {
                  DOMSource domSource = new DOMSource(doc);
                  StringWriter writer = new StringWriter();
                  StreamResult result = new StreamResult(writer);
                  TransformerFactory tf = TransformerFactory.newInstance();
                  Transformer transformer = tf.newTransformer();
                  transformer.transform(domSource, result);
                  return writer.toString();
            }
            catch(Exception ex)
            {
                  ex.printStackTrace();
                  return null;
            }

      }

      private static void cleanUp(FileOutputStream fs) {
            if (fs == null)
                  return;

            try {
                  fs.close();
            } catch (IOException e) {
                  e.printStackTrace();

            }
      }

}
