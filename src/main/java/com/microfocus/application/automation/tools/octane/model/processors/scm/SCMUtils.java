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
