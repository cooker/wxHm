# Repository Management: Creation, Configuration, Branches & Content

## Repository Management

### Basic Operations

```bash
# Get repository details
gh api repos/owner/repo

# List user repositories
gh api user/repos

# List organization repositories
gh api orgs/orgname/repos

# Create repository
gh repo create my-repo --public --description "My description"

# Or via API
gh api user/repos -X POST -f name="my-repo" -f private=false

# Delete repository
gh api repos/owner/repo -X DELETE

# Update repository settings
gh api repos/owner/repo -X PATCH \
  -f description="New description" \
  -f homepage="https://example.com"

# Archive repository
gh api repos/owner/repo -X PATCH -f archived=true

# Transfer repository
gh api repos/owner/repo/transfer -X POST -f new_owner="newowner"
```

## Branches and Protection

### Branch Operations

```bash
# List branches
gh api repos/owner/repo/branches

# Get branch
gh api repos/owner/repo/branches/main

# Create branch (via Git refs)
gh api repos/owner/repo/git/refs -X POST \
  -f ref="refs/heads/new-branch" \
  -f sha="COMMIT_SHA"

# Delete branch
gh api repos/owner/repo/git/refs/heads/branch-name -X DELETE
```

### Branch Protection

```bash
# Get branch protection
gh api repos/owner/repo/branches/main/protection

# Enable branch protection
gh api repos/owner/repo/branches/main/protection -X PUT \
  -f required_status_checks[strict]=true \
  -f required_pull_request_reviews[required_approving_review_count]=2 \
  -f enforce_admins=true

# Update branch protection
gh api repos/owner/repo/branches/main/protection -X PATCH \
  -f required_pull_request_reviews[required_approving_review_count]=1
```

## Commits and Content

### Viewing Commits

```bash
# List commits
gh api repos/owner/repo/commits

# Get specific commit
gh api repos/owner/repo/commits/SHA

# Compare commits
gh api repos/owner/repo/compare/base...head
```

### File Operations

```bash
# Get file contents
gh api repos/owner/repo/contents/path/to/file

# Create or update file
gh api repos/owner/repo/contents/path/to/file -X PUT \
  -f message="Commit message" \
  -f content="BASE64_ENCODED_CONTENT"

# Delete file
gh api repos/owner/repo/contents/path/to/file -X DELETE \
  -f message="Delete file" \
  -f sha="FILE_BLOB_SHA"
```

## Releases and Tags

### Release Management

```bash
# List releases
gh api repos/owner/repo/releases

# Get latest release
gh api repos/owner/repo/releases/latest

# Create release
gh release create v1.0.0 --title "Version 1.0.0" --notes "Release notes"

# Or via API
gh api repos/owner/repo/releases -X POST \
  -f tag_name="v1.0.0" \
  -f name="Version 1.0.0" \
  -f body="Release notes"

# Upload release asset
gh release upload v1.0.0 ./artifact.zip

# Delete release
gh api repos/owner/repo/releases/RELEASE_ID -X DELETE
```

### Tag Operations

```bash
# List tags
gh api repos/owner/repo/tags

# Create tag
gh api repos/owner/repo/git/tags -X POST \
  -f tag="v1.0.0" \
  -f message="Version 1.0.0" \
  -f object="COMMIT_SHA" \
  -f type="commit"
```

## Git Data (Low-Level)

### Blobs

```bash
# Get blob
gh api repos/owner/repo/git/blobs/SHA

# Create blob
gh api repos/owner/repo/git/blobs -X POST \
  -f content="File content" \
  -f encoding="utf-8"
```

### Trees

```bash
# Get tree
gh api repos/owner/repo/git/trees/SHA

# Get tree recursively
gh api repos/owner/repo/git/trees/SHA?recursive=1

# Create tree
gh api repos/owner/repo/git/trees -X POST \
  -f base_tree="BASE_SHA" \
  -f tree[][path]="file.txt" \
  -f tree[][mode]="100644" \
  -f tree[][type]="blob" \
  -f tree[][sha]="BLOB_SHA"
```

### Commits (Git Level)

```bash
# Get commit
gh api repos/owner/repo/git/commits/SHA

# Create commit
gh api repos/owner/repo/git/commits -X POST \
  -f message="Commit message" \
  -f tree="TREE_SHA" \
  -f parents[]="PARENT_SHA"
```

### References

```bash
# List references
gh api repos/owner/repo/git/refs

# Get reference
gh api repos/owner/repo/git/refs/heads/main

# Create reference
gh api repos/owner/repo/git/refs -X POST \
  -f ref="refs/heads/feature" \
  -f sha="COMMIT_SHA"

# Update reference
gh api repos/owner/repo/git/refs/heads/feature -X PATCH \
  -f sha="NEW_SHA" \
  -f force=true

# Delete reference
gh api repos/owner/repo/git/refs/heads/feature -X DELETE
```
