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

package com.microfocus.application.automation.tools.octane.workflow;

import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;

import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.microfocus.application.automation.tools.octane.model.CIEventCausesFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.*;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.util.*;

/**
 * Jenkins create a new instance of this for each run of workflowRun (by using @Extension annotation)
 * 1. This listener send an event on stage start and stage finish.
 * 2. this listener save the details of the node (after the node finished to run) for the next flow of the test results report.
 *
 * User: gadiel
 * Date: 07/06/2016
 * Time: 17:21
 */
@Extension
public class WorkflowGraphListener implements GraphListener {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
//	private Stack<CIEvent> finishedEventsStack = new Stack<CIEvent>();
//    String lastDisplayName = "";
//    String lastId = "";
//    CIEventCause lastCIEventCause;

    public static FlowNodeContainer container;

    private FlowNode previousStage = null;

    @Override
    public void onNewHead(FlowNode flowNode) {
        if (flowNode instanceof StepEndNode) {
            //check - if this is stage end node - just save it to the next time
            if(isStageEndNode(flowNode)){
                sendFinishEventOnPreviousStage(((StepEndNode)flowNode).getStartNode(), flowNode);
            }
            else {
                sendFinishEventOnPreviousStage(previousStage, flowNode);
            }

            WorkspaceAction workspaceAction = ((StepEndNode) flowNode).getStartNode().getAction(WorkspaceAction.class);
            if (workspaceAction != null) {

                try {
                    WorkflowRun run = (WorkflowRun) flowNode.getExecution().getOwner().getExecutable();
                    WorkflowBuildAdapter adapter = new WorkflowBuildAdapter(run.getParent(), run, workspaceAction.getWorkspace());
                    FlowNodeContainer.addFlowNode(adapter);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (StageNodeExt.isStageNode(flowNode)) {
            //ints a stage  - it is also a finish of the previous stage - will send finish event
            sendFinishEventOnPreviousStage(previousStage, flowNode);
            sendStartEventOnCurrentStage(flowNode);

//					.setEstimatedDuration(r.getEstimatedDuration())
//					.setCauses(CIEventCausesFactory.processCauses(extractCauses(r)));
//			// its a stage node - it is also a finish of previous stage
//			finishAllStartedEvents();
//			// starting an event:
//			dispatchStartEvent(flowNode);
//			// after the dispatching of the event, we create the finish event and add it to the queue:
//			createFinishEvent(flowNode);
        }
//		else if (flowNode instanceof StepAtomNode)       // its outer Job
//		{
//			if(isItStageStep(flowNode)){
//				// its a stage node - it is also a finish of previous stage
//				finishAllStartedEvents();
//				// starting an event:
//				dispatchStartEvent(flowNode);
//				// after the dispatching of the event, we create the finish event and add it to the queue:
//				createFinishEvent(flowNode);
//			}else if( isItBuildStep(flowNode))
//				popNewJobEvent(flowNode);       // popping a new event (we want to draw it on NGA)
//		}
    }

    private boolean isStageEndNode(FlowNode flowNode) {
        return StageNodeExt.isStageNode(((StepEndNode)flowNode).getStartNode());
    }

    private void sendFinishEventOnPreviousStage(FlowNode previousStageNode, FlowNode flowNode) {
        if (previousStageNode == null) {
            return;
        }

        try {
            /*Workaround:
            Solution for calculating the duration of the stage: There is currently no end event of the stage, and cannot know the duration
            of the total duration of the stage. Therefore, this is calculated using the start time of the previous stage and the current stage.*/

            Long startTimeCurrent = TimingAction.getStartTime(flowNode);
            Long startTimePrevious = TimingAction.getStartTime(previousStageNode);
            Long duration = startTimeCurrent -startTimePrevious;
            WorkflowRun parentRun = (WorkflowRun) flowNode.getExecution().getOwner().getExecutable();

            CIEvent event = dtoFactory.newDTO(CIEvent.class)
                    .setEventType(CIEventType.FINISHED)
                    .setPhaseType(PhaseType.POST)
                    .setProject(previousStageNode.getDisplayName())
                    .setStartTime(startTimePrevious)
                    .setNumber(String.valueOf(parentRun.getNumber()))
                    .setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
                    .setCauses(getCauses(parentRun))
                    .setDuration(duration)
                    .setEstimatedDuration(duration)
                    .setResult(getStatus(flowNode));

            OctaneSDK.getInstance().getEventsService().publishEvent(event);
            this.previousStage = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CIBuildResult getStatus(FlowNode node){
        if(node.getActions() !=null) {
            for (Action action : node.getActions()) {
                if (action.getClass().equals(ErrorAction.class))
                    return CIBuildResult.FAILURE;
            }
        }
        return  CIBuildResult.SUCCESS;
    }
    private List<CIEventCause> getCauses(WorkflowRun parentRun) {
        CIEventCause causes = dtoFactory.newDTO(CIEventCause.class)
                .setType(CIEventCauseType.UPSTREAM)
                .setProject(BuildHandlerUtils.getJobCiId(parentRun))
                .setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
                .setCauses(CIEventCausesFactory.processCauses((parentRun.getCauses())));

        List<CIEventCause> causeList = new ArrayList<>();
        causeList.add(causes);

        return causeList;
    }

    private void sendStartEventOnCurrentStage(FlowNode flowNode) {
        CIEvent event;
        try {
            //   StageNodeExt stageNode = null;
            //   StageNodeExt.create(flowNode);
            WorkflowRun parentRun = (WorkflowRun) flowNode.getExecution().getOwner().getExecutable();

            event = dtoFactory.newDTO(CIEvent.class)
                    .setEventType(CIEventType.STARTED)
                    .setPhaseType(PhaseType.POST)
                    .setProject(flowNode.getDisplayName())//stageNode.getName())
                    .setStartTime(TimingAction.getStartTime(flowNode))//stageNode.getStartTimeMillis())
                    .setNumber(String.valueOf(parentRun.getNumber()))
                    .setBuildCiId(BuildHandlerUtils.getBuildCiId(parentRun))
                    .setCauses(getCauses(parentRun));

            OctaneSDK.getInstance().getEventsService().publishEvent(event);
            previousStage = flowNode;
        } catch (IOException e) {
            e.printStackTrace();
            previousStage = null;
        }
    }

//	private boolean isItBuildStep(FlowNode flowNode) {
//		return ((StepAtomNode) flowNode).getDescriptor().getFunctionName().equalsIgnoreCase("build");
//	}
//
//	private void popNewJobEvent(FlowNode flowNode) {
//		// this option is off at the moment.
//		// in general, we want to pop regular jobs that created inside a stage HERE,
//		// and not in RunListenerImpl class.
//		// the reason for that is that we need to care of the CAUSE issue.
//
//
//		// getting real Jenkins object from its name.
//        String buildActualName = flowNode.getDisplayName().replace("Building ", "");
//        TopLevelItem jobAsJenkinsObj = Jenkins.getInstance().getItem(buildActualName);
//
//        if (jobAsJenkinsObj instanceof AbstractProject) {
//            AbstractProject jobAsAbstractProject = (AbstractProject) jobAsJenkinsObj;
//            List<CIEventCause> prevList = new LinkedList<CIEventCause>();
//            prevList.add(lastCIEventCause);
//            CIEventCause cause = dtoFactory.newDTO(CIEventCause.class);
//            cause.setType(CIEventCauseType.UPSTREAM)
//                    .setProject(lastDisplayName)
//                    .setBuildCiId(lastId)
//                    .setCauses(prevList);
//            List<CIEventCause> tempList = new LinkedList<CIEventCause>();
//            tempList.add(cause);
//
//            //creating CIEventCause and add it to the map
//            List<CIEventCause> ListForMap = new LinkedList<CIEventCause>();
//            ListForMap.add(cause);
//            CIEventCause causeForMap = dtoFactory.newDTO(CIEventCause.class);
//            causeForMap.setType(CIEventCauseType.UPSTREAM)
//                    .setProject(jobAsAbstractProject.getDisplayName())
//                    .setBuildCiId(String.valueOf(jobAsAbstractProject.getLastBuild().getNumber()))
//                    .setCauses(ListForMap);
//            BuildRelations.getInstance().addBuildRelation(jobAsAbstractProject.getDisplayName() + String.valueOf(jobAsAbstractProject.getLastBuild().getNumber() + 1), causeForMap);
//
//
//            // start event
//            CIEvent event = dtoFactory.newDTO(CIEvent.class)
//                    .setEventType(CIEventType.STARTED)
//                    .setProject(jobAsAbstractProject.getDisplayName())
//                    .setBuildCiId(String.valueOf(jobAsAbstractProject.getLastBuild().getNumber()))
//                    .setNumber(String.valueOf(jobAsAbstractProject.getLastBuild().getNumber()))
//                    .setStartTime(jobAsAbstractProject.getLastBuild().getStartTimeInMillis())
//                    .setEstimatedDuration(jobAsAbstractProject.getLastBuild().getEstimatedDuration())
//                    .setCauses(tempList);
//            EventsService.getExtensionInstance().dispatchEvent(event);
//
//            // finishEvent
//            CIEventCause endCause = dtoFactory.newDTO(CIEventCause.class);
//            endCause.setType(CIEventCauseType.UPSTREAM)
//                    .setProject(lastDisplayName)
//                    .setBuildCiId(lastId)
//                    .setCauses(prevList);
//            List<CIEventCause> endTempList = new LinkedList<CIEventCause>();
//            endTempList.add(endCause);
//            CIEvent endEvent = dtoFactory.newDTO(CIEvent.class)
//                    .setEventType(CIEventType.FINISHED)
//                    .setProject(jobAsAbstractProject.getDisplayName())
//                    .setBuildCiId(String.valueOf(jobAsAbstractProject.getLastBuild().getNumber()))
//                    .setNumber(String.valueOf(jobAsAbstractProject.getLastBuild().getNumber()))
//                    .setStartTime(12345678910L)
//                    .setEstimatedDuration(12345678910L)
//                    .setCauses(endTempList)
//                    .setResult(CIBuildResult.SUCCESS)
//                    .setDuration(12345678910L);
////            finishedEventsQueue.add(endEvent);
//        }
//
//	}


//	private void finishAllStartedEvents() {
//		while (!finishedEventsStack.isEmpty()) {
//			EventsService.getExtensionInstance().dispatchEvent(finishedEventsStack.pop());
//		}
//	}

//	private List<? extends Cause> extractCauses(Run r) {
//		return r.getCauses();
//	}

//	private String getParentName(FlowNode flowNode) {
//
//		String url = null;
//		try {
//			url = flowNode.getUrl();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		String[] words = url.split("/");
//		return words[1];
//	}


//	private String getParentId(FlowNode flowNode) {
//		String url = null;
//		try {
//			url = flowNode.getUrl();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		String[] words = url.split("/");
//		return words[2];
//	}

//
//	private void dispatchStartEvent(FlowNode flowNode) {
//		Long nodeStartTime = 0L;
//		if ((flowNode.getAllActions().get(3) instanceof TimingAction)) {
//			TimingAction t = (TimingAction) flowNode.getAllActions().get(3);
//			nodeStartTime = t.getStartTime();
//		}
//		List<CIEventCause> causeList = getCIEventCause(flowNode);
//		CIEvent event = dtoFactory.newDTO(CIEvent.class)
//				.setEventType(CIEventType.STARTED)
//				.setProject(flowNode.getDisplayName())
//				.setBuildCiId(String.valueOf(flowNode.getId()))
//				.setNumber(flowNode.getId())
//				.setStartTime(nodeStartTime)
//				.setEstimatedDuration(FlowNodeUtil.getStageExecDuration(flowNode).getPauseDurationMillis())
//				.setCauses(causeList);
//		EventsService.getExtensionInstance().dispatchEvent(event);
////        lastDisplayName = flowNode.getDisplayName();
////        lastId = String.valueOf(flowNode.getId());
////        lastCIEventCause = causeList.get(0);
//	}
////
//	private void createFinishEvent(FlowNode flowNode) {
//		String ParentName = getParentName(flowNode);
//		String ParentBuildNum = getParentId(flowNode);
//		CIEventCause endCause = dtoFactory.newDTO(CIEventCause.class)
//				.setType(CIEventCauseType.UPSTREAM)
//				.setProject(ParentName)
//				.setBuildCiId(ParentBuildNum);
//		List<CIEventCause> endTempList = new LinkedList<CIEventCause>();
//		endTempList.add(endCause);
//		CIEvent endEvent = dtoFactory.newDTO(CIEvent.class)
//				.setEventType(CIEventType.FINISHED)
//				.setProject(flowNode.getDisplayName())
//				.setBuildCiId(String.valueOf(flowNode.getId()))
//				.setNumber(flowNode.getId())
//				.setStartTime(12345678910L)
//				.setEstimatedDuration(12345678910L)
//				.setCauses(endTempList)
//				.setResult(CIBuildResult.SUCCESS)
//				.setDuration(12345678910L);
//		finishedEventsStack.push(endEvent);
//	}

//	private List<CIEventCause> getCIEventCause(FlowNode flowNode) {
//		String ParentName = getParentName(flowNode);
//		String ParentBuildNum = getParentId(flowNode);
//		CIEventCause cause = dtoFactory.newDTO(CIEventCause.class)
//				.setType(CIEventCauseType.UPSTREAM)
//				.setProject(ParentName)
//				.setBuildCiId(ParentBuildNum);
//		List<CIEventCause> tempList = new LinkedList<>();
//		tempList.add(cause);
//		return tempList;
//
//	}

    public static class FlowNodeContainer {
        private static Map<String, List<Run>> map = new HashMap<>();

        protected static void addFlowNode(Run run) {
            String key = getKey(run);
            List<Run> list = map.get(key);

            if (list == null) {
                list = new ArrayList<>();
                map.put(key, list);
            }
            list.add(run);
        }

        public static List<Run> getFlowNode(Run run) {
            return map.remove(getKey(run));
        }

        private static String getKey(Run run) {
            return run.getId() + run.getParent().getName();
        }
    }
}