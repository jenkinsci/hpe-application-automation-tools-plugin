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

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class AuTeConfigurationStoreTest {
    private static final String DOCUMENTATION_FILE_NAME = "default-configuration-example.md";

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void createsEmptySectionsAndPreservesAdministratorEdits() throws Exception {
        Path root = temporary.getRoot().toPath();
        Path file = AuTeConfigurationStore.ensureTemplate(root, "connection", "1001");
        JSONObject template = (JSONObject) JSONValue.parse(Files.readString(file));
        assertEquals(Set.of("BROWSER", "AGENT", "LLM_MODELS", "APLS_LICENSE", "ENVIRONMENT"), template.keySet());
        for (Object section : template.values()) {
            assertEquals(new JSONObject(), section);
        }
        assertBundledDocumentation(file);
        Files.writeString(file, "{\"administrator\":true}");
        Path documentation = file.resolveSibling(DOCUMENTATION_FILE_NAME);
        Files.writeString(documentation, "Administrator notes");
        AuTeConfigurationStore.ensureTemplate(root, "connection", "1001");
        assertEquals("{\"administrator\":true}", Files.readString(file));
        assertEquals("Administrator notes", Files.readString(documentation));
        Path otherSpace = AuTeConfigurationStore.ensureTemplate(root, "connection", "1002");
        Path otherConnection = AuTeConfigurationStore.ensureTemplate(root, "other", "1001");
        assertNotEquals(file, otherSpace);
        assertNotEquals(file, otherConnection);
        assertBundledDocumentation(otherSpace);
        assertBundledDocumentation(otherConnection);
    }

    @Test
    public void addsMissingDocumentationToExistingConfiguration() throws Exception {
        Path root = temporary.getRoot().toPath();
        Path folder = Files.createDirectories(root.resolve("aute-config").resolve("connection").resolve("1001"));
        Path file = Files.writeString(folder.resolve("default-configuration.json"), "{\"administrator\":true}");

        assertEquals(file, AuTeConfigurationStore.ensureTemplate(root, "connection", "1001"));
        assertEquals("{\"administrator\":true}", Files.readString(file));
        assertBundledDocumentation(file);
        Files.delete(file.resolveSibling(DOCUMENTATION_FILE_NAME));
        AuTeConfigurationStore.ensureTemplate(root, "connection", "1001");
        assertBundledDocumentation(file);
    }

    @Test
    public void recreatesMissingConfigurationWithoutOverwritingDocumentation() throws Exception {
        Path root = temporary.getRoot().toPath();
        Path file = AuTeConfigurationStore.ensureTemplate(root, "connection", "1001");
        Path documentation = file.resolveSibling(DOCUMENTATION_FILE_NAME);
        Files.writeString(documentation, "Administrator notes");
        Files.delete(file);

        AuTeConfigurationStore.ensureTemplate(root, "connection", "1001");
        assertEquals(5, ((JSONObject) JSONValue.parse(Files.readString(file))).size());
        assertEquals("Administrator notes", Files.readString(documentation));
    }

    @Test
    public void concurrentCreationPublishesOneCompleteTemplate() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            Callable<Path> create = () -> {
                Path file = AuTeConfigurationStore.ensureTemplate(temporary.getRoot().toPath(), "connection", "1001");
                assertBundledDocumentation(file);
                return file;
            };
            var results = executor.invokeAll(Collections.nCopies(12, create));
            for (var result : results) {
                assertEquals(5, ((JSONObject) JSONValue.parse(Files.readString(result.get()))).size());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void rejectsPathTraversal() throws Exception {
        Path root = temporary.getRoot().toPath();
        assertThrows(IOException.class, () -> AuTeConfigurationStore.ensureTemplate(root, "../outside", "1001"));
        assertThrows(IOException.class, () -> AuTeConfigurationStore.ensureTemplate(root, "connection", "../outside"));
        assertFalse(Files.exists(root.resolve("aute-config")));
    }

    private static void assertBundledDocumentation(Path template) throws IOException {
        try (var input = AuTeConfigurationStore.class.getResourceAsStream(DOCUMENTATION_FILE_NAME)) {
            assertNotNull("Bundled configuration guide must be available", input);
            byte[] expected = input.readAllBytes();
            assertFalse("Bundled configuration guide must not be empty", expected.length == 0);
            assertArrayEquals(expected, Files.readAllBytes(template.resolveSibling(DOCUMENTATION_FILE_NAME)));
        }
    }
}