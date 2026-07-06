# Repository Build Policy

## Docker-only compilation

All compilation, package, build, and automated test commands for this repository must run inside Docker containers.

Do not run build commands directly on the host machine, including:

- `mvn test`, `mvn package`, or other Maven build phases.
- `npm run build:*`, `npm test`, `npm run test:*`, or other Node build/test scripts.
- Local Java or Node compiler commands used as substitutes for the above.

Allowed host-side commands are limited to non-compilation operations such as `git`, `rg`, `sed`, `curl`, `ssh`, and file inspection.

Use the Docker verification script when possible:

```bash
scripts/docker/verify-builds.sh
```

The script runs the backend tests in a Maven container and builds the backend, admin web, and H5 Docker images using the repository Dockerfiles.
