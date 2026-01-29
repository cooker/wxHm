# Security, Advanced APIs & Webhooks: Scanning, Packages, Notifications & Events

## Security Features

### Dependabot

```bash
# List Dependabot alerts
gh api repos/owner/repo/dependabot/alerts

# Get Dependabot alert
gh api repos/owner/repo/dependabot/alerts/ALERT_NUMBER

# Update Dependabot alert
gh api repos/owner/repo/dependabot/alerts/ALERT_NUMBER -X PATCH \
  -f state="dismissed" \
  -f dismissed_reason="tolerable_risk"

# List Dependabot secrets
gh api repos/owner/repo/dependabot/secrets

# Create Dependabot secret
gh secret set DEPENDABOT_SECRET --body "secret-value" --app dependabot
```

### Code Scanning

```bash
# List code scanning alerts
gh api repos/owner/repo/code-scanning/alerts

# Get code scanning alert
gh api repos/owner/repo/code-scanning/alerts/ALERT_NUMBER

# Update code scanning alert
gh api repos/owner/repo/code-scanning/alerts/ALERT_NUMBER -X PATCH \
  -f state="dismissed" \
  -f dismissed_reason="false positive"

# List code scanning analyses
gh api repos/owner/repo/code-scanning/analyses

# Get SARIF
gh api repos/owner/repo/code-scanning/sarifs/SARIF_ID

# Upload SARIF
gh api repos/owner/repo/code-scanning/sarifs -X POST \
  -f sarif="BASE64_ENCODED_SARIF" \
  -f commit_sha="COMMIT_SHA" \
  -f ref="refs/heads/main"
```

### Secret Scanning

```bash
# List secret scanning alerts
gh api repos/owner/repo/secret-scanning/alerts

# Get secret scanning alert
gh api repos/owner/repo/secret-scanning/alerts/ALERT_NUMBER

# Update secret scanning alert
gh api repos/owner/repo/secret-scanning/alerts/ALERT_NUMBER -X PATCH \
  -f state="resolved" \
  -f resolution="revoked"

# List secret scanning locations
gh api repos/owner/repo/secret-scanning/alerts/ALERT_NUMBER/locations
```

### Security Advisories

```bash
# List repository advisories
gh api repos/owner/repo/security-advisories

# Get advisory
gh api repos/owner/repo/security-advisories/GHSA_ID

# Create advisory
gh api repos/owner/repo/security-advisories -X POST \
  -f summary="Security issue" \
  -f description="Details" \
  -f severity="high"

# Update advisory
gh api repos/owner/repo/security-advisories/GHSA_ID -X PATCH \
  -f state="published"
```

## Packages

### GitHub Packages

```bash
# List packages for user
gh api user/packages

# List packages for organization
gh api orgs/orgname/packages

# Get package
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME

# Package types: npm, maven, rubygems, docker, nuget, container

# Delete package
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME -X DELETE

# List package versions
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME/versions

# Get package version
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME/versions/VERSION_ID

# Delete package version
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME/versions/VERSION_ID -X DELETE

# Restore package version
gh api users/username/packages/PACKAGE_TYPE/PACKAGE_NAME/versions/VERSION_ID/restore -X POST
```

### Container Registry

```bash
# List container packages
gh api user/packages?package_type=container

# Get container package
gh api user/packages/container/PACKAGE_NAME

# List container versions
gh api user/packages/container/PACKAGE_NAME/versions

# Delete container version
gh api user/packages/container/PACKAGE_NAME/versions/VERSION_ID -X DELETE
```

## Webhooks

### Repository Webhooks

```bash
# List webhooks
gh api repos/owner/repo/hooks

# Get webhook
gh api repos/owner/repo/hooks/HOOK_ID

# Create webhook
gh api repos/owner/repo/hooks -X POST \
  -f name="web" \
  -f config[url]="https://example.com/webhook" \
  -f config[content_type]="json" \
  -f events[]="push" \
  -f events[]="pull_request"

# Update webhook
gh api repos/owner/repo/hooks/HOOK_ID -X PATCH \
  -f events[]="push" \
  -f events[]="issues"

# Test webhook
gh api repos/owner/repo/hooks/HOOK_ID/tests -X POST

# Ping webhook
gh api repos/owner/repo/hooks/HOOK_ID/pings -X POST

# Delete webhook
gh api repos/owner/repo/hooks/HOOK_ID -X DELETE
```

### Webhook Deliveries

```bash
# List webhook deliveries
gh api repos/owner/repo/hooks/HOOK_ID/deliveries

# Get delivery
gh api repos/owner/repo/hooks/HOOK_ID/deliveries/DELIVERY_ID

# Redeliver webhook
gh api repos/owner/repo/hooks/HOOK_ID/deliveries/DELIVERY_ID/attempts -X POST
```

### Organization Webhooks

