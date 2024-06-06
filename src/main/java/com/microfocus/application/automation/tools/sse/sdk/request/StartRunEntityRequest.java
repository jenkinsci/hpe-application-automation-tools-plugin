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

package com.microfocus.application.automation.tools.sse.sdk.request;

import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.common.Pair;
import com.microfocus.application.automation.tools.model.CdaDetails;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */

public class StartRunEntityRequest extends PostRequest {
    
    private final String _duration;
    private final String _suffix;
    private final String _environmentConfigurationId;
    private final CdaDetails _cdaDetails;
    
    public StartRunEntityRequest(
            Client client,
            String suffix,
            String runId,
            String duration,
            String postRunAction,
            String environmentConfigurationId,
            CdaDetails cdaDetails) {
        
        super(client, runId);
        _duration = duration;
        _suffix = suffix;
        _environmentConfigurationId = environmentConfigurationId;
        _cdaDetails = cdaDetails;
    }
    
    @Override
    protected List<Pair<String, String>> getDataFields() {
        
        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
        ret.add(new Pair<String, String>("duration", _duration));
        ret.add(new Pair<String, String>("vudsMode", "false"));
        ret.add(new Pair<String, String>("reservationId", "-1"));
        if (!StringUtils.isNullOrEmpty(_environmentConfigurationId)) {
            ret.add(new Pair<String, String>("valueSetId", _environmentConfigurationId));
            if (_cdaDetails != null) {
                ret.add(new Pair<String, String>("topologyAction", _cdaDetails.getTopologyAction()));
                ret.add(new Pair<String, String>(
                        "realizedTopologyName",
                        _cdaDetails.getDeployedEnvironmentName()));
                
            }
        }
        
        return ret;
    }
    
    @Override
    protected String getSuffix() {
        
        return _suffix;
    }
    
}
