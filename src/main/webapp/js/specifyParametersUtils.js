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
// has to be declared like this, because it has to be globally accessible and multiple steps can be added in a single job, which would throw duplicate exception
// holder, which contain all the valid parameter input types
if (typeof selectableTypeList === "undefined") {
    selectableTypeList = '';
}

if (typeof BUILDER_SELECTOR === "undefined") {
    BUILDER_SELECTOR = "div[name='builder'][descriptorid*='com.microfocus.application.automation.tools.run.RunFrom']";
}

function setupParamSpecification(hasConfigPermission) {
    document.body.style.cursor = "wait";
    let main = null;
    if (document.location.href.indexOf("pipeline-syntax") > 0) {
        main = document;
    } else if (document.currentScript) {
        main = document.currentScript.parentElement.closest(BUILDER_SELECTOR);
    }
    if (main == null) {
        setTimeout(() => { getFSContainerAndStartListening4Params(hasConfigPermission, 0); }, 500);
    } else {
        setTimeout(() => {
            try {
                startListening4Params(main, hasConfigPermission);
            } catch(e) {
                console.error(e);
            } finally {
                document.body.style.cursor = "";
            }
        }, 200);
    }
}

function getFSContainerAndStartListening4Params(hasConfigPermission, idxOfRetry) {
    let divs = document.querySelectorAll(BUILDER_SELECTOR);
    if (divs == null || divs.length == 0) {
        if (idxOfRetry > 5) {
            console.error("Failed to initialize Specific Params controls! Please retry again.");
            document.body.style.cursor = "";
        } else {
            console.log("Retry to initialize Specific Params controls ...");
            setTimeout(() => { getFSContainerAndStartListening4Params(++idxOfRetry); }, 500);
        }
    } else {
        try {
            startListening4Params(divs[divs.length - 1]);
        } catch (e) {
            console.error(e);
        } finally {
            document.body.style.cursor = "";
        }
    }
}

function startListening4Params(main, hasConfigPermission) {
    if (main == null) {
        console.error("Failed to initialize Specific Params controls! Please retry or refresh the page.");
        return;
    }
    loadParamInputs(main, hasConfigPermission);

    const btnAddNewParam = main.querySelector("button[name='addNewParamBtn']");
    if (btnAddNewParam) {
        if (hasConfigPermission) {
            btnAddNewParam.addEventListener('click', () => { addNewParam(main, true); });
        } else {
            btnAddNewParam.disabled = true;
            btnAddNewParam.style.cursor = "not-allowed";
            btnAddNewParam.style.pointerEvents = "auto";
        }
    } else {
        console.warn("Add parameter button is missing.");
    }

    if (hasConfigPermission) {
        const updateMaxNumber4Spinner = (testInput) => {
            const rowInputs = main.querySelectorAll(".test-param > div > .num-of-test-spinner");
            const newMax = testInput.value.split("\n").filter(row => row !== "").length;
            rowInputs.forEach(rowInput => rowInput.setAttribute("max", newMax === 0 ? 1 : newMax.toString()));
        }
        const updateTest = (container, spinner, testInput) => {
            const testLabel = spinner.parentElement.nextElementSibling.querySelector(".test-label");
            if (spinner.value === '') {
                testLabel.value = "";
                return;
            }
            testLabel.value = testInput.value.split("\n")[parseInt(spinner.value) - 1] || "Please, specify tests first";
        }

        let testInput;
        const prepareTestInput = () => {
            testInput = queryTestInput(main);
            if (testInput) {
                testInput.addEventListener("change", () => {
                    updateMaxNumber4Spinner(testInput);
                    rowInputs.forEach((rowInput) => {
                        updateTest(main, rowInput, testInput);
                    });
                });
                testInput.dispatchEvent(new Event("change"));
            } else {
                console.warn("Test input text area is missing.");
            }
        }

        const rowInputs = main.querySelectorAll(".test-param > div > .num-of-test-spinner");
        prepareTestInput();
        rowInputs.forEach(rowInput => {
            rowInput.addEventListener("click", () => {
                updateTest(main, rowInput, testInput);
            });
            rowInput.addEventListener("change", () => {
                updateTest(main, rowInput, testInput);
            })
        });

        const chkAreParamsEnabled = main.querySelector("input[name='areParametersEnabled']");
        if (chkAreParamsEnabled) {
            chkAreParamsEnabled.addEventListener("click", () => cleanParamInput(main, true));
        }

        const expandTestsFieldBtn = main.querySelector(".expanding-input__button [type='button']");
        expandTestsFieldBtn && expandTestsFieldBtn.addEventListener("click", () => {
            prepareTestInput();
        });
    }
}

