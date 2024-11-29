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
package com.microfocus.application.automation.tools.uft.model;

import com.microfocus.application.automation.tools.EncryptionUtils;
import com.microfocus.application.automation.tools.model.EnumDescription;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Node;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class SpecifyParametersModel extends AbstractDescribableImpl<SpecifyParametersModel> {
	private final static String PWD = "Password";

    // FOR GUI
    private final static EnumDescription STRING_TYPE = new EnumDescription("String", "String");
    private final static EnumDescription NUMBER_TYPE = new EnumDescription("Number", "Number");
    private final static EnumDescription DATE_TYPE = new EnumDescription("Date", "Date");
    private final static EnumDescription BOOL_TYPE = new EnumDescription("Boolean", "Boolean");
    private final static EnumDescription ANY_TYPE = new EnumDescription("Any", "Any");
    private final static EnumDescription PWD_TYPE = new EnumDescription(PWD, PWD);

    public final static List<EnumDescription> paramTypesGUI = Arrays.asList(STRING_TYPE, NUMBER_TYPE, DATE_TYPE, BOOL_TYPE, ANY_TYPE, PWD_TYPE);

    // FOR API
    private final static EnumDescription INT_TYPE = new EnumDescription("Int", "Int");
    private final static EnumDescription FLOAT_TYPE = new EnumDescription("Float", "Float");
    private final static EnumDescription DATETIME_TYPE = new EnumDescription("DateTime", "DateTime");
    private final static EnumDescription LONG_TYPE = new EnumDescription("Long", "Long");
    private final static EnumDescription DOUBLE_TYPE = new EnumDescription("Double", "Double");
    private final static EnumDescription DECIMAL_TYPE = new EnumDescription("Decimal", "Decimal");

    public final static List<EnumDescription> paramTypesAPI = Arrays.asList(STRING_TYPE, INT_TYPE, FLOAT_TYPE, DATETIME_TYPE, BOOL_TYPE, LONG_TYPE, DOUBLE_TYPE, DECIMAL_TYPE);

    public final static Map<String, List<EnumDescription>> mapping = new HashMap<>();
    public final static int NUM_OF_TYPES = paramTypesAPI.size() + paramTypesGUI.size();

    static {
        mapping.put("GUI", paramTypesGUI);
        mapping.put("API", paramTypesAPI);
    }

    private String parameterJson;

    @DataBoundConstructor
    public SpecifyParametersModel(String parameterJson) {
        this.parameterJson = parameterJson;
    }

    public String getParameterJson() {
        return parameterJson;
    }

    public void setParameterJson(String parameterJson) {
        this.parameterJson = parameterJson;
    }

    public void addProperties(Properties props, String searchStr, Node node) throws Exception {
        JSONArray testParams = (JSONArray) JSONValue.parseStrict(parameterJson);

        int pidx = 1;
        while (props.getProperty(searchStr + pidx) != null) {
            final int currPidx = pidx;

            List<Object> relevant = testParams.stream().filter(elem -> Integer.parseInt((String) (((JSONObject) elem).get("index"))) == currPidx).collect(Collectors.toList());

            for (int i = 0; i < relevant.size(); ++i) {
                JSONObject curr = ((JSONObject) relevant.get(i));
                String name = curr.get("name").toString();
                String type = curr.get("type").toString();
                String val = curr.get("value").toString();
                if (type.equals(PWD) && StringUtils.isNotBlank(val))
                {
                    val = EncryptionUtils.encrypt(val, node);
                }

                props.setProperty(String.format("Param%d_Name_%d", currPidx, i + 1), name);
                props.setProperty(String.format("Param%d_Value_%d", currPidx, i + 1), val);
                props.setProperty(String.format("Param%d_Type_%d", currPidx, i + 1), type);
            }

            ++pidx;
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SpecifyParametersModel> {
        @Nonnull
        public String getDisplayName() {
            return "Specify test parameters model";
        }

        public List<EnumDescription> getParamTypesGUI() {
            return paramTypesGUI;
        }

        public List<EnumDescription> getParamTypesAPI() {
            return paramTypesAPI;
        }

        public Map<String, List<EnumDescription>> getMapping() {
            return mapping;
        }

        public int getNumOfTypes() {
            return NUM_OF_TYPES;
        }
    }

}
