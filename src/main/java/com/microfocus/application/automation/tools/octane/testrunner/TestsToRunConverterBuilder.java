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

package com.microfocus.application.automation.tools.octane.testrunner;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.dto.general.MbtUnitParameter;
import com.hp.octane.integrations.executor.*;
import com.hp.octane.integrations.executor.converters.*;
import com.hp.octane.integrations.utils.SdkConstants;
import com.hp.octane.integrations.utils.SdkStringUtils;
import com.microfocus.application.automation.tools.AlmToolsUtils;
import com.microfocus.application.automation.tools.model.TestsFramework;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationValidator;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.*;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.microfocus.application.automation.tools.octane.executor.UftConstants.CODELESS_FOLDER_TEMPLATE;
import static com.microfocus.application.automation.tools.run.RunFromFileBuilder.HP_TOOLS_LAUNCHER_EXE;
import static java.util.stream.Collectors.groupingBy;

/**
 * Builder for available frameworks for converting
 */
public class TestsToRunConverterBuilder extends Builder implements SimpleBuildStep {

    private static final String DEFAULT_EXECUTING_DIRECTORY = "${workspace}";

    private static final String CHECKOUT_DIRECTORY_PARAMETER = "testsToRunCheckoutDirectory";

    private static final String CODELESS_SCRIPT_FILE = ".cl";

    private static final String TEST_FILE_EXT = ".json";

    private static final String MBT_JSON_FILE = "mbt.json";

    public static final String TESTS_TO_RUN_PARAMETER = "testsToRun";

    private TestsToRunConverterModel framework;

    public TestsToRunConverterBuilder(String framework) {
        this.framework = new TestsToRunConverterModel(framework, "");
    }

    @DataBoundConstructor
    public TestsToRunConverterBuilder(String framework, String format) {
        this.framework = new TestsToRunConverterModel(framework, format);
    }

