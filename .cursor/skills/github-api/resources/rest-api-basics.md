# REST API Basics: Authentication, Pagination, Rate Limiting & Error Handling

## Authentication Methods

### 1. GitHub CLI (`gh`)
The `gh` CLI is the recommended method when available:

```bash
# Authenticate
gh auth login

# Check authentication status
gh auth status

# Use authenticated requests
gh api /user
gh api repos/owner/repo/issues
```

### 2. Personal Access Token (PAT)
For direct API calls:

```bash
# Classic PAT
curl -H "Authorization: token YOUR_TOKEN" \
  https://api.github.com/user

# Fine-grained PAT (recommended)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/user
```

### 3. GitHub Apps
For building integrations:

```bash
# Installation access token
curl -H "Authorization: Bearer INSTALLATION_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/installation/repositories
```

### 4. OAuth Apps
For user authentication flows:

```bash
# After OAuth flow completion
curl -H "Authorization: token USER_ACCESS_TOKEN" \
  https://api.github.com/user
```

## API Versions

### REST API (v3)
Base URL: `https://api.github.com`

```bash
# Using gh CLI
gh api /repos/owner/repo

# Using curl
curl -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/owner/repo
```

### GraphQL API (v4)
Endpoint: `https://api.github.com/graphql`

```bash
# Using gh CLI
gh api graphql -f query='
  query {
    viewer {
      login
      name
    }
  }'

# Using curl
curl -H "Authorization: Bearer TOKEN" \
  -X POST -d '{"query":"query { viewer { login name } }"}' \
  https://api.github.com/graphql
```

## Rate Limiting

### Check Rate Limit

```bash
# Get rate limit status
gh api rate_limit

# Check specific rate limit
gh api rate_limit | jq '.resources.core'

# Rate limit headers are included in all API responses:
# X-RateLimit-Limit: Maximum requests per hour
# X-RateLimit-Remaining: Remaining requests
# X-RateLimit-Reset: Time when limit resets (Unix timestamp)
```

### Rate Limit Tiers

- **Authenticated requests**: 5,000 requests/hour
- **Unauthenticated requests**: 60 requests/hour
- **GraphQL**: 5,000 points/hour (varies by query complexity)
- **Search**: 30 requests/minute
- **GitHub Actions**: 1,000 requests/hour per repository

### Best Practices

1. **Use conditional requests** with ETags:
```bash
# First request
response=$(gh api repos/owner/repo -i)
etag=$(echo "$response" | grep -i etag | cut -d' ' -f2)

# Subsequent request
gh api repos/owner/repo -H "If-None-Match: $etag"
# Returns 304 Not Modified if unchanged (doesn't count against rate limit)
```

2. **Use GraphQL for complex queries** (more efficient than multiple REST calls)

3. **Implement exponential backoff** when hitting rate limits

4. **Cache responses** when appropriate

## Pagination

### REST API Pagination

```bash
# Default: 30 items per page, max: 100
gh api repos/owner/repo/issues --paginate

# Or manually with per_page and page
gh api repos/owner/repo/issues -f per_page=100 -f page=1

# Link header provides navigation:
# Link: <https://api.github.com/repos/owner/repo/issues?page=2>; rel="next",
#       <https://api.github.com/repos/owner/repo/issues?page=5>; rel="last"
```

### GraphQL Pagination (Cursor-based)

```bash
gh api graphql -f query='
  query($cursor:String) {
    repository(owner: "owner", name: "repo") {
      issues(first: 100, after: $cursor) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          number
          title
        }
      }
    }
  }' -f cursor="$end_cursor"
```

## Error Handling

### Common HTTP Status Codes

- `200 OK`: Success
- `201 Created`: Resource created successfully
- `204 No Content`: Success with no response body
- `304 Not Modified`: Resource hasn't changed (with conditional requests)
- `400 Bad Request`: Invalid request
- `401 Unauthorized`: Authentication required or failed
- `403 Forbidden`: Authentication succeeded but not authorized / rate limited
- `404 Not Found`: Resource doesn't exist
- `422 Unprocessable Entity`: Validation failed
- `500 Internal Server Error`: GitHub server error
- `502 Bad Gateway`: GitHub temporarily unavailable
- `503 Service Unavailable`: GitHub in maintenance mode

### Error Response Format

```json
{
  "message": "Validation Failed",
  "errors": [
    {
      "resource": "Issue",
      "field": "title",
      "code": "missing_field"
    }
  ],
  "documentation_url": "https://docs.github.com/rest/issues/issues#create-an-issue"
}
```

### Handling Errors

```bash
# Check response status
response=$(gh api repos/owner/repo -i 2>&1)
if echo "$response" | grep -q "HTTP/2 404"; then
  echo "Repository not found"
elif echo "$response" | grep -q "HTTP/2 403"; then
  echo "Rate limited or forbidden"
fi

# Use jq to parse error messages
gh api repos/owner/repo 2>&1 | jq -r '.message'
```

## Best Practices

### 1. Use Appropriate Authentication
- Use fine-grained PATs with minimal scopes
- Prefer GitHub Apps for integrations
- Use `gh` CLI when available

### 2. Optimize API Usage
- Use GraphQL for complex data requirements
- Implement pagination properly
- Use conditional requests with ETags
- Batch operations when possible

### 3. Security
- Never commit tokens to repositories
- Rotate tokens regularly
- Use minimal required permissions
- Validate webhook signatures
