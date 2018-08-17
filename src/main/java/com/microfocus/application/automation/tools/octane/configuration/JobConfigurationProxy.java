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

package com.microfocus.application.automation.tools.octane.configuration;

import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.exception.RequestException;
import com.hp.mqm.client.model.*;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.client.JenkinsMqmRestClientFactory;
import com.microfocus.application.automation.tools.octane.client.RetryModel;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import hudson.ExtensionList;
import hudson.model.Job;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * This class is a proxy between JS UI code and server-side job configuration.
 */
public class JobConfigurationProxy {
	private final static Logger logger = LogManager.getLogger(JobConfigurationProxy.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	final private Job job;
	private RetryModel retryModel;

	private static final String PRODUCT_NAME = Messages.ServerName();
	private static final String NOT_SPECIFIED = "-- Not specified --";

	JobConfigurationProxy(Job job) {
		this.job = job;
	}

	@JavaScriptMethod
	public JSONObject createPipelineOnServer(JSONObject pipelineObject) throws IOException {
		JSONObject result = new JSONObject();

		PipelineNode pipelineNode = ModelFactory.createStructureItem(job);
		CIServerInfo ciServerInfo = OctaneSDK.getInstance().getPluginServices().getServerInfo();
		Long releaseId = pipelineObject.getLong("releaseId") != -1 ? pipelineObject.getLong("releaseId") : null;

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}

		try {
			Pipeline createdPipeline = client.createPipeline(
					ConfigurationService.getModel().getIdentity(),
                    pipelineNode.getJobCiId(),
					pipelineObject.getString("name"),
					pipelineObject.getLong("workspaceId"),
					releaseId,
					dtoFactory.dtoToJson(pipelineNode),
					dtoFactory.dtoToJson(ciServerInfo));

			//WORKAROUND BEGIN
			//getting workspaceName - because the workspaceName is not returned from configuration API
			List<Workspace> workspaces = client.getWorkspaces(Collections.singletonList(createdPipeline.getWorkspaceId()));
			if (workspaces.size() != 1) {
				throw new ClientException("WorkspaceName could not be retrieved for workspaceId: " + createdPipeline.getWorkspaceId());
			}
			//WORKAROUND END

			JSONObject pipelineJSON = fromPipeline(createdPipeline, workspaces.get(0));
			//WORKAROUND BEGIN
			//all metadata have to be loaded in separate REST calls for this pipeline: releaseName, taxonomyNames and listFieldNames are not returned from configuration API
			enrichPipeline(pipelineJSON, client);
			//WORKAROUND END
			result.put("pipeline", pipelineJSON);

			JSONArray fieldsMetadata = getFieldMetadata(createdPipeline.getWorkspaceId(), client);
			result.put("fieldsMetadata", fieldsMetadata);

		} catch (RequestException e) {
			logger.warn("Failed to create pipeline", e);
            String msg = e.getDescription() != null ? e.getDescription() : e.getMessage();
            return error("Unable to create pipeline. " + msg);
		} catch (ClientException e) {
			logger.warn("Failed to create pipeline", e);
			return error(e.getMessage(), e.getLink());
		}
		return result;
	}