    private static void printToConsole(TaskListener listener, String msg) {
        listener.getLogger().println(TestsToRunConverterBuilder.class.getSimpleName() + " : " + msg);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        try {
            ParametersAction parameterAction = build.getAction(ParametersAction.class);
            String rawTests = null;
            String executingDirectory = DEFAULT_EXECUTING_DIRECTORY;
            if (parameterAction != null) {
                ParameterValue suiteIdParameter = parameterAction.getParameter(SdkConstants.JobParameters.SUITE_ID_PARAMETER_NAME);
                if (suiteIdParameter != null) {
                    printToConsole(listener, SdkConstants.JobParameters.SUITE_ID_PARAMETER_NAME + " : " + suiteIdParameter.getValue());
                }
                ParameterValue suiteRunIdParameter = parameterAction.getParameter(SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME);
                if (suiteRunIdParameter != null) {
                    printToConsole(listener, SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME + " : " + suiteRunIdParameter.getValue());
                }

                ParameterValue executionIdParameter = parameterAction.getParameter(SdkConstants.JobParameters.EXECUTION_ID_PARAMETER_NAME);
                if (executionIdParameter != null) {
                    printToConsole(listener, SdkConstants.JobParameters.EXECUTION_ID_PARAMETER_NAME + " : " + executionIdParameter.getValue());
                }

                ParameterValue testsParameter = parameterAction.getParameter(TESTS_TO_RUN_PARAMETER);
                if (testsParameter != null && testsParameter.getValue() instanceof String) {
                    rawTests = (String) testsParameter.getValue();
                    printToConsole(listener, TESTS_TO_RUN_PARAMETER + " found with value : " + rawTests);
                }

                ParameterValue checkoutDirParameter = parameterAction.getParameter(CHECKOUT_DIRECTORY_PARAMETER);
                if (checkoutDirParameter != null) {
                    if (testsParameter.getValue() instanceof String && StringUtils.isNotEmpty((String) checkoutDirParameter.getValue())) {
                        executingDirectory = (String) checkoutDirParameter.getValue();//"%" + CHECKOUT_DIRECTORY_PARAMETER + "%";
                        printToConsole(listener, CHECKOUT_DIRECTORY_PARAMETER + " parameter found with value : " + executingDirectory);
                    } else {
                        printToConsole(listener, CHECKOUT_DIRECTORY_PARAMETER + " parameter found, but its value is empty or its type is not String. Using default value.");
                    }
                }
                printToConsole(listener, "checkout directory : " + executingDirectory);
            }
            if (StringUtils.isEmpty(rawTests)) {
                printToConsole(listener, TESTS_TO_RUN_PARAMETER + " is not found or has empty value. Skipping.");
                return;
            }

            if (framework == null || SdkStringUtils.isEmpty(getFramework())) {
                printToConsole(listener, "No frameworkModel is selected. Skipping.");
                return;
            }
            String frameworkName = getFramework();
            String frameworkFormat = getFormat();
            printToConsole(listener, "Selected framework = " + frameworkName);
            if (SdkStringUtils.isNotEmpty(frameworkFormat)) {
                printToConsole(listener, "Using format = " + frameworkFormat);
            }

            TestsToRunFramework testsToRunFramework = TestsToRunFramework.fromValue(frameworkName);
            boolean isMbt = rawTests.contains("mbtData");
            TestsToRunConverterResult convertResult;
            Map<String, String> globalParameters = getGlobalParameters(parameterAction);

            List<TestToRunData> testsData = TestsToRunConverter.parse(rawTests);
            TestsToRunConvertersFactory.createConverter(testsToRunFramework).enrichTestsData(testsData, globalParameters);

            if (isMbt) {
                //MBT needs to know real path to tests and not ${workspace}
                //MBT needs to run on slave  to extract function libraries from checked out files
                try {
                    EnvVars env = build.getEnvironment(listener);
                    executingDirectory = env.expand(executingDirectory);
                } catch (IOException | InterruptedException e) {
                    listener.error("Failed loading build environment " + e);
                }
                convertResult = filePath.act(new GetConvertResult(testsToRunFramework, frameworkFormat, testsData, executingDirectory, globalParameters));
            } else {
                convertResult = (new GetConvertResult(testsToRunFramework, frameworkFormat, testsData, executingDirectory, globalParameters)).invoke(null, null);
            }
            // process tests by type
            if (convertResult.getMbtTests() != null) {
                processTests(build, filePath, launcher, listener, convertResult);
            }

            printToConsole(listener, "Found #tests : " + convertResult.getTestsData().size());
            printToConsole(listener, "Set to parameter : " + convertResult.getTestsToRunConvertedParameterName() + " = " + convertResult.getConvertedTestsString());
            printToConsole(listener, "********************* Conversion is done *********************");
            if (JobProcessorFactory.WORKFLOW_RUN_NAME.equals(build.getClass().getName())) {
                List<ParameterValue> newParams = (parameterAction != null) ? new ArrayList<>(parameterAction.getAllParameters()) : new ArrayList<>();
                newParams.add(new StringParameterValue(convertResult.getTestsToRunConvertedParameterName(), convertResult.getConvertedTestsString()));
                ParametersAction newParametersAction = new ParametersAction(newParams);
                build.addOrReplaceAction(newParametersAction);
            } else {
                VariableInjectionAction via = new VariableInjectionAction(convertResult.getTestsToRunConvertedParameterName(), convertResult.getConvertedTestsString());
                build.addAction(via);
            }
        } catch (IllegalArgumentException e) {
            printToConsole(listener, "Failed to convert : " + e.getMessage());
            build.setResult(Result.FAILURE);

            return;
        }
    }

    private void processTests(Run<?, ?> build, FilePath filePath, Launcher launcher, TaskListener listener, TestsToRunConverterResult convertResult) throws IOException, InterruptedException {
        // group tests by type
        Map<TestingToolType, List<MbtTest>> mbtTestsByType = convertResult.getMbtTests().stream().collect(groupingBy(MbtTest::getType));

        // first handle uft tests if exist
        if (CollectionUtils.isNotEmpty(mbtTestsByType.get(TestingToolType.UFT))) {
            prepareUftTests((List<MbtUftTest>)(List<?>)mbtTestsByType.get(TestingToolType.UFT), build, filePath, launcher, listener);
        }

        // handle codeless tests
        if (CollectionUtils.isNotEmpty(mbtTestsByType.get(TestingToolType.CODELESS))) {
            prepareCodelessTests((List<MbtCodelessTest>)(List<?>)mbtTestsByType.get(TestingToolType.CODELESS), build, filePath);
        }
    }