function queryTestInput(container) {
    return container.querySelector("textarea[name='fsTests'], input[name='fsTests'], textarea[name='runfromalm.almTestSets'], input[name='runfromalm.almTestSets']");
}

function generateAndPutJSONResult(container) {
    const paramsContainer = container.querySelector("ul[name='testParams']");

    const inputs = paramsContainer.querySelectorAll("li[name='testParam']");
    let inputJSON = [];

    const strParamRes = paramsContainer.parentElement.querySelector("input.json-params");

    if (!strParamRes) return console.warn("Param input JSON result hidden field is missing, reload the page.");

    inputs.forEach(elem => {
        let curr = {};
        const idx = elem.dataset.index;
        curr.index = elem.querySelector(`#paramInputRow_${idx}`).value;
        const name = curr.name = elem.querySelector(`#paramInputName_${idx}`).value;

        if (name !== "") {
            curr.type = elem.querySelector(`#paramInputType_${elem.dataset.index}`).value;

            const val = elem.querySelector(`#paramInputValue_${elem.dataset.index}`);
            if (curr.type === "Boolean") {
                curr.value = val.checked;
            } else if (curr.type === "Date" || curr.type === "DateTime") {
                const date = new Date(val.value);
                curr.value = `${date.getDate() < 10 ? '0' + date.getDate() : date.getDate()}/${date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth()}/${date.getFullYear()}`;
            } else {
                curr.value = val.value;
            }

            inputJSON.push(curr);
        }
    });

    strParamRes.value = normalizeJsonFormat(JSON.stringify(inputJSON));
}

function cleanParamInput(container, hasConfigPermission) {
    if (this.checked) {
        loadParamInputs(container, hasConfigPermission);
    } else {
        const strParamRes = container.querySelector("input.json-params");
        if (!strParamRes) return console.warn("Param input JSON result hidden field is missing, reload the page.");
        strParamRes.value = normalizeJsonFormat(JSON.stringify([]));
    }
}

function addNewParam(container, hasConfigPermission) {
    const paramContainer = container.querySelector("ul[name='testParams']");
    const params = paramContainer.querySelectorAll("li[name='testParam']") || [];
    const nextIdx = params.length !== 0 ? parseInt(Array.from(params).reduce((prev, curr) => {
        if (parseInt(prev.dataset.index) > parseInt(curr.dataset.index)) return prev;

        return curr;
    }).dataset.index) + 1 : 1;

    let maxNumOfTests = 1;
    let testInput = queryTestInput(container);
    if (testInput) {
        maxNumOfTests = testInput.value.split("\n").filter(row => row !== "").length.toString();
    } else {
        console.warn("Test input field is missing.");
    }

    let htmlDelBtn = hasConfigPermission ?
        `<span class="yui-button danger" id="delParamInput_${nextIdx}" name="delParam"><span class="first-child"><button type="button" tabindex="0">&#9747;</button></span></span>`:
        "";
    const elem = `
        <li class="test-param" name="testParam" data-index="${nextIdx}">
            <div>
                <input class="jenkins-input setting-input num-of-test-spinner" name="paramInput" id="paramInputRow_${nextIdx}" min="1" max="${maxNumOfTests === 0 ? 1 : maxNumOfTests}" type="number" required="required" />
            </div>
            <div>
                <input class="jenkins-input setting-input test-label" name="paramInput" id="paramInputTest_${nextIdx}" type="text" value="" disabled />
            </div>
            <div>
                <input class="jenkins-input setting-input" name="paramInput" id="paramInputName_${nextIdx}" type="text" required="required" />
            </div>
            <div>
                <input class="jenkins-input setting-input" name="paramInput" id="paramInputValue_${nextIdx}" type="text"/>
            </div>
            <div>
                <select name="paramInput" id="paramInputType_${nextIdx}">
                    ${selectableTypeList}
                </select>
            </div>  
            ${htmlDelBtn}
        </li>
        `;

    paramContainer.insertAdjacentHTML("beforeend", elem);

    const testLabel = paramContainer.querySelector(`#paramInputTest_${nextIdx}`);
    const spinner = paramContainer.querySelector(`#paramInputRow_${nextIdx}`);

    const handleSpinner = () => {
        if (spinner.value === '') {
            testLabel.value = "";
            return;
        }

        testLabel.value = queryTestInput(container).value.split("\n")[parseInt(spinner.value) - 1] || "Please, specify tests first";
    };
    spinner.addEventListener("click", () => {
        handleSpinner();
    });
    spinner.addEventListener("change", () => {
        handleSpinner();
    });

    spinner.dispatchEvent(new Event("change"));

    Array.from(paramContainer.querySelectorAll(`[name='paramInput']`)).filter(input => input.getAttribute("id").endsWith("_" + nextIdx.toString()))
        .forEach(input => input.addEventListener("change", () => generateAndPutJSONResult(container)));

    const delButton = paramContainer.querySelector(`#delParamInput_${nextIdx} > span > button`);
    delButton?.addEventListener("click", () => deleteParam(delButton, container));

    const typeField = paramContainer.querySelector(`#paramInputType_${nextIdx}`);
    const valueField = paramContainer.querySelector(`#paramInputValue_${nextIdx}`);
    typeField.addEventListener("change", () => {
        valueField.value = "";
        valueField.setAttribute("type", map4TypeAssociations[typeField.value] || "text");
    });
}