	@JavaScriptMethod
	public JSONObject updatePipelineOnSever(JSONObject pipelineObject) throws IOException {
		JSONObject result = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {
			long pipelineId = pipelineObject.getLong("id");

			LinkedList<Taxonomy> taxonomies = new LinkedList<>();
			JSONArray taxonomyTags = pipelineObject.getJSONArray("taxonomyTags");
			for (JSONObject jsonObject : toCollection(taxonomyTags)) {
				taxonomies.add(new Taxonomy(jsonObject.optLong("tagId"), jsonObject.getString("tagName"),
						new Taxonomy(jsonObject.optLong("tagTypeId"), jsonObject.getString("tagTypeName"), null)));
			}

			LinkedList<ListField> fields = new LinkedList<>();
			JSONArray fieldTags = pipelineObject.getJSONArray("fieldTags");
			for (JSONObject jsonObject : toCollection(fieldTags)) {
				List<ListItem> assignedValues = new LinkedList<>();
				for (JSONObject value : toCollection(jsonObject.getJSONArray("values"))) {
					String id;
					if (value.containsKey("id")) {
						id = value.getString("id");
					} else {
						id = null;
					}
					assignedValues.add(new ListItem(id, null, value.getString("name"), null));
				}
				fields.add(new ListField(jsonObject.getString("name"), assignedValues));
			}

            final String jobCiId = JobProcessorFactory.getFlowProcessor(job).getTranslateJobName();

            Pipeline pipeline = client.updatePipeline(ConfigurationService.getModel().getIdentity(), jobCiId,
					new Pipeline(pipelineId, pipelineObject.getString("name"), null, pipelineObject.getLong("workspaceId"), pipelineObject.getLong("releaseId"), taxonomies, fields, pipelineObject.getBoolean("ignoreTests")));

			//WORKAROUND BEGIN
			//getting workspaceName - because the workspaceName is not returned from configuration API
			List<Workspace> workspaces = client.getWorkspaces(Collections.singletonList(pipeline.getWorkspaceId()));
			if (workspaces.size() != 1) {
				throw new ClientException("WorkspaceName could not be retrieved for workspaceId: " + pipeline.getWorkspaceId());
			}
			//WORKAROUND END

			JSONObject pipelineJSON = fromPipeline(pipeline, workspaces.get(0));
			//WORKAROUND BEGIN
			//all metadata have to be loaded in separate REST calls for this pipeline: releaseName, taxonomyNames and listFieldNames are not returned from configuration API
			enrichPipeline(pipelineJSON, client);
			//WORKAROUND END
			result.put("pipeline", pipelineJSON);


			//Server might do partial sucess
			//So need to validate each item if it succedded or not
			//For now we add handling of duplicate pipeline name
			String originalName = pipelineObject.get("name").toString();
			String updatedName = pipelineJSON.get("name").toString();
			if (!originalName.equalsIgnoreCase(updatedName)) {
				JSONObject errorObj = new JSONObject();
				errorObj.put("message", "Failed to update pipeline name. Make sure not to enter the name of an existing pipeline.");
				result.put("error", errorObj);
			}

		} catch (RequestException e) {
			logger.warn("Failed to update pipeline", e);
			return error("Unable to update pipeline");
		} catch (ClientException e) {
			logger.warn("Failed to update pipeline", e);
			return error(e.getMessage(), e.getLink());
		}

		return result;
	}


	@JavaScriptMethod
	public JSONObject deleteTests(JSONObject pipelineObject) throws IOException, InterruptedException {
		JSONObject result = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {
			long pipelineId = pipelineObject.getLong("id");
			long workspaceId = pipelineObject.getLong("workspaceId");
			client.deleteTestsFromPipelineNodes(job.getName(), pipelineId, workspaceId);
			result.put("Test deletion was succeful", "");
		} catch (RequestException e) {
			logger.warn("Failed to delete tests", e);
			return error("Unable to delete tests");
		}

		return result;
	}

	@JavaScriptMethod
	public JSONObject loadJobConfigurationFromServer() throws IOException {
		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}

