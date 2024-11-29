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
if (typeof RUN_FROM_FS_BUILDER_SELECTOR == "undefined") {
	RUN_FROM_FS_BUILDER_SELECTOR = 'div[name="builder"][descriptorid="com.microfocus.application.automation.tools.run.RunFromFileBuilder"]';
}
//const templates = { "regions": [ "Europe (Frankfurt)" ], "os": [ "Windows Server 2022" ], "browsers": [ { "type": "Chrome", "versions": [ { "version": "117", "tag": "117" }, { "version": "117", "tag": "latest" }, { "version": "116", "tag": "latest-1" }, { "version": "116", "tag": "116" }, { "version": "115", "tag": "latest-2" }, { "version": "115", "tag": "115" } ] }, { "type": "Edge", "versions": [ { "version": "117", "tag": "117" }, { "version": "117", "tag": "latest" }, { "version": "116", "tag": "latest-1" }, { "version": "116", "tag": "116" }, { "version": "115", "tag": "latest-2" }, { "version": "115", "tag": "115" } ] }, { "type": "Firefox", "versions": [ { "version": "117", "tag": "117" }, { "version": "117", "tag": "latest" }, { "version": "116", "tag": "latest-1" }, { "version": "116", "tag": "116" }, { "version": "115", "tag": "latest-2" }, { "version": "115", "tag": "115" } ] } ] }

function getDigitalLab(divMain) {
    const dl = divMain.querySelector("#mobileSpecificSection");
    const o = { serverName: dl.querySelector('select[name="mcServerName"]').value,
        userName: "",
        password: "",
        tenantId: "",
        execToken: "",
        authType: dl.querySelector('input[name$="authModel"]:checked').value,
        useProxy: dl.querySelector('input[name="proxySettings"]').checked,
        proxyAddress: "",
        useProxyAuth: false,
        proxyUserName: "",
        proxyPassword: "",
        recreateJob: dl.querySelector('input[name="recreateJob"]').checked,
        jobId: dl.querySelector('input[name="fsJobId"]').value,
        deviceInfo: dl.querySelector(".device-info-section")
    };
    if (o.authType == "base") {
        o.userName = dl.querySelector('input[name="mcUserName"]').value;
        o.password = dl.querySelector('input[name="mcPassword"]').value;
        o.tenantId = dl.querySelector('input[name="mcTenantId"]').value;
    } else {
        o.execToken = dl.querySelector('input[name="mcExecToken"]').value;
    }
    if (o.useProxy) {
        o.proxyAddress = dl.querySelector('input[name="fsProxyAddress"]').value;
        o.useProxyAuth = dl.querySelector('input[name="fsUseAuthentication"]').checked;
        if (o.useProxyAuth) {
            o.proxyUserName = dl.querySelector('input[name="fsProxyUserName"]').value;
            o.proxyPassword = dl.querySelector('input[name="fsProxyPassword"]').value;
        }
    }

    return o;
}

function startLoadInfo(a, b, path) {
    triggerBtnState(b, true);
    setTimeout( async () => {
        await loadInfo(a, b, path)}, 100);
}

async function triggerBtnState(b, disabled) {
    b.disabled = disabled;
    if (b.name == "cloudBrowserLab") {
        b.value = disabled ? "Loading ..." : "Browser settings";
    } else if (b.name == "digitalLabWizard") {
        b.value = disabled ? "Loading ..." : "Wizard";
    } else if (b.name == "env-wizard") {
        b.value = disabled ? "Loading ..." : "Environment wizard";
    }
}
async function loadInfo(a, b, path) {
    let dl = {};
    try {
        const divMain = b.parentElement.closest(RUN_FROM_FS_BUILDER_SELECTOR);
        const err = b.parentElement.querySelector(".error-msg");
        dl = getDigitalLab(divMain);
        err.style.display = "none";
        const isMcCredentialMissing = ("base" == dl.authType ? (dl.userName.trim() == "" || dl.password.trim() == "") : dl.execToken.trim() == "");

        const isProxyAddressRequiredButMissing = dl.useProxy && dl.proxyAddress.trim() == "";
        const isProxyCredentialRequiredButMissing = dl.useProxyAuth && (dl.proxyUserName.trim() == "" || dl.proxyPassword.trim() == "");
        if (isMcCredentialMissing || isProxyAddressRequiredButMissing || isProxyCredentialRequiredButMissing) {
            err.style.display = "inline-block";
            await triggerBtnState(b, false);
            return;
        }

        if (b.name == "cloudBrowserLab") {
            await loadBrowserLabInfo(a, b, dl, path, err);
        } else if (b.name == "digitalLabWizard") {
            await loadMobileInfo(a, b, dl, err);
        }
    } catch (e) {
        console.error(e);
        err.style.display = "inline-block";
        await triggerBtnState(b, false);
    }
 }

