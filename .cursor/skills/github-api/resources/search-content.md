# Search & Content Discovery: Repositories, Code, Issues, Users & Commits

## Search Repositories

### Basic Repository Search

```bash
# Search repositories
gh api search/repositories -f q="language:python stars:>1000"

# Search with multiple criteria
gh api search/repositories -f q="topic:machine-learning language:python pushed:>2024-01-01"

# Search with sorting
gh api search/repositories -f q="react" -f sort="stars" -f order="desc"

# Sort options: stars, forks, help-wanted-issues, updated
# Order: desc, asc
```

## Search Code

### Code Search Operations

```bash
# Search code
gh api search/code -f q="addClass in:file language:js repo:owner/repo"

# Search in organization
gh api search/code -f q="TODO org:orgname"

# Search by filename
gh api search/code -f q="filename:package.json"

# Search by path
gh api search/code -f q="path:src/components"
```

## Search Issues and PRs

### Issue and PR Search

```bash
# Search issues
gh api search/issues -f q="is:issue is:open label:bug"

# Search pull requests
gh api search/issues -f q="is:pr is:merged author:username"

# Search by date
gh api search/issues -f q="is:issue created:>2024-01-01"

# Search by state
gh api search/issues -f q="is:issue is:closed state:closed"

# Search by assignee
gh api search/issues -f q="is:issue assignee:username"
```

## Search Users

### User Discovery

```bash
# Search users
gh api search/users -f q="location:London language:python"

# Search by followers
gh api search/users -f q="followers:>1000"

# Search by repositories
gh api search/users -f q="repos:>10"
```

## Search Commits

### Commit Search

```bash
# Search commits
gh api search/commits -f q="bug fix repo:owner/repo"

# Search by author
gh api search/commits -f q="author:username"

# Search by date
gh api search/commits -f q="committer-date:>2024-01-01"
```

## GraphQL Query Examples

### Search Repositories with Details

```bash
gh api graphql -f query='
  query($searchQuery:String!) {
    search(query: $searchQuery, type: REPOSITORY, first: 10) {
      edges {
        node {
          ... on Repository {
            name
            owner {
              login
            }
            stargazerCount
            forkCount
            primaryLanguage {
              name
            }
          }
        }
      }
    }
  }' -f searchQuery="language:python stars:>1000"
```

## Common Patterns

### Find All Python Repositories by User

```bash
gh api search/repositories \
  -f q="user:username language:python" \
  --paginate \
  --jq '.items[] | {name: .name, stars: .stargazers_count, url: .html_url}'
```

### Search for TODO Comments in Code

```bash
gh api search/code -f q="TODO in:file repo:owner/repo" \
  --paginate \
  --jq '.items[] | {file: .path, repository: .repository.name}'
```

### Find Open Security-Related Issues

```bash
gh api search/issues \
  -f q="is:open label:security repo:owner/repo" \
  --paginate \
  --jq '.items[] | {number: .number, title: .title, created: .created_at}'
```
