# Security Policy

## Reporting a Vulnerability

If you discover a security issue in UniversalImageStudio, please report it privately instead of publishing sensitive details in a public Issue, Pull Request, commit message, screenshot, or chat transcript.

Recommended report contents:

- A clear description of the vulnerability.
- Steps to reproduce the issue.
- Affected version, commit, or build artifact if known.
- Relevant logs with secrets removed.
- Suggested mitigation if available.

## Secret Handling

Do not disclose API keys, GitHub tokens, signing keys, keystores, passwords, `.env` files, or other credentials in public project channels.

If a secret is accidentally exposed:

1. Revoke or rotate the exposed credential immediately.
2. Remove the secret from local files, logs, screenshots, and comments.
3. Check repository settings, GitHub Actions secrets, release assets, and workflow logs for accidental exposure.
4. Create a replacement credential with the minimum required permissions.
5. Re-run affected workflows only after the secret has been rotated.

## GitHub Actions and Release Signing

Release signing material should be stored only in GitHub Actions secrets or another trusted secret manager. Do not commit keystores or signing property files to the repository.

The repository `.gitignore` is configured to exclude common Android signing files and local environment files, but maintainers should still review changes before committing.

## Supported Versions

This project is maintained from the `main` branch. Security fixes are expected to target the latest maintained source state unless a separate release branch is explicitly created.

## Responsible Disclosure

Please allow reasonable time for investigation and remediation before public disclosure.