		JSONObject ret = new JSONObject();
		JSONObject workspaces = new JSONObject();
		JSONArray fieldsMetadata = new JSONArray();
		try {
			boolean isUftJob = false;
			List<CIParameter> parameters = ParameterProcessors.getConfigs(job);
			if(parameters != null) {
				for(CIParameter parameter : parameters) {
					if(parameter != null && parameter.getName() != null && parameter.getName().equals("suiteId")) {
						isUftJob = true;
						break;
					}
				}
			}
			ret.put("isUftJob", isUftJob);

            final String jobCiId = JobProcessorFactory.getFlowProcessor(job).getTranslateJobName();
            JobConfiguration jobConfiguration = client.getJobConfiguration(ConfigurationService.getModel().getIdentity(), jobCiId);

			if (!jobConfiguration.getWorkspacePipelinesMap().isEmpty()) {
				Map<Long, List<Pipeline>> workspacesMap = jobConfiguration.getWorkspacePipelinesMap();
				//WORKAROUND BEGIN
				//getting workspaceName - because the workspaceName is not returned from configuration API
				Map<Long, String> relatedWorkspaces = new HashMap<>();
				List<Workspace> workspaceList = client.getWorkspaces(new LinkedList<>(workspacesMap.keySet()));
				for (Workspace workspace : workspaceList) {
					relatedWorkspaces.put(workspace.getId(), workspace.getName());
				}
				//WORKAROUND END

				Map<Workspace, List<Pipeline>> sortedWorkspacesMap = new TreeMap<>(new Comparator<Workspace>() {
					@Override
					public int compare(final Workspace w1, final Workspace w2) {
						return w1.getName().compareTo(w2.getName());
					}
				});
				Comparator<Pipeline> pipelineComparator = new Comparator<Pipeline>() {
					@Override
					public int compare(final Pipeline p1, final Pipeline p2) {
						return p1.getName().compareTo(p2.getName());
					}
				};

				//create workspaces JSON Object
				for (Entry<Long, List<Pipeline>> workspacePipelines : workspacesMap.entrySet()) {
					Workspace relatedWorkspace = new Workspace(workspacePipelines.getKey(), relatedWorkspaces.get(workspacePipelines.getKey()));
					JSONObject relatedPipelinesJSON = new JSONObject();

					for (Pipeline relatedPipeline : workspacePipelines.getValue()) {
						JSONObject pipelineJSON = fromPipeline(relatedPipeline, relatedWorkspace);
						relatedPipelinesJSON.put(String.valueOf(relatedPipeline.getId()), pipelineJSON);
					}
					JSONObject workspaceJSON = new JSONObject();
					workspaceJSON.put("id", relatedWorkspace.getId());
					workspaceJSON.put("name", relatedWorkspace.getName());
					workspaceJSON.put("pipelines", relatedPipelinesJSON);
					workspaces.put(String.valueOf(relatedWorkspace.getId()), workspaceJSON);

					//inserting this workspace into sortedMap (sorted by workspaceName and by pipelineName, so that we can pick first workspace and its first pipeline as preselected values
					LinkedList<Pipeline> workspacePipelinesList = new LinkedList<Pipeline>(workspacePipelines.getValue());
					Collections.sort(workspacePipelinesList, pipelineComparator);
					sortedWorkspacesMap.put(relatedWorkspace, workspacePipelinesList);
				}

				//create currentPipeline JSON Object
				//currently the first pipeline in the first workspace is picked
				Workspace preSelectedWorkspace = sortedWorkspacesMap.keySet().iterator().next();
				Pipeline preSelectedPipeline = sortedWorkspacesMap.get(preSelectedWorkspace).get(0);
				JSONObject preSelectedPipelineJSON = fromPipeline(preSelectedPipeline, preSelectedWorkspace);

				//WORKAROUND BEGIN
				//all metadata have to be loaded in separate REST calls for this pipeline: releaseName, taxonomyNames and listFieldNames are not returned from configuration API
				enrichPipeline(preSelectedPipelineJSON, client);
				//WORKAROUND END
				ret.put("currentPipeline", preSelectedPipelineJSON);

				//retrieving metadata fields for preselected workspace
				fieldsMetadata = getFieldMetadata(preSelectedWorkspace.getId(), client);
			}

			ret.put("workspaces", workspaces);
			ret.put("fieldsMetadata", fieldsMetadata);

		} catch (RequestException e) {
			logger.warn("Failed to retrieve job configuration", e);
			return error("Unable to retrieve job configuration");
		}

