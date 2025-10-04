# SonarQube Setup for WOM Auth Service API

## Local Setup

### 1. Start SonarQube with Docker

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:latest
```

### 2. Access SonarQube

- URL: http://localhost:9000
- Default credentials: admin/admin
- Change password on first login

### 3. Create Project

1. Click "Create Project" → "Manually"
2. Project key: `wom-auth-service-api`
3. Display name: `WOM Auth Service API`
4. Generate a token and save it

### 4. Run Analysis Locally

```bash
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=wom-auth-service-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE
```

## GitHub Actions Setup

### Required Secrets

Add these secrets in GitHub repository settings (Settings → Secrets and variables → Actions → New repository secret):

1. **SONAR_TOKEN**: Token generated from SonarQube
2. **SONAR_HOST_URL**: Your SonarQube URL (e.g., https://sonarcloud.io or http://your-server:9000)
3. **NVD_API_KEY**: API Key from National Vulnerability Database (https://nvd.nist.gov/developers/request-an-api-key)
4. **CODECOV_TOKEN**: Token from Codecov (optional)

### Using SonarCloud (Free for Open Source)

1. Go to https://sonarcloud.io
2. Sign in with GitHub
3. Import your repository
4. Get the token from Account → Security
5. Add `SONAR_TOKEN` and `SONAR_HOST_URL=https://sonarcloud.io` to GitHub secrets

## Quality Gates

The pipeline enforces:

- **Code Coverage**: Minimum 80%
- **Duplications**: Less than 3%
- **Maintainability Rating**: A
- **Reliability Rating**: A
- **Security Rating**: A

## OWASP Dependency Check

Automatically scans for known vulnerabilities in dependencies.

- **Threshold**: CVSS >= 7 (High and Critical)
- **NVD API Key**: Required for accessing National Vulnerability Database
- **Reports**: Available in GitHub Actions artifacts

### Get NVD API Key

1. Visit: https://nvd.nist.gov/developers/request-an-api-key
2. Enter your email and request API key
3. Check your email for the API key
4. Add it as `NVD_API_KEY` secret in GitHub

## Trivy Security Scan

Scans for vulnerabilities in code and dependencies.

- Results uploaded to GitHub Security tab
- SARIF format for integration with GitHub Advanced Security