    private Map<String, String> getGlobalParameters(ParametersAction parameterAction) {
        Map<String, String> map = new HashMap<>();
        Set<String> predefinedParams = new HashSet<>(Arrays.asList(
                SdkConstants.JobParameters.ADD_GLOBAL_PARAMETERS_TO_TESTS_PARAM,
                SdkConstants.JobParameters.SUITE_ID_PARAMETER_NAME,
                SdkConstants.JobParameters.SUITE_RUN_ID_PARAMETER_NAME,
                SdkConstants.JobParameters.OCTANE_SPACE_PARAMETER_NAME,
                SdkConstants.JobParameters.OCTANE_WORKSPACE_PARAMETER_NAME,
                SdkConstants.JobParameters.OCTANE_CONFIG_ID_PARAMETER_NAME,
                SdkConstants.JobParameters.OCTANE_URL_PARAMETER_NAME,
                SdkConstants.JobParameters.OCTANE_RUN_BY_USERNAME
        ));

        parameterAction.getAllParameters().stream()
                .filter(p -> predefinedParams.contains(p.getName()) || p.getName().toLowerCase(Locale.ROOT).contains("octane"))
                .forEach(param -> addParameterIfExist(map, parameterAction, param.getName()));

        return map;
    }

    private void addParameterIfExist(Map<String, String> map, ParametersAction parameterAction, String paramName) {
        ParameterValue param = parameterAction.getParameter(paramName);
        if (param != null) {
            map.put(param.getName(), param.getValue().toString());
        }
    }

    private void prepareUftTests(List<MbtUftTest> tests, @Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        build.getRootDir();
        Properties props = new Properties();
        props.setProperty("runType", "MBT");
        props.setProperty("resultsFilename", "must be here");

        EnvVars env = build.getEnvironment(listener);

        props.setProperty("parentFolder", workspace.getRemote() + "\\" + MfMBTConverter.MBT_PARENT_SUB_DIR);
        props.setProperty("repoFolder", workspace.getRemote());
        ParametersAction parameterAction = build.getAction(ParametersAction.class);
        ParameterValue checkoutDirParameter = parameterAction.getParameter(CHECKOUT_DIRECTORY_PARAMETER);
        if (checkoutDirParameter != null) {
            props.setProperty("parentFolder", env.expand((String) checkoutDirParameter.getValue()) + "\\" + MfMBTConverter.MBT_PARENT_SUB_DIR);
            props.setProperty("repoFolder", env.expand((String) checkoutDirParameter.getValue()));
        }

        int counter = 1;

        for (MbtUftTest mbtUftTest : tests) {
            props.setProperty("test" + counter, mbtUftTest.getName());
            props.setProperty("package" + counter, "_" + counter);
            props.setProperty("script" + counter, env.expand(mbtUftTest.getScript()));
            props.setProperty("unitIds" + counter, mbtUftTest.getUnitIds().stream().map(n -> n.toString()).collect(Collectors.joining(";")));
            props.setProperty("underlyingTests" + counter, env.expand((String.join(";", mbtUftTest.getUnderlyingTests()))));

            if (mbtUftTest.getEncodedIterations() != null && !mbtUftTest.getEncodedIterations().isEmpty()) {
                //Expects to receive params in CSV format, encoded base64, for example Y29sMSxjb2wyCjEsMwoyLDQK
                props.setProperty("datableParams" + counter, mbtUftTest.getEncodedIterations());
            }
            counter++;
        }

        //prepare time
        Date now = new Date();
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(now);

        // get properties serialized into a stream
        String strProps;
        try {
            strProps = AlmToolsUtils.getPropsAsString(props);
        } catch (IOException e) {
            build.setResult(Result.FAILURE);
            listener.error("Failed to store properties on agent machine: " + e);
            return;
        }
        String propsFileName = String.format("mbt_props%s.txt", time);
        FilePath fileProps = workspace.child(propsFileName);

        //HP Tool Launcher
        URL cmdExeUrl = Jenkins.get().pluginManager.uberClassLoader.getResource(HP_TOOLS_LAUNCHER_EXE);
        if (cmdExeUrl == null) {
            listener.fatalError(HP_TOOLS_LAUNCHER_EXE + " not found in resources");
            return;
        }
        FilePath cmdLineExe = workspace.child(HP_TOOLS_LAUNCHER_EXE);

        try {
            // create a file for the properties file, and save the properties
            if (!AlmToolsUtils.tryCreatePropsFile(listener, strProps, fileProps)) {
                build.setResult(Result.FAILURE);
                return;
            }
            printToConsole(listener, "MBT props file saved to " + fileProps.getRemote());

            // Copy the script to the project workspace
            if (!cmdLineExe.exists()) {
                cmdLineExe.copyFrom(cmdExeUrl);
                printToConsole(listener, "HPToolLauncher copied to " + cmdLineExe.getRemote());
            }

        } catch (IOException | InterruptedException e) {
            build.setResult(Result.FAILURE);
            listener.error("Copying executable files to executing node " + e);
        }

        try {
            // Run the HpToolsLauncher.exe
            AlmToolsUtils.runOnBuildEnv(build, launcher, listener, cmdLineExe, propsFileName, null);
            // Has the report been successfully generated?
        } catch (IOException ioe) {
            Util.displayIOException(ioe, listener);
            build.setResult(Result.FAILURE);
            listener.error("Failed running HpToolsLauncher " + ioe);
            return;
        }
    }

