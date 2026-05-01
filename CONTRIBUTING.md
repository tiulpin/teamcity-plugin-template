# Contributing

Thanks for considering a contribution.

## Development loop

```bash
./gradlew check                # compile + run all tests
./gradlew serverPlugin         # build the plugin zip (server/build/distributions)
./gradlew startTeamcity        # boot a sandbox at http://localhost:8111
./gradlew deployToTeamcity     # hot-redeploy without restarting the sandbox
./gradlew stopTeamcity         # tear it down
```

## Pull requests

- Keep changes focused. One concern per PR.
- Add or update tests under `*/src/test/kotlin/`.
- Update `CHANGELOG.md` under `[Unreleased]` for any user-visible change. The
  release workflow rotates the section automatically when a tag is cut, so don't
  add version headers by hand.
- Use a PR title that reads well in release notes — Release Drafter pipes titles
  straight into the next draft.

## Releasing

1. Decide on the next version (e.g. `1.2.3`) and ensure `[Unreleased]` in
   `CHANGELOG.md` describes the diff since the previous tag.
2. Bump `pluginVersion` in `gradle.properties`.
3. Run `./gradlew patchChangelog` locally to rotate `[Unreleased]` into a
   versioned section, and commit.
4. Tag the commit (`git tag v1.2.3 && git push --tags`) — or trigger the
   `publish` job manually in the TeamCity UI. That job runs
   `./gradlew publishPlugin`, which uploads to the JetBrains Marketplace using
   the `marketplace-token` secret declared in `.teamcity.yml`.
