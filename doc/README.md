# Micro Focus Application Automation Tools


This  [Micro Focus Application Automation Tools plugin](https://plugins.jenkins.io/hp-application-automation-tools-plugin) integrates Micro Focus products with Jenkins. Using the plugin, you can create and use virtual services, run LoadRunner Enterprise or LoadRunner Professional tests, run UFT functional tests, run tests in your lab, and run tests on mobile devices. This plugin also lets you upload test results to ALM. In addition, ALM Octane users can track and trigger Jenkins pipelines from within the user interface.

### Jenkins versions

Beginning version 5.5, this plugin only supports the latest five LTS versions of Jenkins (minimum version is 2.190.1). This is a result of the Jenkins policy to no longer support older update centers. Jenkins now requires you to have Java 8 installed on the machine. To recover any information lost during an upgrade relating to package names, backup the Jenkins folder and then run the [Rebranding script](https://wiki.jenkins.io/display/JENKINS/Micro+Focus+Application+Automation+Tools#MicroFocusApplicationAutomationTools-UserGuide).

### Submit issues and feedback through JIRA


This plugin is open source. If you identify any issues or want to submit enhancement requests, please use [JIRA](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20hp-application-automation-tools-plugin). The component for this plugin is the *hp-application-automation-tools-plugin* component. Your feedback and involvement will allow us to stabilize and enhance the capabilities of the plugin. The latest, early access, beta versions of this plugin are available [here](https://mvnrepository.com/artifact/org.jenkins-ci.plugins/hp-application-automation-tools-plugin).

A list of known bugs is available [here](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20issuetype%20%3D%20Bug%20AND%20status%20not%20in%20(Resolved%2C%20Closed)%20AND%20component%20%3D%20hp-application-automation-tools-plugin%20ORDER%20BY%20votes%20DESC%2C%20watchers%20DESC).

## Table of Contents

[New Features and Enhancements](#new-features-and-enhancements)

[Changelog](#changelog)

[Supported Integrations](#supported-integrations)

[Prerequisites](#prerequisites)

[User Guide](#user-guide)

- [Rebranding Script](#rebranding-script)
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



### Content Security Policy

Starting with version 1.641 (or 1.625.3), Jenkins introduced the  **Content-Security-Policy**  header. This causes some of the integration links, such as links to reports, to become inoperable. For details, see [Configuring Content Security Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy) and [Jenkins Security Advisory ](https://jenkins.io/security/advisory/2015-12-09/). For suggested workarounds until the issue is resolved, see [Content Security Policy Header](#content-security-policy-header).

## New Features and Enhancements in version 6.5

Version 6.5 introduced the following enhancement(s):

**General**

- “github-branch-source” dependency was removed.
- Certified for Jenkins version 2.263.1.

**ALM Octane**

- New option to cache the job list for pipeline creation in ALM Octane
- Caching of Octane pipeline roots, to avoid sending non-relevant events/tasks/logs to ALM Octane
- Added ability to execute UFT tests with parameters from ALM Octane
- Bug fix: UFT test descriptions are properly formatted in ALM Octane

For information about enhancements introduced in previous versions, see [What's new in earlier versions](WhatsNewEarlier.md). 

## Changelog

The [Changelog ](https://wiki.jenkins.io/display/JENKINS/Changelog)page lists the bug fix changes in the versions of the plugin.



- [Version 5.6.2](https://wiki.jenkins.io/display/JENKINS/Changelog#Version5.6.2(Dec19,2018))

- [Version 5.6.1](https://wiki.jenkins.io/display/JENKINS/Changelog#Version5.6.1(Dec18,2018))

- [Version 5.5.2](https://wiki.jenkins.io/display/JENKINS/Changelog#Version5.5.2Beta(Oct2,2018))

- [Version 5.5.1](https://wiki.jenkins.io/display/JENKINS/Changelog#Version5.5.1(Sept12,2018))



## Supported Integrations



This plugin supports the following Micro Focus product versions:

| Micro Focus tool                                    | Supported versions                                           | Find more information...                                     |
| :-------------------------------------------------- | :----------------------------------------------------------- | ------------------------------------------------------------ |
| ALM (Application Lifecycle Management)              | 12.xx, 15.x                                                  | [ALM integration page](ALM_Integration.md)                  |
| ALM Lab Management                                  | 12.xx, 15.x                                                  | [ALM integration page](ALM_Integration.md#running-server-side-tests-using-alm-lab-management) |
| ALM Octane                                          | 12.53.20 and higher (12.55.4 or later required for direct UFT One integration and for LoadRunner Enterprise integration using pipelines) | [ALM Octane Help](https://admhelp.microfocus.com/octane/en/latest/Online/Content/AdminGuide/jenkins-integration.htm) |
| LoadRunner                                          | 12.xx, 2020                                                  | [LoadRunner Professional integration page](LR_Integration.md)             |
| UFT Mobile (formerly Mobile Center)                 | 2.0 and higher                                               | [UFT Mobile integration page](UFT_Mobile_Integration.md) |
| LoadRunner Enterprise (formerly Performance Center) | 12.xx (12.53 or higher required for trend reports), 2020     | [LoadRunner Enterprise Help Center](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Continuous-Integration-Jenkins.htm) |
| Service Virtualization                              | 3.80 and higher                                              | [Service Virtualization Help Center](https://admhelp.microfocus.com/sv/en/latest/Help/Content/UG/c_continuous_integration.htm) |
| UFT One (formerly UFT)                              | 12.xx and 14.03 and higher                                   | [UFT One Help Center](https://admhelp.microfocus.com/uft/en/latest/UFT_Help/Content/User_Guide/Jenkins.htm) |
| UFT Developer (formerly LeanFT)                     | 14.03 and higher                                             | [UFT Developer Help Center](https://admhelp.microfocus.com/leanft/en/latest/HelpCenter/Content/HowTo/CI_Tools.htm)<br />Blog: [Integrating LeanFT with Jenkins  in just a few simple steps](https://community.softwaregrp.com/t5/Quality-and-Testing-Blog/Integrating-LeanFT-with-Jenkins-in-just-a-few-simple-steps/ba-p/247321#.WpzwYOhubcte%20steps) |

## Prerequisites

1. Install one of the five latest LTS versions of Jenkins, [(Click here for a list.](https://jenkins.io/changelog-stable/))

2. **Java version 8 or higher.**  To verify your Java version, go to [http://www.java.com/en/download/installed.jsp](http://www.java.com/en/download/installed.jsp).

3. Install the plugin:
   
   a. Download the .hpi file for the Jenkins plugin from the [plugin page](https://updates.jenkins-ci.org/download/plugins/hp-application-automation-tools-plugin/ ). 
   
   b. Navigate to the **Manage Jenkins > Manage Plugins > Advanced** tab. 
   
   c. In the **Upload Plugin** section, upload the .hpi file.
4. **For working with ALM and Quality Center,** make sure the ALM/Quality Center client  is installed on the machine that will run the tests. To check if the client installed, follow the instructions on:   http://<your\_server>:8080/qcbin/TDConnectivity\_index.html

5. **For running UFT tests from ALM**  install the ALMClient in common registration mode by accessing the following link from an Internet Explorer browser on the UFT machine: http://<your\_server>:8080/qcbin/start\_a.jsp?Common=true

6. **For running file system scenarios with LoadRunner or UFT**  set up a node in Jenkins. For details, see [Create an execution node](#create-an-execution-node).

7. **For building and tracking pipelines on ALM Octane:**

   *  JQuery Plugin 1.7.2-1 or later (Required to enable the integration)
   *  A plugin that enables publishing test results. For example, JUnit Plugin 1.10 or later, NUnit plugin, and so on (Required to enable ALM Octane to collect your automated test results).

8. **For ALM Octane integration with UFT**, make sure you have version 2.4.4 or higher of the Git plugin.

# User Guide

You can run client side-or server-side (Default or Functional) test sets and build verification suites from Application Lifecycle Management (ALM) or functional tests from the file system. You can create and configure ALM Octane pipelines from the ALM Octane user interface, or on Jenkins.

## Rebranding script

Due to rebranding in this version of the plugin to the &quot;Micro Focus&quot; name, certain job history and configurations from previous jobs may be not be recognized by Jenkins. Before you use this version of the plugin, back up all projects and jobs, and all job configurations related to the plugin that are included in the General Jenkins configuration. Run the following job to convert your earlier configurations:

- Create a new  **Free-Style**  project.

- Click  **Add build step**  and select  **Fix old Micro Focus Jenkins builds**.

- Run the job.

- Restart the server after the build status is successful. You can restart the server by entering the following URL:  **Jenkins\_URL/safeRestart**



## Create an execution node



1. Creating an execution node is only required when running scenarios from LoadRunner or UFT, that are stored on the file system. You only need to set an execution node if the Jenkins master machine is  **not**  the executing machine. If you are running LoadRunner or UFT One on the master machine, you do not need to set and select a node.

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

To set up a pipeline test job for your Micro Focus testing tool:

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
         c. Fill in the fields as required. Fields marked in red are mandatory. **Note:**   For fields that take multiple values, such as in the **Tests** field, separate multiple entries with a line break.
         d. If relevant, select one of the [Report archive modes](#report-archive-modes).
         e. Click  **Generate Pipeline Script**. Copy the code to the clipboard.

6. Return to the Project Configuration page, and paste the generated Groovy script into the  **Script**  field in the Pipeline section.

7. Repeat the above steps to add other commands to your script.

8. Save the script and run or schedule the job as you would with any standard Jenkins job.

9. After the test run, click the  **Console**  link on the dashboard to see a link to your results. Copy the link to your browser (Internet Explorer required to view ALM test sets in ALM).



### Supported Pipeline job types

The available Pipeline job types are: loadRunnerTest, uftScenarioLoad, runFromAlmBuilder, sseBuild, sseBuildAndPublish, pcBuild, svChangeModeStep, svDeployStep, svExportStep, and svUndeployStep.

| Product                | Pipeline step name | Description                                                  |
| ---------------------- | ------------------ | ------------------------------------------------------------ |
| LoadRunner             | loadRunnerTest     | Run LoadRunner performance tests from a file system scenario file |
| UFT One                | uftScenarioLoad    | Run a UFT scenario from file system scenario                 |
| ALM                    | runFromAlmBuilder  | Execute functional tests from ALM                            |
| ALM Lab Management     | sseBuild           | Execute tests using ALM Lab Management                       |
| ALM Lab Management     | sseBuildAndPublish | Execute tests using ALM Lab Management and publish test results |
| LoadRunner Enterprise  | pcBuild            | Execute tests using LoadRunner Enterprise                    |
| Service Virtualization | svChangeModeStep   | Change the mode of a Virtual Service                         |
| Service Virtualization | svDeployStep       | Deploy a Virtual Service                                     |
| Service Virtualization | svExportStep       | Export a Virtual Service                                     |
| Service Virtualization | svUndeployStep     | Undeploy a Virtual Service                                   |

Pipeline jobs are not supported for UFT Mobile (Mobile Center) uploads, ALM test uploader, and ALM AUT job types.



### Report archive modes

The available archive modes are:

- Archive test report for failed tests

- Always archive test reports

- Always archive and publish test reports (LR only)

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

Starting with version 1.641 (or 1.625.3), Jenkins introduced the  **Content-Security-Policy**  header. This prevents some of the links that appear in the integration to become inoperable. For example, the links to the LoadRunner Performance and UFT HTML reports will not work.

For workarounds to enable viewing UFT HTML Reports, see the [UFT Help Center](https://admhelp.microfocus.com/uft/en/latest/UFT_Help/Content/User_Guide/Jenkins.htm#mt-item-5).



### Integration Issues

- If your job includes UFT, QTP, or Service Test tests running on a remote ALM/QC machine (run mode = Run Remotely), you should manually stop the test execution.

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