    private void prepareCodelessTests(List<MbtCodelessTest> tests, Run<?, ?> build, FilePath workspace) throws IOException, InterruptedException {
        FilePath parentFolder = workspace.child(String.format(CODELESS_FOLDER_TEMPLATE, build.getNumber()));
        parentFolder.mkdirs();

        // write the codeless script files
        writeUnitScriptFiles(tests, parentFolder);
        // write the mbt json file
        writeMbtJsonFile(tests, parentFolder);
    }

    private void writeUnitScriptFiles(List<MbtCodelessTest> tests, FilePath parentFolder) throws IOException, InterruptedException {
        for (MbtCodelessTest test : tests) {
            for (MbtCodelessUnit unit : test.getUnits()) {
                String fileName = unit.getUnitId() + CODELESS_SCRIPT_FILE;
                FilePath unitFile = parentFolder.child(fileName);
                unitFile.write(unit.getScript(), String.valueOf(StandardCharsets.UTF_8));
                unit.setPath(unitFile.getRemote());
            }
        }
    }

    private void writeMbtJsonFile(List<MbtCodelessTest> tests, FilePath parentFolder) throws IOException, InterruptedException {
        JSONArray mbtJsonArr = new JSONArray();

        int counter = 1;
        for (MbtCodelessTest test : tests) {
            JSONObject codelessTestJson = new JSONObject();

            // add test section to json
            JSONObject testJsonObj = new JSONObject();
            testJsonObj.put("reportPath", parentFolder.getRemote());
            codelessTestJson.put("test", testJsonObj);

            // add units to json
            JSONArray unitsJsonArr = generateUnitsJson(test.getUnits());
            codelessTestJson.put("units", unitsJsonArr);

            // add iterations data to json
            JSONObject dataJsonObj = generateDataJson(test);
            codelessTestJson.put("data", dataJsonObj);

            // write codeless test file
            // use counter since test name is not unique
            String fileName = counter + "_" + test.getName() + TEST_FILE_EXT;
            FilePath testFile = parentFolder.child(fileName);
            testFile.write(codelessTestJson.toString(), String.valueOf(StandardCharsets.UTF_8));

            JSONObject mbtTestJsonObj = new JSONObject();
            mbtTestJsonObj.put("counter", counter);
            mbtTestJsonObj.put("testName", test.getName());
            mbtTestJsonObj.put("path", testFile.getRemote());

            mbtJsonArr.add(mbtTestJsonObj);
            counter++;
        }

        // write mbt json file
        FilePath mbtJsonFile = parentFolder.child(MBT_JSON_FILE);
        mbtJsonFile.write(mbtJsonArr.toString(), String.valueOf(StandardCharsets.UTF_8));
    }

