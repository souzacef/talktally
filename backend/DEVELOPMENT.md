# Backend development tasks

Normal validation is offline and uses H2:

```bash
./gradlew clean test
./gradlew check
```

Real PostgreSQL validation is opt-in and requires a Docker-compatible container runtime:

```bash
./gradlew postgresIntegrationTest
```

Build the production image from the repository root:

```bash
docker build -t talktally-backend .
```

The image activates the `prod` profile and requires database and JWT environment variables at runtime. No secrets are included in the image.