		return ret;
	}

	@JavaScriptMethod
	public JSONObject loadWorkspaceConfiguration(JSONObject pipelineJSON) {
		MqmRestClient client;
		JSONObject ret = new JSONObject();
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}

		try {
			JSONArray fieldsMetadata = getFieldMetadata(pipelineJSON.getLong("workspaceId"), client);
			ret.put("fieldsMetadata", fieldsMetadata);
			enrichPipeline(pipelineJSON, client);
			ret.put("pipeline", pipelineJSON);
		} catch (RequestException e) {
			logger.warn("Failed to retrieve metadata for workspace", e);
			return error("Unable to retrieve metadata for workspace");
		}

		return ret;
	}

	private JSONObject fromPipeline(final Pipeline pipeline, Workspace relatedWorkspace) {
		JSONObject pipelineJSON = new JSONObject();
		pipelineJSON.put("id", pipeline.getId());
		pipelineJSON.put("name", pipeline.getName());
		pipelineJSON.put("releaseId", pipeline.getReleaseId() != null ? pipeline.getReleaseId() : -1);
		pipelineJSON.put("isRoot", pipeline.isRoot());
		pipelineJSON.put("workspaceId", relatedWorkspace.getId());
		pipelineJSON.put("workspaceName", relatedWorkspace.getName());
		pipelineJSON.put("ignoreTests", pipeline.getIgnoreTests());
		addTaxonomyTags(pipelineJSON, pipeline);
		addFields(pipelineJSON, pipeline);

		return pipelineJSON;
	}

	@JavaScriptMethod
	public JSONObject enrichPipeline(JSONObject pipelineJSON) {
		MqmRestClient client;
		JSONObject ret = new JSONObject();
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}

		try {
			enrichPipeline(pipelineJSON, client);
			ret.put("pipeline", pipelineJSON);
		} catch (RequestException e) {
			logger.warn("Failed to retrieve metadata for pipeline", e);
			return error("Unable to retrieve metadata for pipeline");
		}

		return ret;
	}

	private void enrichPipeline(JSONObject pipelineJSON, MqmRestClient client) {
		enrichRelease(pipelineJSON, client);
		enrichTaxonomies(pipelineJSON, client);
		enrichFields(pipelineJSON, client);
	}

	private void enrichRelease(JSONObject pipeline, MqmRestClient client) {
		long workspaceId = pipeline.getLong("workspaceId");
		if (pipeline.containsKey("releaseId") && pipeline.getLong("releaseId") != -1) {
			long releaseId = pipeline.getLong("releaseId");
			String releaseName = client.getRelease(releaseId, workspaceId).getName();
			pipeline.put("releaseName", releaseName);
		}
	}

	private void enrichTaxonomies(JSONObject pipeline, MqmRestClient client) {
		JSONArray ret = new JSONArray();
		if (pipeline.has("taxonomyTags")) {

			JSONArray taxonomyTags = pipeline.getJSONArray("taxonomyTags");
			List<Long> taxonomyIdsList = new LinkedList<>();
			for (int i = 0; i < taxonomyTags.size(); i++) {
				JSONObject taxonomy = taxonomyTags.getJSONObject(i);
				if (taxonomy.has("tagId")) {
					taxonomyIdsList.add(taxonomy.getLong("tagId"));
				}
			}
			List<Taxonomy> taxonomies = client.getTaxonomies(taxonomyIdsList, pipeline.getLong("workspaceId"));
			for (Taxonomy tax : taxonomies) {
				ret.add(tag(tax));
			}
		}
		pipeline.put("taxonomyTags", ret);
	}

	private void enrichFields(JSONObject pipeline, MqmRestClient client) {
		JSONObject ret = new JSONObject();

		if (pipeline.has("fields")) {
			long workspaceId = pipeline.getLong("workspaceId");
			JSONObject pipelineFields = pipeline.getJSONObject("fields");
			Iterator<?> keys = pipelineFields.keys();
			//iteration over listFields
			while (keys.hasNext()) {
				String key = (String) keys.next();
				if (pipelineFields.get(key) instanceof JSONArray) {
					List<String> fieldTagsIdsList = new LinkedList<>();
					//getting all ids assigned to listField
					for (JSONObject singleField : toCollection(pipelineFields.getJSONArray(key))) {
						fieldTagsIdsList.add(singleField.getString("id"));
					}
					//retrieving names of assigned items
					if (fieldTagsIdsList.size() > 0) {
						List<ListItem> enrichedFields = client.getListItems(fieldTagsIdsList, workspaceId);
						JSONArray values = new JSONArray();
						for (ListItem item : enrichedFields) {
							JSONObject value = new JSONObject();
							value.put("id", item.getId());
							value.put("name", item.getName());
							values.add(value);
						}
						ret.put(key, values);
					}
				}
			}
		}
		pipeline.put("fields", ret);
	}

	private JSONArray getFieldMetadata(final long workspaceId, MqmRestClient client) {
		JSONArray fieldMetadataArray = new JSONArray();
		List<FieldMetadata> metadataList = client.getFieldsMetadata(workspaceId);

		for (FieldMetadata fieldMetadata : metadataList) {
			fieldMetadataArray.add(fromFieldMetadata(fieldMetadata));
		}
		return fieldMetadataArray;
	}

	private JSONObject fromFieldMetadata(FieldMetadata fieldMetadata) {
		JSONObject fieldMetadataJSON = new JSONObject();
		fieldMetadataJSON.put("name", fieldMetadata.getName());
		fieldMetadataJSON.put("listName", fieldMetadata.getListName());
		fieldMetadataJSON.put("logicalListName", fieldMetadata.getLogicalListName());
		fieldMetadataJSON.put("extensible", fieldMetadata.isExtensible());
		fieldMetadataJSON.put("multiValue", fieldMetadata.isMultiValue());
		fieldMetadataJSON.put("order", fieldMetadata.getOrder());
		return fieldMetadataJSON;
	}

	@JavaScriptMethod
	public JSONObject searchListItems(String logicalListName, String term, long workspaceId, boolean multiValue, boolean extensible) {
		int defaultSize = 10;
		JSONObject ret = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {

			PagedList<ListItem> listItemPagedList = client.queryListItems(logicalListName, term, workspaceId, 0, defaultSize);
			List<ListItem> listItems = listItemPagedList.getItems();
			boolean moreResults = listItemPagedList.getTotalCount() > listItems.size();

			JSONArray retArray = new JSONArray();
			if (moreResults) {
				retArray.add(createMoreResultsJson());
			}

			if (!multiValue) {
				String quotedTerm = Pattern.quote(term.toLowerCase());
				if (Pattern.matches(".*" + quotedTerm + ".*", NOT_SPECIFIED.toLowerCase())) {
					JSONObject notSpecifiedItemJson = new JSONObject();
					notSpecifiedItemJson.put("id", -1);
					notSpecifiedItemJson.put("text", NOT_SPECIFIED);
					retArray.add(notSpecifiedItemJson);
				}
			}

			for (ListItem item : listItems) {
				if (!toBeFiltered(item)) {
					JSONObject itemJson = new JSONObject();
					itemJson.put("id", item.getId());
					itemJson.put("text", item.getName());
					retArray.add(itemJson);
				}

			}
			// we shall use "if (extensible){}" on following line, but we do not have UI ready for the case: multiValue = true & extensible = true
			if (extensible && !multiValue) {
				//if exactly one item matches, we do not want to bother user with "new value" item
				if ((listItems.size() != 1) || (!listItems.get(0).getName().toLowerCase().equals(term.toLowerCase()))) {
					retArray.add(createNewValueJson("0"));
				}
			}


			ret.put("results", retArray);
		} catch (RequestException e) {
			logger.warn("Failed to retrieve list items", e);
			return error("Unable to retrieve job configuration");
		}

		return ret;
	}

	private boolean toBeFiltered(ListItem item) {
		return (item.getLogicalName().equalsIgnoreCase("list_node.testing_tool_type.manual"));
	}

	@JavaScriptMethod
	public JSONObject searchReleases(String term, long workspaceId) {
		int defaultSize = 5;
		JSONObject ret = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {
			PagedList<Release> releasePagedList = client.queryReleases(term, workspaceId, 0, defaultSize);
			List<Release> releases = releasePagedList.getItems();
			boolean moreResults = releasePagedList.getTotalCount() > releases.size();

			JSONArray retArray = new JSONArray();
			if (moreResults) {
				retArray.add(createMoreResultsJson());
			}

			String quotedTerm = Pattern.quote(term.toLowerCase());
			if (Pattern.matches(".*" + quotedTerm + ".*", NOT_SPECIFIED.toLowerCase())) {
				JSONObject notSpecifiedItemJson = new JSONObject();
				notSpecifiedItemJson.put("id", -1);
				notSpecifiedItemJson.put("text", NOT_SPECIFIED);
				retArray.add(notSpecifiedItemJson);
			}

			for (Release release : releases) {
				JSONObject relJson = new JSONObject();
				relJson.put("id", release.getId());
				relJson.put("text", release.getName());
				retArray.add(relJson);
			}
			ret.put("results", retArray);

		} catch (RequestException e) {
			logger.warn("Failed to retrieve releases", e);
			return error("Unable to retrieve releases");
		}

		return ret;
	}

	@JavaScriptMethod
	public JSONObject searchWorkspaces(String term) {
		int defaultSize = 5;
		JSONObject ret = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {
			PagedList<Workspace> workspacePagedList = client.queryWorkspaces(term, 0, defaultSize);
			List<Workspace> workspaces = workspacePagedList.getItems();
			boolean moreResults = workspacePagedList.getTotalCount() > workspaces.size();

			JSONArray retArray = new JSONArray();
			if (moreResults) {
				retArray.add(createMoreResultsJson());
			}

			for (Workspace workspace : workspaces) {
				JSONObject relJson = new JSONObject();
				relJson.put("id", workspace.getId());
				relJson.put("text", workspace.getName());
				retArray.add(relJson);
			}
			ret.put("results", retArray);

		} catch (RequestException e) {
			logger.warn("Failed to retrieve workspaces", e);
			return error("Unable to retrieve workspaces");
		}

		return ret;
	}

	@JavaScriptMethod
	public JSONObject searchTaxonomies(String term, long workspaceId, JSONArray pipelineTaxonomies) {
		int defaultSize = 20;
		JSONObject ret = new JSONObject();

		MqmRestClient client;
		try {
			client = createClient();
		} catch (ClientException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			return error(e.getMessage(), e.getLink());
		}
		try {

			//currently existing taxonomies on pipeline -> we need to show these options as disabled
			List<Long> pipelineTaxonomiesList = new LinkedList<>();
			for (int i = 0; i < pipelineTaxonomies.size(); i++) {
				JSONObject pipelineTaxonomy = pipelineTaxonomies.getJSONObject(i);
				if (pipelineTaxonomy.containsKey("tagId") && pipelineTaxonomy.containsKey("tagTypeId")) {   // we need to compare only taxonomies which already exist on server
					pipelineTaxonomiesList.add(pipelineTaxonomy.getLong("tagId"));
				}
			}
			//retrieving taxonomies from server
			PagedList<Taxonomy> foundTaxonomies = client.queryTaxonomies(term, workspaceId, 0, defaultSize);
			final List<Taxonomy> foundTaxonomiesList = foundTaxonomies.getItems();
			boolean moreResults = foundTaxonomies.getTotalCount() > foundTaxonomiesList.size();

			//creating map <TaxonomyCategoryID : Set<Taxonomy>>
			// for easier creating result JSON
			Map<Long, Set<Taxonomy>> taxonomyMap = new HashMap<>();
			Map<Long, String> taxonomyCategories = new HashMap<>();
			for (Taxonomy taxonomy : foundTaxonomiesList) {
				if (taxonomy.getRoot() != null) {
					if (taxonomyMap.containsKey(taxonomy.getRoot().getId())) {
						taxonomyMap.get(taxonomy.getRoot().getId()).add(taxonomy);
					} else {
						taxonomyMap.put(taxonomy.getRoot().getId(), new LinkedHashSet<>(Arrays.asList(taxonomy)));
						taxonomyCategories.put(taxonomy.getRoot().getId(), taxonomy.getRoot().getName());
					}
				}
			}

			//writing result json
			JSONArray select2InputArray = new JSONArray();
			JSONObject allTags = new JSONObject();
			JSONObject tagTypesByName = new JSONObject();
			JSONObject tagTypes = new JSONObject();

			//show warning, that there are more results and user should filter more specific
			if (moreResults) {
				select2InputArray.add(createMoreResultsJson());
			}

			for (Entry<Long, Set<Taxonomy>> taxonomyType : taxonomyMap.entrySet()) {
				Long tagTypeId = taxonomyType.getKey();
				String tagTypeName = taxonomyCategories.get(tagTypeId);
				JSONArray childrenArray = new JSONArray();

				JSONObject optgroup = new JSONObject();
				optgroup.put("text", tagTypeName);

				//for tagTypesByName
				JSONObject tagTypeJson = new JSONObject();
				tagTypeJson.put("tagTypeId", tagTypeId);
				tagTypeJson.put("tagTypeName", tagTypeName);
				JSONArray tagTypeByNameValues = new JSONArray();

				for (Taxonomy tax : taxonomyType.getValue()) {
					//creating input format for select2, so that this structure does not have to be refactored in javascript
					JSONObject taxonomyJson = new JSONObject();
					taxonomyJson.put("id", tax.getId());
					taxonomyJson.put("text", tax.getName());
					taxonomyJson.put("value", tax.getId());
					if (pipelineTaxonomiesList.contains(tax.getId())) {
						taxonomyJson.put("disabled", "disabled");
					}
					childrenArray.add(taxonomyJson);

					//for allTags - adding tag into table of selected ones
					JSONObject tagObject = new JSONObject();
					tagObject.put("tagId", tax.getId());
					tagObject.put("tagName", tax.getName());
					tagObject.put("tagTypeId", tax.getRoot().getId());
					tagObject.put("tagTypeName", tax.getRoot().getName());
					allTags.put(String.valueOf(tax.getId()), tagObject);

					//for tagTypesByName
					JSONObject tagTypeByNameValue = new JSONObject();
					tagTypeByNameValue.put("tagId", tax.getId());
					tagTypeByNameValue.put("tagName", tax.getName());
					tagTypeByNameValues.add(tagTypeByNameValue);
				}
				//New value.. for current type
				JSONObject newValueJson = createNewValueJson(Long.toString(tagTypeValue(tagTypeId)));
				childrenArray.add(newValueJson);

				optgroup.put("children", childrenArray);
				select2InputArray.add(optgroup);
				tagTypeJson.put("values", tagTypeByNameValues);
				tagTypesByName.put(tagTypeName, tagTypeJson);
				tagTypes.put(Long.toString(tagTypeId), tagTypeJson);
			}

			// New type... New value...
			JSONObject optgroup = new JSONObject();
			optgroup.put("text", "New type...");
			JSONObject newValueJson = createNewValueJson("newTagType");
			JSONArray childrenArray = new JSONArray();
			childrenArray.add(newValueJson);
			optgroup.put("children", childrenArray);
			select2InputArray.add(optgroup);

			ret.put("select2Input", select2InputArray);
			ret.put("allTags", allTags);
			ret.put("tagTypesByName", tagTypesByName);
			ret.put("tagTypes", tagTypes);
			ret.put("more", moreResults);

		} catch (RequestException e) {
			logger.warn("Failed to retrieve environments", e);
			return error("Unable to retrieve environments");
		}

		return ret;
	}

	private JSONObject createMoreResultsJson() {
		JSONObject moreResultsJson = new JSONObject();
		moreResultsJson.put("id", "moreResultsFound");
		moreResultsJson.put("text", Messages.TooManyResults());
		moreResultsJson.put("warning", "true");
		moreResultsJson.put("disabled", "disabled");
		return moreResultsJson;
	}

	private JSONObject createNewValueJson(String id) {
		JSONObject newValueJson = new JSONObject();
		newValueJson.put("id", id);
		newValueJson.put("text", "New value...");
		newValueJson.put("newValue", "true");
		return newValueJson;
	}

	private long tagTypeValue(long n) {
		// mapping to ensure negative value (solve the "0" tag type ID)
		return -(n + 1);
	}

	private void addTaxonomyTags(JSONObject result, Pipeline pipeline) {
		JSONArray pipelineTaxonomies = new JSONArray();
		for (Taxonomy taxonomy : pipeline.getTaxonomies()) {
			pipelineTaxonomies.add(tag(taxonomy));
		}
		result.put("taxonomyTags", pipelineTaxonomies);
	}

	private void addFields(JSONObject result, Pipeline pipeline) {
		JSONObject listFields = new JSONObject();

		for (ListField field : pipeline.getFields()) {
			JSONArray assignedValuesArray = listFieldValues(field);
			listFields.put(field.getName(), assignedValuesArray);
		}
		result.put("fields", listFields);
	}

	private JSONArray listFieldValues(ListField field) {
		JSONArray ret = new JSONArray();

		for (ListItem item : field.getValues()) {
			JSONObject value = new JSONObject();
			value.put("id", item.getId());
			if (item.getName() != null) {
				value.put("name", item.getName());
			}
			ret.add(value);
		}
		return ret;
	}

	private static Collection<JSONObject> toCollection(JSONArray array) {
		return (Collection<JSONObject>) array.subList(0, array.size());
	}

	private JSONObject tag(Long tagId, String value) {
		JSONObject tag = new JSONObject();
		tag.put("tagId", String.valueOf(tagId));
		tag.put("tagName", value);
		return tag;
	}

	private JSONObject tag(Taxonomy taxonomy) {
		JSONObject tag = tag(taxonomy.getId(), taxonomy.getName());
		if (taxonomy.getRoot() != null) {
			tag.put("tagTypeId", String.valueOf(taxonomy.getRoot().getId()));
			tag.put("tagTypeName", taxonomy.getRoot().getName());
		}
		return tag;
	}

	private JSONObject error(String message) {
		return error(message, null);
	}

	private JSONObject error(String message, ExceptionLink exceptionLink) {
		JSONObject result = new JSONObject();
		JSONArray errors = new JSONArray();
		JSONObject error = new JSONObject();
		error.put("message", message);
		if (exceptionLink != null) {
			error.put("url", exceptionLink.getUrl());
			error.put("label", exceptionLink.getLabel());
		}
		errors.add(error);
		result.put("errors", errors);
		return result;
	}

	private MqmRestClient createClient() throws ClientException {
		ServerConfiguration configuration = ConfigurationService.getServerConfiguration();
		if (StringUtils.isEmpty(configuration.location)) {
			String label = "Please configure server here";
			throw new ClientException(PRODUCT_NAME + " not configured", new ExceptionLink("/configure", label));
		}

		RetryModel retryModel = getRetryModel();

		if (retryModel.isQuietPeriod()) {
			String label = "Please validate your configuration settings here";
			throw new ClientException(PRODUCT_NAME + " not connected", new ExceptionLink("/configure", label));
		}

		JenkinsMqmRestClientFactory clientFactory = getExtension(JenkinsMqmRestClientFactory.class);
		MqmRestClient client = clientFactory.obtain(
				configuration.location,
				configuration.sharedSpace,
				configuration.username,
				configuration.password);
		try {
			client.validateConfigurationWithoutLogin();
		} catch (RequestException e) {
			logger.warn(PRODUCT_NAME + " connection failed", e);
			retryModel.failure();
			throw new ClientException("Connection to " + PRODUCT_NAME + " failed");
		}

		retryModel.success();
		return client;
	}

	private RetryModel getRetryModel() {
		if (retryModel == null) {
			retryModel = getExtension(RetryModel.class);
		}
		return retryModel;
	}

	private static <T> T getExtension(Class<T> clazz) {
		ExtensionList<T> items = Jenkins.getInstance().getExtensionList(clazz);
		assert 1 == items.size() : "Expected to have one and only one extension of type " + clazz;
		return items.get(0);
	}

	private static class ClientException extends Exception {

		private ExceptionLink link;

		ClientException(String message) {
			this(message, null);
		}

		ClientException(String message, ExceptionLink link) {
			super(message);
			this.link = link;
		}

		public ExceptionLink getLink() {
			return link;
		}
	}

	private static class ExceptionLink {

		private String url;
		private String label;

		ExceptionLink(String url, String label) {
			this.url = url;
			this.label = label;
		}

		public String getUrl() {
			return url;
		}

		public String getLabel() {
			return label;
		}
	}
}
