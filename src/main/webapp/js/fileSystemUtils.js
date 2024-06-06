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
document.addEventListener('DOMContentLoaded', function() {
    var checkbox1 = document.getElementById('checkBox1');

    if (checkbox1.checked) {
        document.getElementsByName("fsTestType")[0].disabled = false;
        document.getElementsByName("selectedNode")[0].disabled = false;
    } else {
        document.getElementsByName("fsTestType")[0].disabled = true;
        document.getElementsByName("selectedNode")[0].disabled = true;
    }

    var selectIndex = document.getElementsByName("fsTestType")[0].selectedIndex;
    var selectValue = document.getElementsByName("fsTestType")[0].options[selectIndex].text;
    if (selectValue === "Rerun the entire set of tests" || selectValue === "Rerun only failed tests") {
        selectCleanupTest("none");
    } else {
        selectCleanupTest("block");
    }
}, false);

function enableCombobox(object) {
    if (object.checked) {
        document.getElementsByName("fsTestType")[0].disabled = false;
        document.getElementsByName("selectedNode")[0].disabled = false;
        //document.getElementById("checkBox2").disabled=false;
    } else {
        document.getElementsByName("fsTestType")[0].disabled = true;
        document.getElementsByName("selectedNode")[0].disabled = true;
        //document.getElementById("checkBox2").disabled=true;
    }
}

function fileSelected(input) {
    var selectIndex = document.getElementById('testTypeSelect').selectedIndex;
    var selectValue = document.getElementById('testTypeSelect').options[selectIndex].text;
    if (selectValue === "Rerun the entire set of tests" || selectValue === "Rerun only failed tests") {
        document.getElementsByName("uftSettingsModel.cleanupTest")[0].value = input.files[0].name;
    } else {
        addCleanupTest(input.files[0].name);
    }
}

function selectCleanupTest(displayStyle) {
    document.getElementById('clearBtn').style.display = displayStyle;
    document.getElementById('clear').style.display = displayStyle;
    document.getElementById('copyPasteBtn').style.display = displayStyle;
    document.getElementById('infoMessage').style.display = displayStyle;
    document.getElementById('testsTable').style.display = displayStyle;
}

function selectValueCombo(selectObj) {
    var selectIndex = selectObj.selectedIndex;
    var selectValue = selectObj.options[selectIndex].text;

    if (selectValue === "Rerun the entire set of tests" || selectValue === "Rerun only failed tests") {
        selectCleanupTest("none");
    } else {
        selectCleanupTest("block");

        if (selectValue === "Of any of the build's tests") {
            selectCleanupTest("hidden");
            //document.getElementById("checkBox2").disabled = false;
        } else {
            selectCleanupTest("visible");
            // document.getElementById("checkBox2").disabled = true;
        }
    }
}

function copyPasteRerunSettings() {
    var checkedTests = document.getElementsByName("rerunSettingsModels.checked");
    var rerunsList = document.getElementsByName('rerunSettingsModels.numberOfReruns');
    var cleanupTestList = document.getElementsByName('rerunSettingsModels.cleanupTest');
    var index = 0;

    var cleanupTest = document.getElementsByName("uftSettingsModel.cleanupTest")[0].value;
    var numberOfReruns = document.getElementsByName("numberOfReruns")[0].value;

    rerunsList.forEach(function (element) {
        if (checkedTests[index].checked) {
            element.value = numberOfReruns;
        }
        index = index + 1;
    });

    index = 0;
    cleanupTestList.forEach(function (element) {
        if (checkedTests[index].checked) {
            element.value = cleanupTest;
        }
        index = index + 1;
    });
}

function addCleanupTest(cleanupTest) {
    var selectCleanupLists = document.getElementsByName("rerunSettingsModels.cleanupTests");

    selectCleanupLists.forEach(function (element) {
        var option = document.createElement("option");
        option.text = cleanupTest;
        option.value = cleanupTest;
        element.add(option);
    });
}

function clearRerunSettings() {
    var checkBoxes = document.getElementsByName("rerunSettingsModels.checked");
    checkBoxes.forEach(function (element) {
        element.checked = false;
    });

    var numberOfRerunsFields = document.getElementsByName("rerunSettingsModels.numberOfReruns");
    numberOfRerunsFields.forEach(function (element) {
        element.value = 0;
    });

    var selectCleanupLists = document.getElementsByName("rerunSettingsModels.cleanupTest");
    selectCleanupLists.forEach(function (element) {
        element.value = "";
    });
}

function checkIfPipelineAndUpdateHelpMsg(msg) {
    setTimeout(function () {
        if (window.location.href.indexOf("pipeline-syntax") > 0) {
            let helpText = document.getElementById("helpTextMsg");

            // verify if the element is found, otherwise an exception will occur which blocks the page loading
            if (helpText) {
                helpText.innerHTML = msg;
            }
        }
    }, 200);
}
