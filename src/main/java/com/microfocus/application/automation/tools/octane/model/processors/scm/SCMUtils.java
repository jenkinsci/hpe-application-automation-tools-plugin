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

package com.microfocus.application.automation.tools.octane.model.processors.scm;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.FilePath;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.SCM;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SCMUtils {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(SCMUtils.class);

    private static final String SCM_DATA_FILE = "scmdata.json";
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    private SCMUtils() {
        //code climate : Add a private constructor to hide the implicit public one
    }

    public static SCMData extractSCMData(Run run, SCM scm, SCMProcessor scmProcessor) {
        SCMData result = null;
        if (run.getParent() instanceof MatrixConfiguration || run instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) run;
            if (!build.getChangeSet().isEmptySet()) {
                result = scmProcessor.getSCMData(build, scm);
            }
        } else if (run instanceof WorkflowRun) {
            WorkflowRun wRun = (WorkflowRun) run;
            if (!wRun.getChangeSets().isEmpty()) {
                result = scmProcessor.getSCMData(wRun, scm);
            }
        }
        return result;
    }

    public static void persistSCMData(Run run, String jobCiId, String buildCiId, SCMData scmData) throws IOException, InterruptedException {
        FilePath resultFile = new FilePath(run.getRootDir()).child(SCM_DATA_FILE);
        List<SCMData> scmDataList = new ArrayList<>();
        scmDataList.add(scmData);
        String scmDataContent = dtoFactory.dtoCollectionToJson(scmDataList);

        try {
            resultFile.write(scmDataContent, "UTF-8");
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to persist SCMData for jobCiId " + jobCiId + " buildCiId " + buildCiId, e);
            throw e;
        }
    }

    public static InputStream getSCMData(Run run) throws IOException, InterruptedException {
        FilePath resultFile = new FilePath(run.getRootDir()).child(SCM_DATA_FILE);
        return resultFile.read();
    }

}