    private JSONArray generateUnitsJson(List<MbtCodelessUnit> codelessUnits) {
        JSONArray unitsJsonArr = new JSONArray();
        codelessUnits.forEach(codelessUnit -> {
            JSONObject unitJsonObj = new JSONObject();
            unitJsonObj.put("unitId", codelessUnit.getUnitId());
            unitJsonObj.put("name", codelessUnit.getName());
            unitJsonObj.put("order", codelessUnit.getOrder());
            unitJsonObj.put("path", codelessUnit.getPath());
            // add parameters section to unit
            JSONArray parametersJsonArr = new JSONArray();
            if (!CollectionUtils.isEmpty(codelessUnit.getParameters())) {
                codelessUnit.getParameters().forEach(parameter -> {
                    JSONObject parameterJsonObj = new JSONObject();
                    // take the parameter id and name from the unit parameter since the codeless scripts contain unit parameters
                    // data and not test parameters data
                    parameterJsonObj.put("id", parameter.getUnitParameterId());
                    parameterJsonObj.put("name", parameter.getUnitParameterName());
                    parameterJsonObj.put("type", parameter.getType());
                    parametersJsonArr.add(parameterJsonObj);
                });
            }
            unitJsonObj.put("parameters", parametersJsonArr);
            unitsJsonArr.add(unitJsonObj);
        });

        return unitsJsonArr;
    }

    private JSONObject generateDataJson(MbtCodelessTest test) {
        // prepare data
        // sort units by order
        List<MbtCodelessUnit> sortedUnits = test.getUnits().stream().sorted(Comparator.comparing(MbtCodelessUnit::getOrder)).collect(Collectors.toList());

        // build a sorted list of all input parameters
        List<MbtUnitParameter> sortedInputParameters = new ArrayList<>();
        sortedUnits.forEach(unit -> {
            List<MbtUnitParameter> sortedList = unit.getParameters().stream().filter(parameter -> parameter.getType().equalsIgnoreCase("input"))
                    .sorted(Comparator.comparing(MbtUnitParameter::getOrder))
                    .collect(Collectors.toList());
            sortedInputParameters.addAll(sortedList);
        });

        // build a map between an output parameter name and the actual parameter. it will be used when constructing the
        // data section of the test json to map between an output parameter in the mbt test to the unit parameter since
        // codeless uses unit parameters data and not test parameter data
        Map<String, MbtUnitParameter> outputParamNameToParameterMap = new HashMap<>();
        sortedUnits.forEach(unit -> unit.getParameters().stream()
                .filter(parameter -> parameter.getType().equalsIgnoreCase("output"))
                .forEach(parameter -> outputParamNameToParameterMap.put(parameter.getName(), parameter)));

        // build a map between a parameter name and the unit holding it. the key is the parameter id and the parameter name
        // since in order to support merged parameters
        Map<String, MbtCodelessUnit> paramNameToUnitMap = new HashMap<>();
        sortedUnits.forEach(unit -> unit.getParameters().forEach(parameter -> paramNameToUnitMap.put(createParameterKey(parameter), unit)));

        List<String> iterationParams = test.getMbtDataTable().getParameters();
        List<List<String>> iterations = test.getMbtDataTable().getIterations();

        // construct json
        JSONObject dataJsonObj = new JSONObject();
        JSONArray iterationsJsonArr = new JSONArray();

        // build iterations
        for (List<String> currentIteration : iterations) {
            JSONArray iterationJsonArr = new JSONArray();
            for (MbtUnitParameter currentParameter : sortedInputParameters) {
                JSONObject parameterJsonObj = generateParameterDataJson(currentParameter, currentIteration, iterationParams, paramNameToUnitMap, outputParamNameToParameterMap);
                iterationJsonArr.add(parameterJsonObj);
            }
            iterationsJsonArr.add(iterationJsonArr);
        }
        dataJsonObj.put("iterations", iterationsJsonArr);

        return dataJsonObj;
    }

    private String createParameterKey(MbtUnitParameter parameter) {
        return parameter.getParameterId() + "_" + parameter.getName();
    }

