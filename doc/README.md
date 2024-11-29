# OpenText Application Automation Tools


This [OpenText Application Automation Tools plugin](https://plugins.jenkins.io/hp-application-automation-tools-plugin) integrates OpenText products with Jenkins. Using the plugin, you can create and use virtual services, run LoadRunner Enterprise or LoadRunner Professional tests, run UFT functional tests, run tests in your lab, and run tests on mobile devices. This plugin also lets you upload test results to ALM. In addition, ALM Octane users can track and trigger Jenkins pipelines from within the user interface.

### Jenkins versions

This plugin requires one of the five latest [LTS versions](https://jenkins.io/changelog-stable/) of Jenkins. This is a result of the Jenkins policy to no longer support older update centers. Jenkins now requires you to have Java 8 installed on the machine.

### Submit issues and feedback through JIRA


This plugin is open source. If you identify any issues or want to submit enhancement requests, please use [JIRA](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20hp-application-automation-tools-plugin). The component for this plugin is the *hp-application-automation-tools-plugin* component. Your feedback and involvement will allow us to stabilize and enhance the capabilities of the plugin. The latest, early access, beta versions of this plugin are available [here](https://mvnrepository.com/artifact/org.jenkins-ci.plugins/hp-application-automation-tools-plugin).

A list of known bugs is available [here](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20issuetype%20%3D%20Bug%20AND%20status%20not%20in%20(Resolved%2C%20Closed)%20AND%20component%20%3D%20hp-application-automation-tools-plugin%20ORDER%20BY%20votes%20DESC%2C%20watchers%20DESC).

## Table of Contents

[New Features and Enhancements](#new-features-and-enhancements)

[Release notes](#release-notes)

[Supported Integrations](#supported-integrations)

[Prerequisites](#prerequisites)

[User Guide](#user-guide)

- [Create an execution node](#create-an-execution-node)
- [Connect an execution node to the Jenkins server](#connect-an-execution-node-to-the-jenkins-server)
- [Support for pipelines](#support-for-pipelines)

[Enable non-English languages](#enable-non-english-languages)

[Configuration for Java Web Start clients](#configuration-for-java-web-start-clients)

[Tips and Troubleshooting](#tips-and-troubleshooting)

- [Best Practice-Use Slave Machine](#best-practice-use-slave-machine)
- [Content Security Policy Header](#content-security-policy-header)

- [Integration Issues](#integration-issues)

- [Workarounds for viewing LoadRunner Performance reports](#workarounds-for-viewing-loadrunner-performance-reports)

[Limitations](#limitations)

### Content Security Policy

Starting with version 1.641 (or 1.625.3), Jenkins introduced the  **Content-Security-Policy**  header. This causes some of the integration links, such as links to reports, to become inoperable. For details, see [Configuring Content Security Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy) and [Jenkins Security Advisory ](https://jenkins.io/security/advisory/2015-12-09/). For suggested workarounds until the issue is resolved, see [Content Security Policy Header](#content-security-policy-header).

## New Features and Enhancements

Version CE 24.4 introduced the following enhancements and fixes:

**ALM Octane**
- Support SCM Data injection from GitLab
- Bug fixes

**OpenText Enterprise Performance(LoadRunner Enterprise)**
- LoadRunner Enterprise has been rebranded to OpenText Enterprise Performance

For information about enhancements introduced in previous versions, see [What's new in earlier versions](https://github.com/jenkinsci/hpe-application-automation-tools-plugin/blob/latest/doc/WhatsNewEarlier.md). 


## Supported Integrations


This plugin supports the following OpenText product versions:

| OpenText tool                               | Supported versions                                           | Find more information...                                     |
|:--------------------------------------------| :----------------------------------------------------------- | ------------------------------------------------------------ |
| ALM (Application Lifecycle Management)      | 12.xx, 15.x, 16.x, 17.x, 24.x                                                  | [ALM Integration  page](https://admhelp.microfocus.com/alm/en/latest/online_help/Content/jenkins-integration.htm)                  |
| ALM Lab Management                          | 12.xx, 15.x, 16.x, 17.x, 24.x                                                | [ALM Integration page](https://admhelp.microfocus.com/alm/en/latest/online_help/Content/jenkins-integration.htm#mt-item-3) |
| ALM Octane                                  | 12.53.20 and higher (12.55.4 or later required for direct UFT One integration and for LoadRunner Enterprise integration using pipelines) | [ALM Octane Help Center](https://admhelp.microfocus.com/octane/en/latest/Online/Content/AdminGuide/jenkins-integration.htm) |
| OpenText Enterprise Performance Engineering | 12.xx (12.53 or higher required for trend reports), 2020 and higher     | [LoadRunner Enterprise Help Center](https://admhelp.microfocus.com/lre/en/latest/online_help/Content/PC/Continuous-Integration-Jenkins.htm) |
| LoadRunner Professional                     | 12.xx, 2020 and higher                                       | [LoadRunner Professional Integration page](https://admhelp.microfocus.com/lr/en/latest/help/WebHelp/Content/Controller/c_jenkins.htm)             |
| Model-based Testing                         | 16.0.300 and higher  | [Model-Based Testing Help Center](https://admhelp.microfocus.com/mbt) |
| Service Virtualization                      | 3.80 and higher                                              | [Service Virtualization Help Center](https://admhelp.microfocus.com/sv/en/latest/Help/Content/UG/c_continuous_integration.htm) |
| UFT Developer                               | 14.03 and higher                                             | [UFT Developer Help Center](https://admhelp.microfocus.com/uftdev/en/latest/HelpCenter/Content/HowTo/CI_Tools.htm)<br />Blog: [Integrate LeanFT with Jenkins in just a few simple steps](https://community.microfocus.com/adtd/b/sws-alm/posts/integrating-leanft-with-jenkins-in-just-a-few-simple-steps) |
| Digital Lab (UFT Mobile)                    | 2.0 and higher                                               | [Digital Lab Integration page](https://admhelp.microfocus.com/digitallab/en/latest/Content/CI_jenkins.htm) |
| UFT One                                     | 12.xx and 14.03 and higher                                   | [UFT One Help Center](https://admhelp.microfocus.com/uft/en/latest/UFT_Help/Content/User_Guide/Jenkins.htm) |
| ValueEdge                                   |                               | [ValueEdge Help Center](https://admhelp.microfocus.com/valuedge) |

## Prerequisites

1. Install one of the five latest LTS versions of Jenkins. For details, see the [LTS Changelog](https://jenkins.io/changelog-stable/). **Note:** The plugin may not operate properly with earlier versions of Jenkins.

2. **Java version 8 or higher.**  To verify your Java version, go to [http://www.java.com/en/download/installed.jsp](http://www.java.com/en/download/installed.jsp).

3. Install the plugin:
   
   a. Download the .hpi file for the Jenkins plugin from the [plugin page]( https://updates.jenkins-ci.org/download/plugins/hp-application-automation-tools-plugin/ ). 
   
   b. Navigate to the **Manage Jenkins > Manage Plugins > Advanced** tab. 
   
   c. In the **Upload Plugin** section, upload the .hpi file.
4. **For working with ALM and Quality Center,** make sure the ALM/Quality Center client  is installed on the machine that will run the tests. To check if the client installed, follow the instructions on:   http://<your\_server>:8080/qcbin/TDConnectivity\_index.html

5. **For running UFT One tests from ALM**  install the ALMClient in common registration mode by accessing the following link from an Internet Explorer browser on the UFT One  machine: http://<your\_server>:8080/qcbin/start\_a.jsp?Common=true

6. **For running file system scenarios with LoadRunner Professional or UFT One**  set up a node in Jenkins. For details, see [Create an execution node](#create-an-execution-node).

7. **For building and tracking pipelines on ALM Octane:**

   *  JQuery Plugin 1.7.2-1 or later (Required to enable the integration)
   *  A plugin that enables publishing test results. For example, JUnit Plugin 1.10 or later, NUnit plugin, and so on (Required to enable ALM Octane/ValuEdge  to collect your automated test results).

8. **For ALM Octane/ValueEdge integration with UFT One**, make sure you have version 2.4.4 or higher of the Git plugin.

# User Guide

You can run client side-or server-side (Default or Functional) test sets and build verification suites from Application Lifecycle Management (ALM) or functional tests from the file system. You can create and configure ALM Octane pipelines from the ALM Octane user interface, or on Jenkins.


## Create an execution node

1. Creating an execution node is only required when running scenarios from LoadRunner Professional or UFT One, that are stored on the file system. You only need to set an execution node if the Jenkins master machine is  **not**  the executing machine. If you are running LoadRunner Professional or UFT One on the master machine, you do not need to set and select a node.

2. Go to the Jenkins Server home page.

3. Click the  **Manage Jenkins**  link in the left pane.

4. In the Manage Jenkins Page click  **Manage Nodes**.

5. Click  **New Node**.

6. Enter a  **Node name** , select  **Permanent Agent** , and then click  **OK**.

7. In the next screen, enter information in the required fields. Enter the full path of the  **Remote Root directory** , to which the tests results will be saved.

8. Provide one or more labels for the node. Separate the values with a space. Through these labels, you will be able to identify the nodes used in the job.

9. In the  **Usage**  area, select  **Only build jobs with label expressions matching this node**.

10. Click  **Save**.

11. Connect the execution node to the Jenkins server as described below.



## Connect an execution node to the Jenkins server

To connect and execution node to the Jenkins machine:

1. On the computer that you defined as an execution node, open a browser and go to the Jenkins Server home page.

2. Open  **Manage Jenkins**  **>**  **Manage Nodes**.

3. If there is a warning mark adjacent to the node you want to connect, click the node&#39;s link. You are prompted to connect the agent to Jenkins.

4. Click  **Launch**  to download the agent and then run the downloaded agent. Alternatively, use one of the other methods suggested on the screen.



## Support for Pipelines

### Generate pipeline code

To set up a pipeline test job for your OpenText testing tool:

  1. From Jenkins Dashboard. click  **New Item**  or select an existing one.

  2. On the page that opens, enter an item name (for a new job), click  **Build a Pipeline project** , and click  **OK**.

  3. In the Project Configuration page, scroll down to the **Pipeline** section.

  4. Enter the  **stage**  and  **node**  arguments into the  **Script**  area. For example,

     ```
     stage('RunUFTTestFromFS'){ // The stage name
       node('Test'){// The name of the node in which to run the test.
     ```

  5. Prepare the code for your testing tool:

         a. Click the **Pipeline Syntax** link.
         b. In the Snippet Generator drop down, select the desired step, for example,  **uftScenarioLoad: Run UFT scenario**.
         c. Fill in the fields as required. Fields marked in red are mandatory. **Note:**  For fields that take multiple values, such as in the **Tests** field, separate multiple entries with a line break.
         d. If relevant, select one of the [Report archive modes](#report-archive-modes).
         e. Click  **Generate Pipeline Script**. Copy the code to the clipboard.

6. Return to the Project Configuration page, and paste the generated Groovy script into the  **Script**  field in the Pipeline section.

7. Repeat the above steps to add other commands to your script.

8. Save the script and run or schedule the job as you would with any standard Jenkins job.

9. After the test run, click the **Console** link on the dashboard to see a link to your results. Copy the link to your browser (Internet Explorer required to view ALM test sets in ALM).



### Supported Pipeline job types

The available Pipeline job types are: loadRunnerTest, uftScenarioLoad, runFromFSBuilder, runFromAlmBuilder, sseBuild, sseBuildAndPublish, pcBuild, svChangeModeStep, svDeployStep, svExportStep, svUndeployStep, and publishMFTestResults

| Product                | Pipeline step name           | Description                                                             |
| ---------------------- | ---------------------------- |-------------------------------------------------------------------------|
| LoadRunner Professional| loadRunnerTest               | Run LoadRunner Professional tests from a file system scenario file.     |
| UFT One                | uftScenarioLoad              | Run a UFT scenario. **Deprecated**, but backwards compatibility exists. |
| UFT One                | runFromFSBuilder             | Execute UFT One Tests from the file system.                             |
| ALM                    | runFromAlmBuilder            | Execute functional tests from ALM.                                      |
| ALM Lab Management     | sseBuild                     | Execute tests using ALM Lab Management.                                 |
| ALM Lab Management     | sseBuildAndPublish           | Execute tests using ALM Lab Management and publish test results.        |
| OpenText Enterprise Performance EngineeringOpenText Enterprise Performance Engineering.        |
| Service Virtualization | svChangeModeStep             | Change the mode of a virtual service.                                   |
| Service Virtualization | svDeployStep                 | Deploy a virtual service.                                               |
| Service Virtualization | svExportStep                 | Export a virtual service.                                               |
| Service Virtualization | svUndeployStep               | Undeploy a virtual service.                                             |
| UFT One, ALM, ALM LM   | publishMicroFocusTestResults | Publish Test Results for FS, ALM and ALM Lab Management executions.     |                                   |

Pipeline jobs are not supported for Digital Lab (formerly UFT Mobile) uploads, ALM test uploader, and ALM AUT job types.



### Report archive modes

The available archive modes are:

- Archive test report for failed tests

- Always archive test reports

- Always archive and publish test reports (LRP only)

- Do not archive test reports



## Enable Non-English Languages

In order to allow the add-in to support non-English languages, make the following changes to your Jenkins Master and nodes configuration:

### Master configuration

1. Open the  **Jenkins.xml**  file in the Jenkins installation folder, for example: c:\Jenkins\Jenkins.xml.

2. Go to the  \<service>  -> \<arguments> section and add the  **-Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8**  flags after the  **-jar**  flag.

### Slave configuration

1. Open the  **jenkins-slave.xml**  file on the Jenkins slave (node) machine in the folder that you designated.

2. Go to the \<service>  -> \<arguments> section and add the  **-Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8**  flags after the  **-jar**  flag.

For example:

```
<service>
<id>jenkinsslave-c__jkns</id>
<name>jenkinsslave-c__jkns</name>
<description>This service runs a slave for Jenkins continuous integration system.</description>
<!--
This example assumes that you have java in your PATH.
To run Jenkins with a specific version of Java, specify a full path to the  desired java.exe.
-->
<executable>C:\Program Files (x86)\Java\jre1.8.0_91\bin\java.exe</executable>
<arguments>-Xrs -jar "%BASE%\slave.jar" -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -jnlpUrl http://xxx.xxx.xxx.xxx:8080/jenkins/computer/VM112233/slave-agent.jnlp -secret xyzabc</arguments>
<!--
The *interactive" flag causes the empty black Java window to be displayed.
<interactive />
-->
<logmode>rotate</logmode>
<onfailure action="restart" />
</service>
```
## Configuration for Java Web Start clients

1. On the Jenkins master machine, go to  **Manage Jenkins --> Manage Nodes**. Click the relevant node or slave. and select  **Slave --> Configure --> Advanced**.
2. Add the following to the JVM options text box:   **&quot;-Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8&quot;**.
3. Restart your Jenkins master, node, and slave.
4. Verify that your changes took effect:
     a. For the Master: Go to  **Manage Jenkins --> System information --> &quot;file.encoding&quot;, &quot;sun.jnu.encoding&quot;**. The value should be &quot;UTF-8&quot;.
     b. For the node/slave: On the master machine, go to  **Manage Jenkins --> Manage Nodes**.
     c. Click on the relevant node or slave and select  **System information --> &quot;file.encoding&quot;, &quot;sun.jnu.encoding&quot;**. The value should be &quot;UTF-8&quot;.

**Workaround:**  If you find that the slave machine still does not support localized encoding, launch the slave from the command line as shown in the image below:

For example:

![Jenkins_slave_command_line](Jenkins_slave_setup.png)

## Tips and Troubleshooting

### Best Practice-Use Slave Machine

For best performance, it is recommended to install the testing tools and ALM/QC as nodes on slave machines, and not directly on the Jenkins server.  For instructions on configuring nodes, see the [Step by step guide to set up master and agent machines in Windows]( https://wiki.jenkins.io/display/JENKINS/Step+by+step+guide+to+set+up+master+and+agent+machines+on+Windows ).

On the Jenkins slave machine, make sure the the Jenkins Slave service is not logged in with the  **Local System account**. To function properly, it should log in with an account the has administrator privileges.

![Jenkins_slave_account](Jenkins_slave_account.png)



### Content Security Policy Header

Starting with version 1.641 (or 1.625.3), Jenkins introduced the  **Content-Security-Policy**  header. This prevents some of the links that appear in the integration to become inoperable. For example, the links to the LoadRunner Performance and UFT One HTML reports will not work.

For workarounds to enable viewing UFT One HTML Reports, see the [UFT One Help Center](https://admhelp.microfocus.com/uft/en/latest/UFT_Help/Content/User_Guide/Jenkins.htm#mt-item-5).



### Integration Issues

- If your job includes UFT One, QTP, or Service Test tests running on a remote ALM/QC machine (run mode = Run Remotely), you should manually stop the test execution.

- When UFT is installed on the slave machine (node), the LoadRunner test job will fail in the  **Analyze Result**  stage.  **Workaround:**  Add the path of the LoadRunner  **bin**  folder ( **%LR\_PATH%/bin** ) to the  **PATH**  environment variable.



### Workarounds for viewing LoadRunner Performance reports

Follow these workarounds to enable the viewing LoadRunner Performance reports:

- View the reports locally on the slave machine, in your Jenkins results folder under the relevant build ID, or on the master machine in the Jenkins builds folder. Go to the  **%jenkins% \jobs\\<job_name>\builds\<job_count>\PerformanceReport**  folder. For example, **C:\Program Files (x86)\Jenkins\jobs\Plug\_Debug\builds\5\PerformanceReport\index.html**.

- Click  **Manage Jenkins > Script Console** , and run one of the following scripts from the console:

  - Type the following line and press  **Run**.
     `System.setProperty("hudson.model.DirectoryBrowserSupport.CSP","")` 

  - Alternatively, type the following line and press  **Run**.
     `System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "sandbox; default-src 'none'; img-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; child-src 'self';")`  

  - Another option that is more secure, but it disables the left pane menu and embedded Javascript:
     `System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "sandbox; default-src 'none'; img-src 'self'; style-src 'self'; script-src 'self'; child-src 'self';")` 

## Limitations

### Test Parameters syntax

The following limitations apply to the inline parameter specification feature of the **Test Sets** input field:

   - The double quote and space character sequence (` "`) is used to separate the path and parameter list specification.  As a result, if you specify the test set path with double quotes, the line won't parse correctly. **Tip:** never specify paths with double quotes when using inline parameters. **Note:** If the path contains whitespaces, the path will be parsed correctly without the quotes.
   - The parameter list's delimiter is a comma. Therefore, neither the path, nor the parameter names or values should contain commas. **Tip:** if you have a complex parameter value (e.g. `"param1"="value1,value2"`), specify separate parameters for this within the respective UFT One test.
   - The parameter-value key-value pair is separated by a colon. Therefore, neither the parameter name, nor the value should contain a colon. **Note:** Currently, due to this limitation, **links** cannot be used in the inline parameter definitions.

**Note**: If any of the above limitations appears within your inline parameter definition, the line will not be parsed correctly, resulting in unexpected behavior in the job execution.
