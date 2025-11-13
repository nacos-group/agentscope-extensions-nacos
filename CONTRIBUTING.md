# Contributing to AgentScope Extensions Nacos

Thank you for your interest in contributing to AgentScope Extensions Nacos! This document provides guidelines and instructions for contributing to this project.

## Getting Started

### Prerequisites

- **For Java Development:**
  - JDK 8 or higher
  - Maven 3.6+

- **For Python Development:**
  - Python 3.7 or higher
  - pip

### Setting Up Your Development Environment

1. **Fork the Repository**
   
   Fork the repository to your GitHub account by clicking the "Fork" button on the repository page.

2. **Clone Your Fork**
   
   ```bash
   git clone https://github.com/YOUR-USERNAME/agentscope-extensions-nacos.git
   cd agentscope-extensions-nacos
   ```

3. **Add Upstream Remote**
   
   ```bash
   git remote add upstream https://github.com/nacos-group/agentscope-extensions-nacos.git
   ```

## Contributing Workflow

### 1. Create a Feature Branch

Always create a new branch for your changes:

```bash
git checkout -b feature/your-feature-name
```

or for bug fixes:

```bash
git checkout -b fix/issue-description
```

### 2. Make Your Changes

- Write clean, maintainable code
- Follow the existing code style
- Add tests for new features
- Update documentation as needed

### 3. Test Your Changes

**For Java:**
```bash
cd java
mvn clean test
```

**For Python:**
```bash
cd python
python -m unittest discover -s tests
```

### 4. Commit Your Changes

Write clear, descriptive commit messages:

```bash
git add .
git commit -m "feat: add new feature description"
```

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:
- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `test:` for test additions or changes
- `chore:` for maintenance tasks

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create a Pull Request

1. Go to the [repository page](https://github.com/nacos-group/agentscope-extensions-nacos)
2. Click "Pull Requests" → "New Pull Request"
3. Click "compare across forks"
4. Select your fork and branch
5. Fill in the PR template with:
   - Description of changes
   - Related issue number (if applicable)
   - Testing done
   - Screenshots (if UI changes)

## Troubleshooting Common Issues

### 403 Error When Pushing

If you encounter a 403 error when trying to push, here are common causes and solutions:

#### 1. **Pushing to the Wrong Remote**

**Problem:** You're trying to push directly to the upstream repository instead of your fork.

**Solution:** Make sure you're pushing to your fork:
```bash
# Check your remotes
git remote -v

# Should show:
# origin    https://github.com/YOUR-USERNAME/agentscope-extensions-nacos.git
# upstream  https://github.com/nacos-group/agentscope-extensions-nacos.git

# Push to your fork (origin), not upstream
git push origin your-branch-name
```

#### 2. **Authentication Issues**

**Problem:** Your credentials are not properly configured.

**Solution for HTTPS:**
```bash
# Use a Personal Access Token (PAT) instead of password
# Generate a PAT at: https://github.com/settings/tokens
# Use it as your password when prompted
```

**Solution for SSH:**
```bash
# Set up SSH keys if not already done
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add SSH key to GitHub account
# Copy your public key
cat ~/.ssh/id_ed25519.pub

# Add it at: https://github.com/settings/keys

# Change remote to SSH
git remote set-url origin git@github.com:YOUR-USERNAME/agentscope-extensions-nacos.git
```

#### 3. **Team Access Without Fork Permissions**

**Problem:** You have team/maintain privileges but haven't set up your fork correctly.

**Solution:**
- Even with maintain privileges, it's recommended to use the fork workflow
- Fork the repository and push to your fork
- Create pull requests from your fork to the main repository

#### 4. **Repository Settings**

Some repositories require all contributions via pull requests from forks, even for maintainers. This is a best practice that:
- Ensures code review for all changes
- Maintains a clean commit history
- Allows CI/CD to run on all changes

### Unable to Install Dependencies

**For Java:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Try install again
cd java
mvn clean install
```

**For Python:**
```bash
# Create a virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
cd python
pip install -e .
```

## Code Style Guidelines

### Java
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Add JavaDoc comments for public APIs

### Python
- Follow PEP 8 style guide
- Use type hints where applicable
- Write docstrings for functions and classes
- Keep functions focused on a single responsibility

## Questions or Need Help?

- Open an issue for bugs or feature requests
- Check existing issues before creating new ones
- Be respectful and constructive in discussions
- Provide detailed information when reporting bugs

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0, the same license as this project.

---

Thank you for contributing to AgentScope Extensions Nacos! 🎉
