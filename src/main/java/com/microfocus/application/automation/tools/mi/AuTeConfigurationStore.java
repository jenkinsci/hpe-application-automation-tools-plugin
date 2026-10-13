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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.mi;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Per shared space AuTe default configuration, kept on the Jenkins controller at
 * {@code <JENKINS_HOME>/aute-config/<configurationId>/<spaceId>/default-configuration.json}
 * and edited by the Jenkins administrator.
 */
public final class AuTeConfigurationStore {
    private static final String FILE_NAME = "default-configuration.json";

    /** Path of each build's copy, relative to the workspace; also its artifact path. */
    public static final String SNAPSHOT_PATH = FILE_NAME;

    private static final String EMPTY_TEMPLATE = """
            {
              "BROWSER": {},
              "AGENT": {},
              "LLM_MODELS": {},
              "APLS_LICENSE": {},
              "ENVIRONMENT": {}
            }
            """;

    private AuTeConfigurationStore() {
    }

    /** Creates the empty template if it is missing; an existing file is never overwritten. */
    public static Path ensureTemplate(String configurationId, String spaceId) throws IOException {
        return ensureTemplate(Jenkins.get().getRootDir().toPath(), configurationId, spaceId);
    }

    static synchronized Path ensureTemplate(Path jenkinsHome, String configurationId, String spaceId)
            throws IOException {
        requireSafeFolderName(configurationId);
        requireSafeFolderName(spaceId);
        Path file = jenkinsHome.resolve("aute-config").resolve(configurationId).resolve(spaceId).resolve(FILE_NAME);
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, EMPTY_TEMPLATE, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        return file;
    }

    public static byte[] readTemplate(String configurationId, String spaceId) throws IOException {
        return Files.readAllBytes(ensureTemplate(configurationId, spaceId));
    }

    // The ids become folder names, so reject anything that could point outside aute-config.
    private static void requireSafeFolderName(String value) throws IOException {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IOException("Invalid AuTe configuration folder name: " + value);
        }
    }
}