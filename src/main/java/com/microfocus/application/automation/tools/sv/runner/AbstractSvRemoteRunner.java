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
 *  Copyright 2012-2025 Open Text.
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
package com.microfocus.application.automation.tools.sv.runner;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.sv.model.AbstractSvRunModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.sv.svconfigurator.build.ProjectBuilder;
import com.microfocus.sv.svconfigurator.core.IProject;
import com.microfocus.sv.svconfigurator.core.IService;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommandExecutorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommunicatorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.ProjectBuilderException;
import com.microfocus.sv.svconfigurator.core.impl.jaxb.atom.ServiceListAtom;
import com.microfocus.sv.svconfigurator.serverclient.ICommandExecutor;
import com.microfocus.sv.svconfigurator.serverclient.impl.CommandExecutorFactory;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

public abstract class AbstractSvRemoteRunner<T extends AbstractSvRunModel> extends MasterToSlaveCallable<String, Exception> {
    protected T model;
    protected FilePath workspace;
    protected TaskListener listener;
    protected SvServerSettingsModel server;

    public AbstractSvRemoteRunner(TaskListener listener, T model, FilePath workspace, SvServerSettingsModel server) {
        this.listener = listener;
        this.model = model;
        this.workspace = workspace;
        this.server = server;
    }

    protected List<ServiceInfo> getServiceList(boolean ignoreMissingServices, PrintStream logger, FilePath workspace) throws Exception {
        SvServiceSelectionModel s = getServiceSelection();
        ICommandExecutor exec = createCommandExecutor();

        ArrayList<ServiceInfo> res = new ArrayList<>();

        switch (s.getSelectionType()) {
            case SERVICE:
                addServiceIfDeployed(s.getService(), res, ignoreMissingServices, exec, logger);
                break;
            case PROJECT:
                IProject project = loadProject(workspace);
                for (IService svc : project.getServices()) {
                    addServiceIfDeployed(svc.getId(), res, ignoreMissingServices, exec, logger);
                }
                break;
            case ALL_DEPLOYED:
                for (ServiceListAtom.ServiceEntry entry : exec.getServiceList(null).getEntries()) {
                    res.add(new ServiceInfo(entry.getId(), entry.getTitle()));
                }
                break;
            case DEPLOY:
                break;
            default:
                throw new IllegalArgumentException();
        }
        return res;
    }

    protected IProject loadProject(FilePath workspace) throws ProjectBuilderException {
        SvServiceSelectionModel s = getServiceSelection();
        FilePath projectPath = workspace.child(s.getProjectPath());
        return new ProjectBuilder().buildProject(new File(projectPath.getRemote()), s.getProjectPassword());
    }

    public SvServiceSelectionModel getServiceSelection() {
        return model.getServiceSelection();
    }

    private void addServiceIfDeployed(String service, ArrayList<ServiceInfo> results, boolean ignoreMissingServices,
                                  ICommandExecutor exec, PrintStream logger) throws CommunicatorException, CommandExecutorException {
        try {
            IService svc = exec.findService(service, null);
            results.add(new ServiceInfo(svc.getId(), svc.getName()));
        } catch (CommandExecutorException e) {
            if (!ignoreMissingServices) {
                throw e;
            }
            logger.printf("Service '%s' is not deployed, ignoring%n", service);
        }
    }

    protected ICommandExecutor createCommandExecutor() throws Exception {
        return new CommandExecutorFactory()
                .createCommandExecutor(server.getUrlObject(), server.isTrustEveryone(), server.getCredentials());
    }
}
