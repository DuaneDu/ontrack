Ontrack Web Core (Next UI)
==========================

## Architecture Decisions Records

See [ADR](ADR.md).

## Local development

Run the main application, this starts the API on http://localhost:8080
and the legacy UI remains available.

> The middleware must first be made available by running
> `./gradlew devStart`

Run the UI locally:

```bash
cd ontrack-web-core
npm run dev
```

This script runs NextJS on http://localhost:3000.

> Behind the scene, the UI connects to the http://localhost:8080 API.
> `admin/admin` credentials are always used.
>
> This behaviour is driven by the values in the `.env.development` file.