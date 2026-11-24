# Autonomous Tester default configuration

## Editing the configuration

Edit the adjacent `default-configuration.json` on the Jenkins controller:

```text
<JENKINS_HOME>\aute-config\<configurationId>\<spaceId>\
```

AuTe jobs for this connection and shared space use this configuration. Each build
copies and archives a snapshot; edits affect only later snapshots. A build fails
when the file is not a valid JSON object.

The top-level blocks are `BROWSER`, `AGENT`, `LLM_MODELS`, and `ENVIRONMENT`, each a
JSON object. Only `LLM_MODELS` is required; the build fails before any run when it is
missing or not a JSON object. Use valid JSON: double-quoted names and strings,
unquoted numbers, booleans, and `null`, with no comments or trailing commas.

Values below are examples, not defaults. Check your mi-agent version for required
fields, supported values, timeout units, and how omitted values, `null`, and empty
lists behave.

**Sensitive data:** this JSON is archived with each build. Restrict access to the
configuration file and build artifacts.

## BROWSER

Controls the browser used for execution.

| Setting                     | JSON type        | Purpose                                                                         |
| --------------------------- | ---------------- | ------------------------------------------------------------------------------- |
| `BROWSER_NAME`            | string           | Browser to use.                                                                 |
| `BROWSER_LOCALE`          | string           | Browser locale, for example `en-US`.                                          |
| `BROWSER_CHANNEL`         | string or null   | Browser distribution/channel.                                                   |
| `BROWSER_EXECUTABLE_PATH` | string or null   | Executable on the agent; escape backslashes, e.g. `C:\\Browsers\\chrome.exe`. |
| `ALLOWED_DOMAINS`         | array of strings | Allowed domains; verify format and empty-list behavior.                         |
| `PROXY_SERVER`            | string or null   | Browser proxy server.                                                           |

```json
{
  "BROWSER": {
    "BROWSER_NAME": "chrome",
    "BROWSER_LOCALE": "en-US",
    "BROWSER_CHANNEL": null,
    "BROWSER_EXECUTABLE_PATH": null,
    "ALLOWED_DOMAINS": [],
    "PROXY_SERVER": null
  }
}
```

## AGENT

Sets execution limits as integers. Check runner-supported ranges and timeout units.

| Setting                     | JSON type | Purpose                  |
| --------------------------- | --------- | ------------------------ |
| `MAX_FAILURES`            | integer   | Failure limit.           |
| `MAX_NUMBER_OF_STEPS`     | integer   | Maximum execution steps. |
| `BROWSER_USE_RUN_TIMEOUT` | integer   | Browser-use run timeout. |

```json
{
  "AGENT": {
    "MAX_FAILURES": 3,
    "MAX_NUMBER_OF_STEPS": 100,
    "BROWSER_USE_RUN_TIMEOUT": 100000
  }
}
```

## LLM_MODELS

Sets execution and analysis models. Choose model names, thinking levels, and
temperatures supported by your model provider.

| Block               | Setting                         | JSON type | Purpose                                 |
| ------------------- | ------------------------------- | --------- | --------------------------------------- |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_MODEL`          | string    | Model used for execution.               |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_THINKING_LEVEL` | string    | Execution model's reasoning level.      |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_TEMPERATURE`    | number    | Execution model's sampling temperature. |
| `ANALYSIS_MODEL`  | `LLM_ANALYZER_MODEL`          | string    | Model used for analysis.                |
| `ANALYSIS_MODEL`  | `LLM_ANALYZER_THINKING_LEVEL` | string    | Analysis model's reasoning level.       |
| `ANALYSIS_MODEL`  | `LLM_ANALYZER_TEMPERATURE`    | number    | Analysis model's sampling temperature.  |

```json
{
  "LLM_MODELS": {
    "EXECUTION_MODEL": {
      "LLM_EXECUTOR_MODEL": "gemini-3.8-flash",
      "LLM_EXECUTOR_THINKING_LEVEL": "medium",
      "LLM_EXECUTOR_TEMPERATURE": 0.2
    },
    "ANALYSIS_MODEL": {
      "LLM_ANALYZER_MODEL": "gemini-3.8-flash",
      "LLM_ANALYZER_THINKING_LEVEL": "high",
      "LLM_ANALYZER_TEMPERATURE": 0.2
    }
  }
}
```

## ENVIRONMENT

Controls execution features. This block is optional; when it is missing, Jenkins
creates it for each run. You can add other environment settings supported by your
mi-agent version.

Jenkins sets these values for every run and overwrites anything set here:

| Value                                       | Set to                                                         |
| ------------------------------------------- | -------------------------------------------------------------- |
| `ENVIRONMENT.OUTPUT_DIR`                  | The run's result folder in the build workspace.                |
| `ENVIRONMENT.EXECUTION_RECORDING_ENABLED` | `false` (execution recording is not supported from Jenkins). |

```json
{
  "ENVIRONMENT": {}
}
```

## Complete example

Copy and adapt this configuration:

```json
{
  "BROWSER": {
    "BROWSER_NAME": "chrome",
    "BROWSER_LOCALE": "en-US",
    "BROWSER_CHANNEL": null,
    "BROWSER_EXECUTABLE_PATH": null,
    "ALLOWED_DOMAINS": [],
    "PROXY_SERVER": null
  },
  "AGENT": {
    "MAX_FAILURES": 3,
    "MAX_NUMBER_OF_STEPS": 100,
    "BROWSER_USE_RUN_TIMEOUT": 100000
  },
  "LLM_MODELS": {
    "EXECUTION_MODEL": {
      "LLM_EXECUTOR_MODEL": "gemini-3.8-flash",
      "LLM_EXECUTOR_THINKING_LEVEL": "medium",
      "LLM_EXECUTOR_TEMPERATURE": 0.2
    },
    "ANALYSIS_MODEL": {
      "LLM_ANALYZER_MODEL": "gemini-3.8-flash",
      "LLM_ANALYZER_THINKING_LEVEL": "high",
      "LLM_ANALYZER_TEMPERATURE": 0.2
    }
  },
  "ENVIRONMENT": {}
}
```

## File preservation and guide updates

Existing files are never overwritten. Missing files are added during AuTe job
creation or a build's configuration snapshot: a missing `default-configuration.json`
is recreated as an empty template with the four blocks, and a missing guide is
restored from the bundled version. To refresh the guide after an upgrade, delete
only `default-configuration-example.md`.