async function fillAndShowDDLs(b, div, dlg) {
    await loadOsDDL(dlg, div.oses);
    await loadBrowserAndVersionDDLs(dlg, div.browsers);
    await loadRegionDDL(dlg, div.regions);
    await triggerBtnState(b, false);
    dlg.style.display = "block";
}
async function loadBrowserLabInfo(a, b, o, path, err) {
    const div = b.parentElement.closest("#mobileSpecificSection");
    const dlg = await generateModalDialog(path);
    div.appendChild(dlg);
    if (div.browsers?.length) {
        await fillAndShowDDLs(b, div, dlg);
    } else {
        await a.getBrowserLab(o.serverName, o.execToken, o.useProxyAuth, o.proxyAddress, o.proxyUserName, o.proxyPassword, async (response) => {
            try {
                if (response?.responseJSON) {
                    const json = response.responseJSON;
                    div.oses = json.os;
                    div.browsers = json.browsers;
                    div.regions = json.regions;
                    await fillAndShowDDLs(b, div, dlg);
                } else {
                    err.style.display = "inline-block";
                    await triggerBtnState(b, false);
                }
            } catch (e) {
                console.error(e);
                await triggerBtnState(b, false);
            }
        });
    }
}

async function loadMobileInfo(a, b, o, err) {
    let baseUrl = "";
    await a.getMcServerUrl(o.serverName, async (r) => {
        baseUrl = r.responseObject();
        if (baseUrl) {
            baseUrl = baseUrl.trim().replace(/[\/]+$/, "");
        } else {
            err.style.display = "inline-block";
            await triggerBtnState(b, false);
            return;
        }
        let prevJobId = o.recreateJob ? "" : o.jobId;
        await a.getJobId(baseUrl, o.userName, o.password, o.tenantId, o.execToken, o.authType, o.useProxyAuth, o.proxyAddress, o.proxyUserName, o.proxyPassword, prevJobId, async (response) => {
            let map = response.responseObject();

            let jobId = '';
            let tenantId = '';
            let isSaaS = false;
            //First need to check the return type(not sure if java map can automatically covert to js map)
            if(map.hasOwnProperty("jobId")) {
                jobId = map["jobId"];
            }
            if(map.hasOwnProperty("TENANT_ID_COOKIE")) {
                tenantId = map["TENANT_ID_COOKIE"];
            }
            if(map.hasOwnProperty("isSaaS")) {
                isSaaS = map["isSaaS"];
            }

            if (jobId == null) {
                err.style.display = "inline-block";
                await triggerBtnState(b, false);
                return;
            }
            //hide the error message after success login
            err.style.display = "none";
            let openedWindow = window.open('/', 'test parameters', 'height=820,width=1130');
            openedWindow.location.href = 'about:blank';
            if (isSaaS) {
                openedWindow.location.href = baseUrl + "/integration8/en/#/main/wizard?TENANTID=" + tenantId + "&jobId=" +  jobId + "&displayUFTMode=true";
            } else {
                openedWindow.location.href = baseUrl + "/integration8/en/#/login?jobId=" + jobId + "&displayUFTMode=true";
            }

            const msgCallback = async (ev) => {
                if (ev?.data == "mcCloseWizard") {
                    await a.populateAppAndDevice(baseUrl, o.userName, o.password, o.tenantId, o.execToken, o.authType, o.useProxyAuth, o.proxyAddress, o.proxyUserName, o.proxyPassword, jobId, async (app) => {
                        let jobInfo = app.responseObject();
                        let deviceId = "", OS = "", manufacturerAndModel = "", targetLab = "";
                        if (jobInfo['deviceJSON']) {
                            if (jobInfo['deviceJSON']['deviceId']) {
                                deviceId = jobInfo['deviceJSON']['deviceId'];
                            }
                            if (jobInfo['deviceJSON']['OS']) {
                                OS = jobInfo['deviceJSON']['OS'];
                            }
                            if (jobInfo['deviceJSON']['manufacturerAndModel']) {
                                manufacturerAndModel = jobInfo['deviceJSON']['manufacturerAndModel'];
                            }
                        }
                        if (jobInfo['deviceCapability']) {
                            if (jobInfo['deviceCapability']['OS']) {
                                OS = jobInfo['deviceCapability']['OS'];
                            }
                            if (jobInfo['deviceCapability']['manufacturerAndModel']) {
                                manufacturerAndModel = jobInfo['deviceCapability']['manufacturerAndModel'];
                            }
                            if (jobInfo['deviceCapability']['targetLab']) {
                                targetLab = jobInfo['deviceCapability']['targetLab'];
                            }
                        }
                        const div = o.deviceInfo;
                        div.querySelector('input[name="fsDeviceId"]').value = deviceId;
                        div.querySelector('input[name="fsOs"]').value = OS;
                        div.querySelector('input[name="fsManufacturerAndModel"]').value = manufacturerAndModel;
                        div.querySelector('input[name="fsTargetLab"]').value = targetLab ?? "";
                        div.querySelector('input[name="fsLaunchAppName"]').value = jobInfo['definitions']['launchApplicationName'] ?? "";
                        div.querySelector('input[name="fsInstrumented"]').value = jobInfo['definitions']['instrumented'] ?? "";
                        div.querySelector('input[name="fsAutActions"]').value = jobInfo['definitions']['autActions'] ?? "";
                        div.querySelector('input[name="fsDevicesMetrics"]').value = jobInfo['definitions']['deviceMetrics'] ?? "";
                        div.querySelector('textarea[name="fsExtraApps"]').value = jobInfo['extraApps'] ?? "";
                        div.querySelector('input[name="fsJobId"]').value = jobInfo['jobUUID'];
                        await triggerBtnState(b, false);
                        err.style.display = "none";
                        window.removeEventListener("message", msgCallback, false);
                        openedWindow.close();
                    });
                }
            };
            window.addEventListener("message", msgCallback, false);

            function checkChild() {
                if (openedWindow?.closed) {
                    clearInterval(timer);
                    triggerBtnState(b, false);
                }
            }

            let timer = setInterval(checkChild, 500);
        });
    });
}

