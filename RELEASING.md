# Releasing AtmosSway

GitHub Actions publishes stable AtmosSway releases to GitHub and Modrinth from version tags. The build uses Java 21 and produces one jar named:

```text
atmossway-<version>-NEO-<minecraft-version>.jar
```

For example, version `1.5.0` for Minecraft `1.21.1` produces `atmossway-1.5.0-NEO-1.21.1.jar`.

## One-time setup

Create a Modrinth personal access token that can create versions for project `m4ChLkSm`, then save it as the repository Actions secret `MODRINTH_TOKEN`. Never commit the token or include it in workflow logs.

## Release checklist

1. Update `mod_version` and, when applicable, `minecraft_version` in `gradle.properties`.
2. Review `.github/modrinth-dependencies.json`. Set its `atmos_version` to the new AtmosSway version even when the relationships did not change. Add or remove required and optional project relationships to match that release.
3. Run `./gradlew clean build -PskipDevelopmentRuntime=true` and confirm the jar has the expected name.
4. Merge the release commit into `main`.
5. Create and push the exact tag `v<version>-NEO-<minecraft-version>` from that commit. For example:

   ```powershell
   git tag v1.5.0-NEO-1.21.1
   git push origin v1.5.0-NEO-1.21.1
   ```

The workflow rejects a tag that differs from the Gradle properties or does not point to a commit contained in `main`. It publishes GitHub first, then Modrinth. A Modrinth failure leaves the GitHub Release intact, and rerunning the workflow safely verifies existing files before continuing.

Dependency entries intentionally reference Modrinth projects instead of exact dependency versions. AtmosSway supports version ranges, while Modrinth dependency metadata can only link a whole project or one exact version.