```bash
# List organization webhooks
gh api orgs/orgname/hooks

# Create organization webhook
gh api orgs/orgname/hooks -X POST \
  -f name="web" \
  -f config[url]="https://example.com/webhook" \
  -f events[]="repository" \
  -f events[]="member"
```

### Webhook Verification

```bash
# Verify webhook signature (in your webhook handler)
signature="$HTTP_X_HUB_SIGNATURE_256"
payload="$REQUEST_BODY"
secret="YOUR_WEBHOOK_SECRET"

computed=$(echo -n "$payload" | openssl dgst -sha256 -hmac "$secret" | sed 's/^.* //')
expected=$(echo "$signature" | sed 's/^sha256=//')

if [ "$computed" = "$expected" ]; then
  echo "Signature valid"
else
  echo "Signature invalid"
fi
```

## Notifications

### Notification Management

```bash
# List notifications
gh api notifications

# List repository notifications
gh api repos/owner/repo/notifications

# Mark as read
gh api notifications -X PUT

# Mark repository notifications as read
gh api repos/owner/repo/notifications -X PUT

# Get thread
gh api notifications/threads/THREAD_ID

# Mark thread as read
gh api notifications/threads/THREAD_ID -X PATCH

# Get thread subscription
gh api notifications/threads/THREAD_ID/subscription

# Set thread subscription
gh api notifications/threads/THREAD_ID/subscription -X PUT \
  -f subscribed=true

# Delete thread subscription
gh api notifications/threads/THREAD_ID/subscription -X DELETE
```

## Apps and OAuth

### GitHub Apps

```bash
# Get app
gh api app

# List installations
gh api app/installations

# Get installation
gh api app/installations/INSTALLATION_ID

# List installation repositories
gh api installation/repositories

# Create installation access token
gh api app/installations/INSTALLATION_ID/access_tokens -X POST

# Suspend installation
gh api app/installations/INSTALLATION_ID/suspended -X PUT

# Unsuspend installation
gh api app/installations/INSTALLATION_ID/suspended -X DELETE
```

### OAuth Apps

```bash
# Get app by client_id
gh api applications/CLIENT_ID/token -X POST \
  -f access_token="USER_TOKEN"

# Delete token
gh api applications/CLIENT_ID/token -X DELETE \
  -f access_token="USER_TOKEN"

# Delete grant
gh api applications/CLIENT_ID/grant -X DELETE \
  -f access_token="USER_TOKEN"
```

## Gists

### Gist Management

```bash
# List gists
gh api gists

# Get gist
gh api gists/GIST_ID

# Create gist
gh gist create file.txt --public

# Or via API
gh api gists -X POST \
  -f description="Description" \
  -f public=true \
  -f files[file.txt][content]="File content"

# Update gist
gh api gists/GIST_ID -X PATCH \
  -f description="Updated description" \
  -f files[file.txt][content]="Updated content"

# Delete gist
gh api gists/GIST_ID -X DELETE

# Star gist
gh api gists/GIST_ID/star -X PUT

# Unstar gist
gh api gists/GIST_ID/star -X DELETE

# Fork gist
gh api gists/GIST_ID/forks -X POST
```

### Gist Comments

```bash
# List comments
gh api gists/GIST_ID/comments

# Create comment
gh api gists/GIST_ID/comments -X POST \
  -f body="Comment text"

# Update comment
gh api gists/comments/COMMENT_ID -X PATCH \
  -f body="Updated comment"

# Delete comment
gh api gists/comments/COMMENT_ID -X DELETE
```

## Projects

### Projects (Classic)

```bash
# List repository projects
gh api repos/owner/repo/projects

# Create project
gh api repos/owner/repo/projects -X POST \
  -f name="Project Name" \
  -f body="Description"

# Get project
gh api projects/PROJECT_ID

# Update project
gh api projects/PROJECT_ID -X PATCH \
  -f name="Updated Name" \
  -f state="closed"

# Delete project
gh api projects/PROJECT_ID -X DELETE

# List project columns
gh api projects/PROJECT_ID/columns

# Create column
gh api projects/PROJECT_ID/columns -X POST \
  -f name="To Do"

# List cards in column
gh api projects/columns/COLUMN_ID/cards

# Create card
gh api projects/columns/COLUMN_ID/cards -X POST \
  -f note="Card content"
```

### Projects (V2 - Beta/GraphQL)

```bash
# Get organization projects
gh api graphql -f query='
  query {
    organization(login: "orgname") {
      projectsV2(first: 10) {
        nodes {
          id
          title
          url
        }
      }
    }
  }'

# Get project details
gh api graphql -f query='
  query {
    node(id: "PROJECT_ID") {
      ... on ProjectV2 {
        title
        items(first: 20) {
          nodes {
            id
            content {
              ... on Issue {
                title
                number
              }
            }
          }
        }
      }
    }
  }'
```