function hideAndMoveAdvancedBody(_id) {
    const tBody = document.querySelector("#" + _id).parentNode; // initial advanced block content
    const initialAdvancedBlock = tBody.previousSibling; // advanced link button block and here was hidden the initial advanced block content
    initialAdvancedBlock.querySelector(".advancedBody").appendChild(tBody); // moves the initial advanced block content back to the hidden block
    initialAdvancedBlock.querySelector(".advancedLink").style.display = ""; // enables once again the advanced link
}

async function loadRegionDDL(dlg, regions) {
    const ddl = dlg.querySelector('select[name="cldBrowserRegion"]');
    await loadDDL(ddl, regions.sort());
}

async function loadOsDDL(dlg, arr) {
    const ddl = dlg.querySelector('select[name="cldBrowserOS"]');
    await loadDDL(ddl, arr);
}

async function loadBrowserAndVersionDDLs(dlg, browsers) {
    const ddlB = dlg.querySelector('select[name="cldBrowser"]');
    const ddlV = dlg.querySelector('select[name="cldBrowserVer"]');
    let mapBV = new Map();

    browsers.forEach((obj) => {
        mapBV.set(obj.type, obj.versions?.map(v => v.tag).sort());
    });
    await loadBrowsersDDL(ddlB, mapBV);

    ddlB.onchange = (e) => {
        const arr = ddlB.selectedOptions[0]?.versions;
        if (arr?.length) {
            loadDDL(ddlV, arr);
        } else {
            ddlV.length = 0;
        }
    };

    const arr = ddlB.selectedOptions[0]?.versions;
    arr?.length && await loadDDL(ddlV, arr);
}

