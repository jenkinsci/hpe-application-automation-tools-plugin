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

package com.hpe.application.automation.tools.nv.plugin;

import com.hpe.nv.api.*;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.tasks.BuildStep;
import com.hpe.application.automation.tools.nv.common.NvTestUtils;
import com.hpe.application.automation.tools.nv.common.NvValidatorUtils;
import com.hpe.application.automation.tools.nv.model.*;
import com.hpe.application.automation.tools.nv.plugin.results.NvJUnitResultsHandler;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

public class NvEmulationInvoker {
    private static final String SCENARIO_PREFIX = "Network Scenario ";
    private static final String DEFAULT_FLOW = "Flow_1";
    private static final String DEFAULT_TRANSACTION = "Transaction";

    private final NvModel nvModel;
    private AbstractBuild<?, ?> build;
    private Launcher launcher;
    private BuildListener listener;

    private List<String> possibleIps;
    private NvNetworkProfile currentProfile;
    private NvJUnitResultsHandler jUnitHandler = null;

    public NvEmulationInvoker(NvModel nvModel, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        this.nvModel = nvModel;
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public boolean invoke() throws InterruptedException, IOException {
        List<NvNetworkProfile> profiles = nvModel.getProfiles();
        if (null == profiles || profiles.size() == 0) {
            return invokeBuildSteps(build, launcher, listener);
        } else {
            Map<String, Float> thresholdsMap = readThresholds();
            if (null == thresholdsMap) {
                listener.getLogger().println("Thresholds file is not valid. Tests will not be affected by thresholds. Check that the file exists and is readable. Check file contents correctness.");
            }

            if (null != nvModel.getReportFiles() && !nvModel.getReportFiles().isEmpty()) {
                jUnitHandler = new NvJUnitResultsHandler(thresholdsMap, nvModel.getReportFiles());
            } else {
                listener.getLogger().println("JUnit test xmls pattern is null or empty. Thresholds will not be imposed on tests.");
            }

            possibleIps = getPossibleIpAddresses();
            initProxy();

            Iterator<NvNetworkProfile> profilesIter = profiles.iterator();
            NvContext context = new NvContext();
            NvDataHolder.getInstance().put(NvTestUtils.getBuildKey(build), context);

            Test test = initAndStartTest(profilesIter);
            context.setTest(test);

            Transaction transaction = initTransaction(test);
            context.setTransaction(transaction);

            context.increaseRun();

            return invokePerProfile(context, profilesIter);
        }
    }

    private Map<String, Float> readThresholds() throws IOException {
        if (null == nvModel.getThresholdsFile() || nvModel.getThresholdsFile().isEmpty() || !NvValidatorUtils.validateFile(nvModel.getThresholdsFile())) {
            return null;
        }

        return NvValidatorUtils.readThresholdsFile(nvModel.getThresholdsFile());
    }

    private boolean invokePerProfile(NvContext context, Iterator<NvNetworkProfile> profilersIter) throws IOException, InterruptedException {
        if (context.getRun() > 1) {
            currentProfile = profilersIter.next();
            listener.getLogger().print(currentProfile);
            Flow flow = createFlow(currentProfile);
            List<Flow> flows = new ArrayList<>();
            flows.add(flow);

            try {
                context.getTest().realTimeUpdate(SCENARIO_PREFIX + currentProfile.getProfileName(), null, flows);
            } catch (NVExceptions.IllegalActionException | NVExceptions.ServerErrorException e) {
                throw new IOException("Failed to update network profile for profile: " + currentProfile.getProfileName() + ". Error: " + e.getMessage(), e);
            }
            listener.getLogger().println("Successfully updated network profile for profile: " + currentProfile.getProfileName());
        }

        try {
            context.getTransaction().start();
        } catch (NVExceptions.ServerErrorException | NVExceptions.IllegalActionException e) {
            throw new IOException("Failed to start transaction. Error: " + e.getMessage(), e);
        }
        listener.getLogger().println("Successfully started transaction.");

        invokeBuildSteps(build, launcher, listener);

        // if user steps fail, do not continue
//        if (!result) {
//            return false;
//        } else {
        if (profilersIter.hasNext()) { // recursive call to run again with next profile
            try {
                context.getTransaction().stop();
            } catch (NVExceptions.ServerErrorException e) {
                throw new IOException("Failed to stop transaction. Error: " + e.getMessage(), e);
            }

            listener.getLogger().println("Successfully stopped transaction.");

            context.increaseRun();

            handleTestResults(false);

            return invokePerProfile(context, profilersIter);
        } else { // last profile, finish
            NvTestUtils.stopTestEmulation(build, listener);

            handleTestResults(true);

            return true;
        }
//        }
    }

    private void handleTestResults(boolean finalize) throws IOException, InterruptedException {
        if (jUnitHandler != null) {
            jUnitHandler.handle(build, launcher, listener, currentProfile);
            if (finalize) {
                jUnitHandler.finalizeResults(build);
            }
        }
    }

    private Transaction initTransaction(Test test) throws IOException {
        try {
            test.connectToTransactionManager();
        } catch (NVExceptions.ServerErrorException e) {
            throw new IOException("Failed to connect to transaction manager. Error: " + e.getMessage(), e);
        }
        listener.getLogger().println("Successfully connected to transaction manager.");

        Transaction transaction = new Transaction(DEFAULT_TRANSACTION);
        try {
            transaction.addToTest(test);
        } catch (NVExceptions.ServerErrorException e) {
            throw new IOException("Failed to add transaction. Error: " + e.getMessage(), e);
        }
        listener.getLogger().println("Successfully added transaction.");

        return transaction;
    }

    private void initProxy() throws IOException, InterruptedException {
        if (nvModel.isUseProxy()) {
            if(nvModel.getNvServer().getProxyPort().isEmpty()) {
                throw new ConnectException("Proxy port was not configured for the selected NV Test Manager");
            }
            VariableInjectionAction action = new VariableInjectionAction(nvModel.getEnvVariable(),
                    nvModel.getNvServer().getServerIp() + ":" + nvModel.getNvServer().getProxyPort());
            build.addAction(action);
        }
    }

    private Test initAndStartTest(Iterator<NvNetworkProfile> profilersIter) throws IOException {
        TestManager tm = new TestManager(nvModel.getNvServer().getServerIp(), Integer.parseInt(nvModel.getNvServer().getNvPort()), nvModel.getNvServer().getUsername(), nvModel.getNvServer().getPassword());
        try {
            tm.init();
        } catch (NVExceptions.MissingPropertyException | NVExceptions.ServerErrorException e) {
            throw new IOException("Failed to start Network Virtualization emulation on host. Error: " + e.getMessage(), e);
        }

        listener.getLogger().println("Created Network Virtualization emulation.");

        currentProfile = profilersIter.next();
        listener.getLogger().print(currentProfile);

        String testId = NvTestUtils.getNvTestId(build);
        Test test;
        try {
            test = new Test(tm, testId, SCENARIO_PREFIX + currentProfile.getProfileName());
            test.setTestMode(Test.Mode.CUSTOM);
            if (nvModel.isUseProxy()) {
                test.setUseNVProxy(true);
            }
        } catch (NVExceptions.MissingPropertyException e) {
            throw new IOException("Failed to create Network Virtualiation test", e);
        }
        listener.getLogger().println("Created Network Virtualization test.");

        // handle first profile
        try {
            Flow flow = createFlow(currentProfile);
            test.addFlow(flow);
        } catch (NVExceptions.NotSupportedException | NVExceptions.MissingPropertyException e) {
            throw new IOException("Failed to add Network Virtualiation flow to test.", e);
        }

        try {
            test.start();
        } catch (NVExceptions.IllegalActionException | NVExceptions.ServerErrorException | NVExceptions.NotSupportedException e) {
            throw new IOException("Failed to start Network Virtualization test. Error: " + e.getMessage(), e);
        }

        listener.getLogger().println("Network Virtualization test was started successfully.");
        return test;
    }

    private Flow createFlow(NvNetworkProfile nvProfile) throws IOException {
        try {
            double bandwidthIn = BandwidthEnum.valueOf(nvProfile.getBandwidthIn()).getValue();
            double bandwidthOut = BandwidthEnum.valueOf(nvProfile.getBandwidthOut()).getValue();
            Flow flow = new Flow(DEFAULT_FLOW, Double.parseDouble(nvProfile.getLatency()), Double.parseDouble(nvProfile.getPacket()), bandwidthIn, bandwidthOut);

            // add all IPs to destinations include
            flow.includeDestIPRange(new IPRange(null, null, 0, IPRange.Protocol.ALL.getId()));

            // add user defined IPs
            addIncludeClientIPs(flow);
            addCustomExcludeServerIPs(flow);

            // add NV server as exclude
            if (nvModel.isUseProxy()) {
                flow.excludeDestIPRange(new IPRange(nvModel.getNvServer().getServerIp(), nvModel.getNvServer().getServerIp(), 0, IPRange.Protocol.ALL.getId()));
            }

            return flow;
        } catch (NVExceptions.MissingPropertyException | NVExceptions.IllegalArgumentException | NVExceptions.NotSupportedException e) {
            throw new IOException("Failed to create Network Virtualiation flow.", e);
        }
    }

    private void addIncludeClientIPs(Flow flow) throws NVExceptions.NotSupportedException, NVExceptions.IllegalArgumentException {
        // add possible ips to the list (host network interfaces)
        Set<String> ips = new HashSet<>(possibleIps);
        // add included ips provided by the user
        boolean invalidIpsFound = false;
        if (null != nvModel.getIncludeClientIPs() && !nvModel.getIncludeClientIPs().isEmpty()) {
            String[] includedIps = nvModel.getIncludeClientIPs().split(";");
            for (String ip : includedIps) {
                if (!ip.isEmpty()) {
                    if (isIpValid(ip)) {
                        ips.add(ip);
                    } else {
                        invalidIpsFound = true;
                    }
                }
            }

            if (invalidIpsFound) {
                listener.getLogger().println("Invalid IPs were found in \"Include Client IPs\" section. It might affect network emulation");
            }
        }

        String[] ranges;
        for (String includedIp : ips) {
            if (!includedIp.isEmpty()) {
                if (includedIp.contains("-")) { // handle IP range
                    ranges = includedIp.split("-");
                    flow.includeSourceIPRange(new IPRange(ranges[0], ranges[1], 0, IPRange.Protocol.ALL.getId()));
                    flow.excludeDestIPRange(new IPRange(ranges[0], ranges[1], 0, IPRange.Protocol.ALL.getId()));
                } else {
                    flow.includeSourceIPRange(new IPRange(includedIp, includedIp, 0, IPRange.Protocol.ALL.getId()));
                    flow.excludeDestIPRange(new IPRange(includedIp, includedIp, 0, IPRange.Protocol.ALL.getId()));
                }
            }
        }
    }

    private boolean isIpValid(String ip) {
        if (ip.contains("-")) {
            String[] ranges = ip.split("-");
            if (!NvValidatorUtils.isValidHostIp(ranges[0]) || !NvValidatorUtils.isValidHostIp(ranges[1])) {
                listener.getLogger().println("IP range: " + ip + " contains an invalid IPv4 address");
                return false;
            }
        } else {
            if (!NvValidatorUtils.isValidHostIp(ip)) {
                listener.getLogger().println("IP: " + ip + " is an invalid IPv4 address");
                return false;
            }
        }
        return true;
    }

    private void addCustomExcludeServerIPs(Flow flow) throws NVExceptions.NotSupportedException, NVExceptions.IllegalArgumentException {
        if (null != nvModel.getExcludeServerIPs() && !nvModel.getExcludeServerIPs().isEmpty()) {
            String[] excludedIps = nvModel.getExcludeServerIPs().split(";");
            String[] ranges;
            boolean invalidIpsFound = false;
            for (String excludedIp : excludedIps) {
                if (!excludedIp.isEmpty()) {
                    if (isIpValid(excludedIp)) {
                        if (excludedIp.contains("-")) { // handle IP range
                            ranges = excludedIp.split("-");
                            flow.excludeDestIPRange(new IPRange(ranges[0], ranges[1], 0, IPRange.Protocol.ALL.getId()));
                        } else {
                            flow.excludeDestIPRange(new IPRange(excludedIp, excludedIp, 0, IPRange.Protocol.ALL.getId()));
                        }
                    } else {
                        invalidIpsFound = true;
                    }
                }
            }

            if (invalidIpsFound) {
                listener.getLogger().println("Invalid IPs were found in \"Exclude Server IPs\" section");
            }
        }
    }

    private List<String> getPossibleIpAddresses() throws IOException, InterruptedException {
        // Get a "channel" to the build machine and run the task there
        return launcher.getChannel().call(new HostIpRetrieverCallable());
    }

    private boolean invokeBuildSteps(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        if (nvModel.getSteps() == null) {
            return true;
        }
        boolean result = true;
        for (BuildStep step : nvModel.getSteps()) {
            result = step.perform(build, launcher, listener);
//            if (!result) {
//                break;
//            }
        }

        return result;
    }

    // Extract the possible IPv4 addresses from the host where the build runs
    public static class HostIpRetrieverCallable implements Callable<List<String>, IOException> {
        private static final long serialVersionUID = 3959091053778932297L;

        @Override
        public List<String> call() throws IOException {
            ArrayList<String> result = new ArrayList<>();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress inetAddress;
            while (networkInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        result.add(inetAddress.getHostAddress());
                    }
                }
            }
            return result;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        }
    }
}
