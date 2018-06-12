package com.hpe.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Run Listener that handles basic CI item events and dispatches notifications to the Octane server
 * User: shitritn
 * Date: 12/06/18
 * Time: 09:33
 */

@Extension
public class ItemListenerImpl extends ItemListener {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Override
    public void onDeleted(Item item) {
        CIEvent event;

        if (item.getParent() != null && item.getParent().getClass().getName().equalsIgnoreCase(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {

            event = dtoFactory.newDTO(CIEvent.class)
                    .setEventType(CIEventType.DELETED)
                    .setProject(JobProcessorFactory.getFlowProcessor((WorkflowJob) item).getTranslateJobName());

            OctaneSDK.getInstance().getEventsService().publishEvent(event);
        }
    }
}