async function loadBrowsersDDL(ddl, map) {
    ddl.length = 0;
    if (map?.size) {
        let x = 0;
        map.forEach((val, key) => {
            let opt = new Option(key, key, 0 == x++);
            opt.versions = val;
            ddl[ddl.length] = opt;
        });
    }
}

async function loadDDL(ddl, arr) {
    ddl.length = 0;
    if (arr?.length) {
        for (let i = 0; i < arr.length; ++i) {
            ddl[ddl.length] = new Option(arr[i], arr[i], i == 0);
        }
    }
}

function hideCloudBrowserModal(b) {
    const div = b.parentElement.closest(".config-table-top-row");
    let dlg = div.querySelector('[name="cloudBrowsersModal"]');
    dlg.remove();
}

function onSaveCloudBrowser(b) {
    try {
        const div = b.parentElement.closest(".config-table-top-row");
        let dlg = div.querySelector('[name="cloudBrowsersModal"]');
        const os = div.querySelector('[name="cloudBrowserOs"]');
        os.value = dlg.querySelector('select[name="cldBrowserOS"]').value;
        const type = div.querySelector('[name="cloudBrowserType"]');
        type.value = dlg.querySelector('select[name="cldBrowser"]').value;
        const ver = div.querySelector('[name="cloudBrowserVersion"]');
        ver.value = dlg.querySelector('select[name="cldBrowserVer"]').value;
        const reg = div.querySelector('[name="cloudBrowserRegion"]');
        reg.value = dlg.querySelector('select[name="cldBrowserRegion"]').value;
        dlg.remove();
    } catch(e) {
        console.error(e);
    }
}

async function loadCssIfNotAlreadyLoaded(path) {
    const ss = document.styleSheets;
    for (let i = 0, max = ss.length; i < max; i++) {
        if (ss[i].href == path)
            return;
    }
    let link = document.createElement("link");
    link.rel = "stylesheet";
	link.type = "text/css";
    link.href = path;

    document.head.appendChild(link);
}

async function generateModalDialog(rootUrl) {
    await loadCssIfNotAlreadyLoaded(rootUrl + "/plugin/hp-application-automation-tools-plugin/css/modal_dialog.css");

	let browsersModal = document.createElement("div");
	browsersModal.setAttribute("name", "cloudBrowsersModal");
	browsersModal.className = "modal";

	let content = document.createElement("div");
	content.className = "modal-content";

	let hdr = document.createElement("div");
	hdr.className = "modal-header";

	let xDiv = document.createElement("div");
	xDiv.innerHTML = "x";
	xDiv.className = "close";
	xDiv.setAttribute("onclick","hideCloudBrowserModal(this)");

	let title = document.createElement("div");
	title.innerHTML = "Choose a browser";
	title.setAttribute("class","modal-title");

	let body = document.createElement("div");
	body.setAttribute("class","modal-body");

    addEmptySelect(body, "Operating System", "cldBrowserOS");
    addEmptySelect(body, "Browser Type", "cldBrowser");
    addEmptySelect(body, "Browser Version", "cldBrowserVer");
    addEmptySelect(body, "Location", "cldBrowserRegion");

	let sDiv = document.createElement('div');
	sDiv.innerHTML = "SAVE";
	sDiv.setAttribute("class","save-text");
	sDiv.setAttribute("onclick",'onSaveCloudBrowser(this)');

	let footer = document.createElement("div");
	footer.setAttribute("class","modal-footer");

	footer.appendChild(sDiv);
	hdr.appendChild(xDiv);
	hdr.appendChild(title);
	content.appendChild(hdr);
	content.appendChild(body);
	content.appendChild(footer);
	browsersModal.appendChild(content);

	return browsersModal;
};

function addEmptySelect(parent, label, name) {
    let div = document.createElement("div");
    div.setAttribute("class", "modal-body-item");
    div.innerHTML = `<label><span>${label}</span><select name="${name}"></select></label>`;
	parent.appendChild(div);
}