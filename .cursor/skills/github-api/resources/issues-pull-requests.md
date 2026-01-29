# Issues & Pull Requests: Tracking, Reviews, and Code Collaboration

## Issue Management

### Basic Operations

```bash
# List issues
gh api repos/owner/repo/issues

# Get issue
gh api repos/owner/repo/issues/123

# Create issue
gh issue create --title "Bug report" --body "Description"

# Or via API
gh api repos/owner/repo/issues -X POST \
  -f title="Bug report" \
  -f body="Description" \
  -f labels[]=bug

# Update issue
gh api repos/owner/repo/issues/123 -X PATCH \
  -f state="closed" \
  -f labels[]=resolved

# Close issue
gh issue close 123

# Reopen issue
gh issue reopen 123

# Lock issue
gh api repos/owner/repo/issues/123/lock -X PUT
```

### Labels and Milestones

```bash
# List labels
gh api repos/owner/repo/labels

# Create label
gh api repos/owner/repo/labels -X POST \
  -f name="bug" \
  -f color="d73a4a" \
  -f description="Something isn't working"

# Add labels to issue
gh api repos/owner/repo/issues/123/labels -X POST \
  -f labels[]=bug -f labels[]=priority-high

# List milestones
gh api repos/owner/repo/milestones

# Create milestone
gh api repos/owner/repo/milestones -X POST \
  -f title="v1.0" \
  -f description="First release" \
  -f due_on="2025-12-31T23:59:59Z"

# Set issue milestone
gh api repos/owner/repo/issues/123 -X PATCH -f milestone=1
```

### Assignees and Reactions

```bash
# Add assignees
gh api repos/owner/repo/issues/123/assignees -X POST \
  -f assignees[]=username1 -f assignees[]=username2

# Add reaction to issue
gh api repos/owner/repo/issues/123/reactions -X POST \
  -f content="+1"

# Reaction types: +1, -1, laugh, confused, heart, hooray, rocket, eyes
```

### Comments

```bash
# List issue comments
gh api repos/owner/repo/issues/123/comments

# Create comment
gh api repos/owner/repo/issues/123/comments -X POST \
  -f body="Comment text"

# Update comment
gh api repos/owner/repo/issues/comments/COMMENT_ID -X PATCH \
  -f body="Updated comment"

# Delete comment
gh api repos/owner/repo/issues/comments/COMMENT_ID -X DELETE
```

## Pull Request Management

### Basic Operations

```bash
# List pull requests
gh api repos/owner/repo/pulls

# Get pull request
gh api repos/owner/repo/pulls/123

# Create pull request
gh pr create --title "Feature" --body "Description" --base main --head feature-branch

# Or via API
gh api repos/owner/repo/pulls -X POST \
  -f title="Feature" \
  -f body="Description" \
  -f head="feature-branch" \
  -f base="main"

# Update pull request
gh api repos/owner/repo/pulls/123 -X PATCH \
  -f title="Updated title" \
  -f state="closed"

# Close without merging
gh pr close 123
```

### Merging

```bash
# Merge pull request
gh pr merge 123 --merge

# Or via API
gh api repos/owner/repo/pulls/123/merge -X PUT \
  -f merge_method="merge"

# Merge methods: merge, squash, rebase
```

### Reviews and Approvals

```bash
# List reviews
gh api repos/owner/repo/pulls/123/reviews

# Create review
gh api repos/owner/repo/pulls/123/reviews -X POST \
  -f body="Looks good!" \
  -f event="APPROVE"

# Review events: APPROVE, REQUEST_CHANGES, COMMENT

# Request reviewers
gh api repos/owner/repo/pulls/123/requested_reviewers -X POST \
  -f reviewers[]=username1 -f reviewers[]=username2

# Dismiss review
gh api repos/owner/repo/pulls/123/reviews/REVIEW_ID/dismissals -X PUT \
  -f message="No longer relevant"
```

### PR Files and Commits

```bash
# List PR files
gh api repos/owner/repo/pulls/123/files

# List PR commits
gh api repos/owner/repo/pulls/123/commits

# Create review comment on code
gh api repos/owner/repo/pulls/123/comments -X POST \
  -f body="Comment on this line" \
  -f commit_id="COMMIT_SHA" \
  -f path="file.js" \
  -f line=42

# List review comments
gh api repos/owner/repo/pulls/123/comments
```

## GraphQL Query Examples

### Repository with Issues and PRs

```bash
gh api graphql -f query='
  query($owner:String!, $repo:String!) {
    repository(owner: $owner, name: $repo) {
      name
      description
      issues(first: 10, states: OPEN) {
        nodes {
          number
          title
          author {
            login
          }
        }
      }
      pullRequests(first: 10, states: OPEN) {
        nodes {
          number
          title
          author {
            login
          }
        }
      }
    }
  }' -f owner="owner" -f repo="repo"
```

### Add Comment Mutation

```bash
gh api graphql -f query='
  mutation($subjectId:ID!, $body:String!) {
    addComment(input: {subjectId: $subjectId, body: $body}) {
      commentEdge {
        node {
          id
          body
        }
      }
    }
  }' -f subjectId="ISSUE_NODE_ID" -f body="Comment text"
```

### Close Issue Mutation

```bash
gh api graphql -f query='
  mutation($issueId:ID!) {
    closeIssue(input: {issueId: $issueId}) {
      issue {
        id
        state
      }
    }
  }' -f issueId="ISSUE_NODE_ID"
```

### Add Reaction Mutation

```bash
gh api graphql -f query='
  mutation($subjectId:ID!, $content:ReactionContent!) {
    addReaction(input: {subjectId: $subjectId, content: $content}) {
      reaction {
        id
        content
      }
    }
  }' -f subjectId="COMMENT_NODE_ID" -f content="THUMBS_UP"
```

## Common Patterns

### Bulk Operations

```bash
# Close multiple issues
for issue in 1 2 3 4 5; do
  gh api repos/owner/repo/issues/$issue -X PATCH -f state="closed"
  sleep 1  # Rate limiting courtesy
done

# Add label to multiple issues
issues=(1 2 3 4 5)
for issue in "${issues[@]}"; do
  gh api repos/owner/repo/issues/$issue/labels -X POST -f labels[]=bug
done
```

### Auto-merge Dependabot PRs

```bash
gh api graphql -f query='
  query {
    repository(owner: "owner", name: "repo") {
      pullRequests(first: 10, states: OPEN) {
        nodes {
          number
          author {
            login
          }
          mergeable
        }
      }
    }
  }' | jq -r '.data.repository.pullRequests.nodes[] | select(.author.login == "dependabot") | select(.mergeable == "MERGEABLE") | .number' | while read pr; do
  gh pr merge "$pr" --auto --squash
done
```
