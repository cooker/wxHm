# GitHub Actions & Workflows: CI/CD Automation, Runs, Artifacts & Secrets

## Workflow Management

### Workflow Operations

```bash
# List workflows
gh api repos/owner/repo/actions/workflows

# Get workflow
gh api repos/owner/repo/actions/workflows/WORKFLOW_ID

# Enable/disable workflow
gh api repos/owner/repo/actions/workflows/WORKFLOW_ID/enable -X PUT
gh api repos/owner/repo/actions/workflows/WORKFLOW_ID/disable -X PUT

# Trigger workflow dispatch
gh api repos/owner/repo/actions/workflows/WORKFLOW_ID/dispatches -X POST \
  -f ref="main" \
  -f inputs[key]="value"

# Or using gh CLI
gh workflow run workflow.yml -f key=value
```

## Workflow Runs

### Run Management

```bash
# List workflow runs
gh api repos/owner/repo/actions/runs

# List runs for specific workflow
gh api repos/owner/repo/actions/workflows/WORKFLOW_ID/runs

# Get workflow run
gh api repos/owner/repo/actions/runs/RUN_ID

# Re-run workflow
gh api repos/owner/repo/actions/runs/RUN_ID/rerun -X POST

# Cancel workflow run
gh api repos/owner/repo/actions/runs/RUN_ID/cancel -X POST

# Delete workflow run
gh api repos/owner/repo/actions/runs/RUN_ID -X DELETE
```

### Run Jobs and Logs

```bash
# List workflow run jobs
gh api repos/owner/repo/actions/runs/RUN_ID/jobs

# Get job logs
gh api repos/owner/repo/actions/jobs/JOB_ID/logs
```

## Artifacts and Cache

### Artifact Operations

```bash
# List artifacts
gh api repos/owner/repo/actions/artifacts

# Download artifact
gh api repos/owner/repo/actions/artifacts/ARTIFACT_ID/zip > artifact.zip

# Delete artifact
gh api repos/owner/repo/actions/artifacts/ARTIFACT_ID -X DELETE
```

### Cache Operations

```bash
# List caches
gh api repos/owner/repo/actions/caches

# Delete cache
gh api repos/owner/repo/actions/caches/CACHE_ID -X DELETE
```

## Secrets and Variables

### Repository Secrets

```bash
# List repository secrets
gh api repos/owner/repo/actions/secrets

# Create/update secret
gh secret set SECRET_NAME --body "secret-value"

# Delete secret
gh api repos/owner/repo/actions/secrets/SECRET_NAME -X DELETE
```

### Repository Variables

```bash
# List variables
gh api repos/owner/repo/actions/variables

# Create variable
gh api repos/owner/repo/actions/variables -X POST \
  -f name="VAR_NAME" \
  -f value="var-value"

# Update variable
gh api repos/owner/repo/actions/variables/VAR_NAME -X PATCH \
  -f value="new-value"
```

### Organization Secrets and Variables

```bash
# List organization secrets
gh api orgs/orgname/actions/secrets

# List organization variables
gh api orgs/orgname/actions/variables

# Create organization secret
gh api orgs/orgname/actions/secrets -X POST \
  -f name="SECRET_NAME" \
  -f visibility="private"

# Create organization variable
gh api orgs/orgname/actions/variables -X POST \
  -f name="VAR_NAME" \
  -f value="var-value" \
  -f visibility="private"
```

## Self-hosted Runners

### Runner Management

```bash
# List runners
gh api repos/owner/repo/actions/runners

# Get runner
gh api repos/owner/repo/actions/runners/RUNNER_ID

# Delete runner
gh api repos/owner/repo/actions/runners/RUNNER_ID -X DELETE

# Generate registration token
gh api repos/owner/repo/actions/runners/registration-token -X POST

# List runner applications
gh api repos/owner/repo/actions/runners/downloads
```

## Common Patterns

### Trigger Workflow with Inputs

```bash
gh workflow run build.yml \
  -f environment=production \
  -f version=1.0.0
```

### Monitor Workflow Status

```bash
# Get latest run status
gh api repos/owner/repo/actions/runs \
  -f per_page=1 \
  --jq '.workflow_runs[0].conclusion'
```

### Clean Up Old Artifacts

```bash
# Delete artifacts older than 30 days
gh api repos/owner/repo/actions/artifacts \
  --paginate \
  --jq '.artifacts[] | select(.created_at < now - 30*86400) | .id' \
  | while read artifact_id; do
    gh api repos/owner/repo/actions/artifacts/$artifact_id -X DELETE
  done
```
