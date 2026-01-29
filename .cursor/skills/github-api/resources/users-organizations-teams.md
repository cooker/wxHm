# Users, Organizations & Teams: Accounts, Permissions, and Collaboration

## User Information

### User Operations

```bash
# Get authenticated user
gh api user

# Get user by username
gh api users/username

# Update authenticated user
gh api user -X PATCH \
  -f bio="My bio" \
  -f location="City, Country" \
  -f blog="https://example.com"

# List user repositories
gh api users/username/repos

# List user gists
gh api users/username/gists
```

### Followers and Following

```bash
# List user followers
gh api users/username/followers

# List user following
gh api users/username/following

# Check if user follows another
gh api user/following/username

# Follow user
gh api user/following/username -X PUT

# Unfollow user
gh api user/following/username -X DELETE
```

## Organization Management

### Organization Information

```bash
# Get organization
gh api orgs/orgname

# Update organization
gh api orgs/orgname -X PATCH \
  -f description="Org description" \
  -f location="City"

# List organization members
gh api orgs/orgname/members

# Check membership
gh api orgs/orgname/members/username

# Remove member
gh api orgs/orgname/members/username -X DELETE

# List organization teams
gh api orgs/orgname/teams

# List organization repositories
gh api orgs/orgname/repos

# List organization projects
gh api orgs/orgname/projects
```

## Teams

### Team Operations

```bash
# List teams
gh api orgs/orgname/teams

# Get team
gh api orgs/orgname/teams/teamslug

# Create team
gh api orgs/orgname/teams -X POST \
  -f name="Team Name" \
  -f description="Team description" \
  -f privacy="closed"

# Update team
gh api orgs/orgname/teams/teamslug -X PATCH \
  -f description="Updated description"

# Delete team
gh api orgs/orgname/teams/teamslug -X DELETE
```

### Team Membership

```bash
# List team members
gh api orgs/orgname/teams/teamslug/members

# Add team member
gh api orgs/orgname/teams/teamslug/memberships/username -X PUT

# Remove team member
gh api orgs/orgname/teams/teamslug/memberships/username -X DELETE
```

### Team Repositories

```bash
# List team repositories
gh api orgs/orgname/teams/teamslug/repos

# Add repository to team
gh api orgs/orgname/teams/teamslug/repos/owner/repo -X PUT \
  -f permission="push"

# Permissions: pull, push, admin, maintain, triage
```

## GraphQL Examples

### User Contributions

```bash
gh api graphql -f query='
  query($username:String!) {
    user(login: $username) {
      contributionsCollection {
        contributionCalendar {
          totalContributions
          weeks {
            contributionDays {
              contributionCount
              date
            }
          }
        }
      }
    }
  }' -f username="username"
```

### Organization Overview

```bash
gh api graphql -f query='
  query($orgName:String!) {
    organization(login: $orgName) {
      name
      description
      members(first: 10) {
        totalCount
        nodes {
          login
          name
        }
      }
      repositories(first: 10) {
        totalCount
        nodes {
          name
          isPrivate
          stargazerCount
        }
      }
    }
  }' -f orgName="orgname"
```
