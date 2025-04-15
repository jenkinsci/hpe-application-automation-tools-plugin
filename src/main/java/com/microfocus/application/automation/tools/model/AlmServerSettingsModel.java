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
package com.microfocus.application.automation.tools.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;


public class AlmServerSettingsModel extends AbstractDescribableImpl<AlmServerSettingsModel> implements Comparable<AlmServerSettingsModel>{
    
    private final String _almServerName;
    private final String _almServerUrl;
    private List<CredentialsModel> _almCredentials;
    private List<SSOCredentialsModel> _almSSOCredentials;

    @DataBoundConstructor
    public AlmServerSettingsModel(String almServerName, String almServerUrl,
                                  List<CredentialsModel> almCredentials,
                                  List<SSOCredentialsModel> almSSOCredentials) {
        
        this._almServerName = almServerName.trim();
        this._almServerUrl = almServerUrl.trim();
        this._almCredentials = almCredentials;
        this._almSSOCredentials = almSSOCredentials;
    }

    /**
     * @return the almServerName
     */
    public String getAlmServerName() {
        
        return _almServerName;
    }
    
    /**
     * @return the almServerUrl
     */
    public String getAlmServerUrl() {
        
        return _almServerUrl;
    }

    public List<CredentialsModel> getAlmCredentials(){
        if(_almCredentials != null) {
            return _almCredentials;
        }

        return new ArrayList<>();
    }

    public void set_almCredentials(List<CredentialsModel> almCredentials){
        this._almCredentials = almCredentials;
    }

    public List<SSOCredentialsModel> getAlmSSOCredentials() {
        if (_almSSOCredentials != null) {
            return _almSSOCredentials;
        }

        return new ArrayList<>();
    }

    public void set_almSSOCredentials(List<SSOCredentialsModel> almSSOCredentials){
        this._almSSOCredentials = almSSOCredentials;
    }

    public Properties getProperties() {
        
        Properties prop = new Properties();

        if (!StringUtils.isEmpty(_almServerUrl)) {
            prop.put("almServerUrl", _almServerUrl.trim());
        } else {
            prop.put("almServerUrl", "");
        }
        return prop;
    }

    @Override
    public int compareTo(AlmServerSettingsModel model) {
        return _almServerName.compareTo(model._almServerName);
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<AlmServerSettingsModel> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Alm Server Settings Model";
        }
    }
}