    private JSONObject generateParameterDataJson(MbtUnitParameter currentParameter, List<String> currentIteration, List<String> iterationParams, Map<String, MbtCodelessUnit> paramNameToUnitMap, Map<String, MbtUnitParameter> outputParamNameToParameterMap) {
        MbtCodelessUnit currentUnit = paramNameToUnitMap.get(createParameterKey(currentParameter));

        boolean isLinkedToOutput = StringUtils.isNotEmpty(currentParameter.getOutputParameter());

        JSONObject parameterJsonObj = new JSONObject();
        parameterJsonObj.put("unitOrder", currentUnit.getOrder());
        // take the parameter name from the unit parameter since the codeless scripts contain unit parameters data and not
        // test parameters data
        parameterJsonObj.put("paramName", currentParameter.getUnitParameterName());
        JSONObject valueJsonObj = new JSONObject();
        if (isLinkedToOutput) { // value should be taken from output parameter
            MbtUnitParameter outputParameter = outputParamNameToParameterMap.get(currentParameter.getOutputParameter());
            MbtCodelessUnit linkedParamUnit = paramNameToUnitMap.get(createParameterKey(outputParameter));
            valueJsonObj.put("type", "parameter");
            // set the order and not the unit id since a unit can be added more than once in an mbt test
            valueJsonObj.put("name", linkedParamUnit.getOrder() + ":" + outputParameter.getUnitParameterName());
        } else { // value should be taken from iteration both for regular parameter and merged parameter by the merged parameter name
            valueJsonObj.put("type", "literal");
            String value = currentIteration.get(iterationParams.indexOf(currentParameter.getName())); // get value by index in iteration parameters list
            valueJsonObj.put("value", value != null ? value : "");
        }
        parameterJsonObj.put("value", valueJsonObj);
        return parameterJsonObj;
    }

    /***
     * Used in UI
     * @return
     */
    public TestsToRunConverterModel getTestsToRunConverterModel() {
        return framework;
    }

    /***
     * Used in Pipeline
     * @return
     */
    public String getFramework() {
        return framework.getFramework().getName();
    }

    public String getFormat() {
        return framework.getFramework().getFormat();
    }

    public boolean getIsCustom() {
        return framework != null && TestsToRunFramework.Custom.value().equals(framework.getFramework().getName());
    }

    private static class GetConvertResult implements FilePath.FileCallable<TestsToRunConverterResult> {

        private TestsToRunFramework framework;

        private List<TestToRunData> testData;

        private String executingDirectory;

        private String format;

        private Map<String, String> globalParameters;

        public GetConvertResult(TestsToRunFramework framework, String format, List<TestToRunData> testData, String executingDirectory, Map<String, String> globalParameters) {
            this.framework = framework;
            this.testData = testData;
            this.format = format;
            this.executingDirectory = executingDirectory;
            this.globalParameters = globalParameters;
        }

        @Override
        public TestsToRunConverterResult invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
            return TestsToRunConvertersFactory.createConverter(framework)
                    .setFormat(format)
                    .convert(testData, executingDirectory, globalParameters);
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            //no need to check roles as this can be run on master and on slave
        }

    }

    @Symbol("convertTestsToRun")
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;//FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "ALM Octane testing framework converter";
        }

        public FormValidation doTestConvert(
                @QueryParameter("testsToRun") String rawTests,
                @QueryParameter("teststorunconverter.framework") String framework,
                @QueryParameter("teststorunconverter.format") String format) {

            try {

                if (StringUtils.isEmpty(rawTests)) {
                    throw new IllegalArgumentException("'Tests to run' parameter is missing");
                }

                if (StringUtils.isEmpty(framework)) {
                    throw new IllegalArgumentException("'Framework' parameter is missing");
                }

                TestsToRunFramework testsToRunFramework = TestsToRunFramework.fromValue(framework);
                if (TestsToRunFramework.Custom.equals(testsToRunFramework) && StringUtils.isEmpty(format)) {
                    throw new IllegalArgumentException("'Format' parameter is missing");
                }

                TestsToRunConverterResult convertResult = TestsToRunConvertersFactory.createConverter(testsToRunFramework)
                        .setFormat(format)
                        .convert(rawTests, TestsToRunConverterBuilder.DEFAULT_EXECUTING_DIRECTORY, null);
                String result = Util.escape(convertResult.getConvertedTestsString());
                return ConfigurationValidator.wrapWithFormValidation(true, "Conversion is successful : <div style=\"margin-top:20px\">" + result + "</div>");
            } catch (Exception e) {
                return ConfigurationValidator.wrapWithFormValidation(false, "Failed to convert : " + e.getMessage());
            }
        }

        /**
         * Gets report archive modes.
         *
         * @return the report archive modes
         */
        public List<TestsFramework> getFrameworks() {

            return TestsToRunConverterModel.Frameworks;
        }

    }

}