function deleteParam(elem, container) {
    elem.parentNode.parentNode.parentNode.remove();
    generateAndPutJSONResult(container);
}

// has to be declared like this, because it has to be globally accessible and multiple steps can be added in a single job, which would throw duplicate exception
if (typeof map4TypeAssociations === "undefined") {
    map4TypeAssociations = {
        String: 'text',
        Number: 'number',
        Boolean: 'checkbox',
        Password: 'password',
        Date: 'date',
        Any: 'text',
        Float: 'number',
        Double: 'number',
        Decimal: 'number',
        Long: 'number',
        DateTime: 'date',
        Int: 'number'
    };
}

function loadParamInputs(container, hasConfigPermission) {
    const paramResultStr = container.querySelector("input.json-params");

    // on some browsers the value may return with extra-quotes
    let params = paramResultStr.value;

    if (params === "" || params === "[]" || params === "\"[]\"") return;

    let json;
    try {
        json = JSON.parse(normalizeJsonFormat(params));
    } catch (e) {
        json = JSON.parse("[]");
    }

    // has to be an object to be valid JSON input, otherwise because of security policies the JSON was altered
    if (typeof(json) === "string") json = JSON.parse("[]");

    for (let i = 0; i < json.length; ++i) addNewParam(container, hasConfigPermission);

    const testParams = container.querySelectorAll("li[name='testParam']");

    for (let i = 0; i < json.length; ++i) {
        const currElem = testParams[i];
        const currElemVal = json[i];

        currElem.querySelector(`#paramInputRow_${currElem.dataset.index}`).value = currElemVal["index"] || 1;
        currElem.querySelector(`#paramInputName_${currElem.dataset.index}`).value = currElemVal["name"] || "";
        const valueField = currElem.querySelector(`#paramInputValue_${currElem.dataset.index}`)
        const typeField = currElem.querySelector(`#paramInputType_${currElem.dataset.index}`);
        typeField.value = currElemVal["type"] || "String";

        valueField.setAttribute("type", map4TypeAssociations[typeField.value] || "text");
        if (typeField.value === "Boolean") {
            valueField.checked = currElemVal["value"] || false;
        } else if (typeField.value === "Date" || typeField.value === "DateTime") {
            const date = new Date(currElemVal["value"].split("/").reverse().join("-")) || Date.now();
            valueField.value = `${date.getFullYear()}-${date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1)}-${date.getDate() < 10 ? '0' + date.getDate() : date.getDate()}`;
        } else {
            valueField.value = currElemVal["value"] || "";
        }
    }
}

function addToSelectableTypeList(type, typeListLength) {
    // if there are more build steps than one, do not populate the dropdown
    // if the dropdown is already populated, do not populate it again
    if (selectableTypeList.split("</option>").length - 1 >= typeListLength) return;

    selectableTypeList += `<option value="${type}">${type}</option>`;
}

function addSeparatorToTypeList(group, idx, groupsLength) {
    // do not populate the list again, if already populated
    // happens when multiple jobs with parameters table is added to the base job
    if (selectableTypeList.split("</optgroup>").length - 1 >= groupsLength) return;

    // special command, called with idx of -1 and group of null if separator needed
    if (idx === -1 && group === null) {
        selectableTypeList += '</optgroup>';
        return;
    }

    selectableTypeList += `<optgroup label="${group}">`;
}

function normalizeJsonFormat(str) {
    // because of certain security policies, on some servers the special characters could be escaped twice, we need to parse them twice
    let ret = str;
    if (str.endsWith("\"")) ret = JSON.parse(ret);
    return ret;
}