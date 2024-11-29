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
package com.microfocus.application.automation.tools.common.utils;

import java.util.Arrays;
import java.util.List;

public enum OperatingSystem {
    LINUX,
    WINDOWS,
    MAC;

    private static final List<String> POSIX_NAMES = Arrays.asList(
            "linux", "os/2", "irix", "hp-ux", "aix", "soalris", "sunos");
    private static String os = System.getProperty("os.name").toLowerCase();
    private static boolean windows = os.contains(WINDOWS.name().toLowerCase());
    private static boolean mac = os.contains(MAC.name().toLowerCase());
    private static boolean linux = POSIX_NAMES.contains(os.toLowerCase());

    public static String getOs() {
        return os;
    }

    public static void refreshOsVariablesForSlave() {
        os = System.getProperty("os.name").toLowerCase();
        windows = os.contains(WINDOWS.name().toLowerCase());
        mac = os.contains(MAC.name().toLowerCase());
        linux = POSIX_NAMES.contains(os.toLowerCase());
    }

    public boolean equalsCurrentOs() {
        refreshOsVariablesForSlave();
        return linux || os.contains(this.name().toLowerCase());
    }

    public static boolean isWindows() {
        return windows;
    }

    public static boolean isMac() {
        return mac;
    }

    public static boolean isLinux() {
        return linux;
    }
}