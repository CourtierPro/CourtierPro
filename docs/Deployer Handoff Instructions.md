
# Deployer Handoff Instructions

The deployment pipeline has been updated to publish production-ready Docker images to the GitHub Container Registry (GHCR).

## Accessing Images

You can now pull the application images directly from GHCR without needing access to the source code.

**Backend:**

docker  pull  ghcr.io/courtierpro/courtierpro/backend:latest

**Frontend:**

docker  pull  ghcr.io/courtierpro/courtierpro/frontend:latest

## Running the Application

Use the provided

docker-compose.prod.yml  (or your own orchestration). The images are pre-configured to work out of the box.

# Update images to the latest version

docker  compose  -f  docker-compose.prod.yml  pull

# Start the application

docker  compose  -f  docker-compose.prod.yml  up  -d

## Versioning & Rollbacks

-   **Latest Version**: Use the  `:latest`  tag for the most recent stable release.
-   **Specific Version**: Every deployment also tags images with the Git commit SHA (e.g.,  `:sha-a1b2c3d`). Use these immutable tags if you need to pin a specific version or rollback.

## Verification

After deployment, you can verify image details:

docker  inspect  ghcr.io/courtierpro/courtierpro/backend:latest  |  grep  org.opencontainers

This will show the source repository and other metadata linking the container back to  `https://github.com/CourtierPro/CourtierPro`.
