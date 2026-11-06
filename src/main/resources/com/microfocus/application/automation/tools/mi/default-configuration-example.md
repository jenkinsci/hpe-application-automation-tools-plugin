# Autonomous Tester default configuration

## Editing the configuration

Edit the adjacent `default-configuration.json` on the Jenkins controller:

```text
<JENKINS_HOME>\aute-config\<configurationId>\<spaceId>\
```

AuTe jobs for this connection and shared space use this configuration. Each build
copies and archives a snapshot; edits affect only later snapshots.

Keep all five top-level blocks as JSON objects: `BROWSER`, `AGENT`, `LLM_MODELS`,
`APLS_LICENSE`, and `ENVIRONMENT`. Use valid JSON: double-quoted names and strings,
unquoted numbers, booleans, and `null`, with no comments or trailing commas.

Values below are examples, not defaults. Check your mi-agent version for required
fields, supported values, timeout units, and how omitted values, `null`, and empty
lists behave.

**Sensitive data:** passwords and tokens in this archived JSON are not protected
as Jenkins Credentials. Restrict file and artifact access. Configure LLM endpoint
secrets through Jenkins Credentials, not here.

## BROWSER

Controls the browser used for execution.

| Setting | JSON type | Purpose |
| --- | --- | --- |
| `BROWSER_NAME` | string | Browser to use. |
| `BROWSER_LOCALE` | string | Browser locale, for example `en-US`. |
| `BROWSER_CHANNEL` | string or null | Browser distribution/channel. |
| `BROWSER_EXECUTABLE_PATH` | string or null | Executable on the agent; escape backslashes, e.g. `C:\\Browsers\\chrome.exe`. |
| `ALLOWED_DOMAINS` | array of strings | Allowed domains; verify format and empty-list behavior. |
| `PROXY_SERVER` | string or null | Browser proxy server. |

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

| Setting | JSON type | Purpose |
| --- | --- | --- |
| `MAX_FAILURES` | integer | Failure limit. |
| `MAX_NUMBER_OF_STEPS` | integer | Maximum execution steps. |
| `BROWSER_USE_RUN_TIMEOUT` | integer | Browser-use run timeout. |

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
temperatures supported by your LLM endpoint.

| Block | Setting | JSON type | Purpose |
| --- | --- | --- | --- |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_MODEL` | string | Model used for execution. |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_THINKING_LEVEL` | string | Execution model's reasoning level. |
| `EXECUTION_MODEL` | `LLM_EXECUTOR_TEMPERATURE` | number | Execution model's sampling temperature. |
| `ANALYSIS_MODEL` | `LLM_ANALYZER_MODEL` | string | Model used for analysis. |
| `ANALYSIS_MODEL` | `LLM_ANALYZER_THINKING_LEVEL` | string | Analysis model's reasoning level. |
| `ANALYSIS_MODEL` | `LLM_ANALYZER_TEMPERATURE` | number | Analysis model's sampling temperature. |

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

## APLS_LICENSE

Sets AutoPass connection and authentication details when licensing is enabled.
Ask your license administrator which authentication fields your deployment needs.

| Setting | JSON type | Purpose |
| --- | --- | --- |
| `AUTOPASS_HOST` | string or null | AutoPass host reachable from the agent. |
| `AUTOPASS_PORT` | integer | AutoPass port, e.g. `5814`. |
| `AUTOPASS_USERNAME` | string or null | Authentication username, if required. |
| `AUTOPASS_PASSWORD` | string or null | Authentication password (sensitive). |
| `AUTOPASS_SERVER_AUTH_TOKEN` | string or null | Server authentication token (sensitive). |
| `AUTOPASS_TOKEN` | string or null | Licensing token (sensitive). |
| `AUTOPASS_APAUTHTOKEN` | string or null | AP authentication token (sensitive). |

```json
{
  "APLS_LICENSE": {
    "AUTOPASS_HOST": null,
    "AUTOPASS_PORT": 5814,
    "AUTOPASS_USERNAME": null,
    "AUTOPASS_PASSWORD": null,
    "AUTOPASS_SERVER_AUTH_TOKEN": null,
    "AUTOPASS_TOKEN": null,
    "AUTOPASS_APAUTHTOKEN": null
  }
}
```

## ENVIRONMENT

Controls execution features. Use unquoted `true` or `false`.

| Setting | JSON type | Purpose |
| --- | --- | --- |
| `LICENSE_INTEGRATION` | boolean | Enable licensing integration. |
| `EXECUTION_RECORDING_ENABLED` | boolean | Enable execution recording. |

```json
{
  "ENVIRONMENT": {
    "LICENSE_INTEGRATION": false,
    "EXECUTION_RECORDING_ENABLED": false
  }
}
```

Jenkins sets `ENVIRONMENT.OUTPUT_DIR` per run and `LLM_ENDPOINT` from the selected
credential. Do not add them here.

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
  "APLS_LICENSE": {
    "AUTOPASS_HOST": null,
    "AUTOPASS_PORT": 5814,
    "AUTOPASS_USERNAME": null,
    "AUTOPASS_PASSWORD": null,
    "AUTOPASS_SERVER_AUTH_TOKEN": null,
    "AUTOPASS_TOKEN": null,
    "AUTOPASS_APAUTHTOKEN": null
  },
  "ENVIRONMENT": {
    "LICENSE_INTEGRATION": false
  }
}
```

## File preservation and guide updates

Existing files are never overwritten. Missing guides are added during AuTe job
creation or a build's configuration snapshot. To refresh the guide after an
upgrade, delete only `default-configuration-example.md`; the next check restores
the bundled version.
