# TeamCity Plugin Template

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![TeamCity 2025.11](https://img.shields.io/badge/TeamCity-2025.11-blue)](https://www.jetbrains.com/teamcity/)
[![Java 21](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![Kotlin 2.2.21](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)

A batteries-included scaffold for TeamCity plugins. Click **Use this template**, clone, run `./gradlew startTeamcity`,
and you have a working plugin loaded in a sandboxed TeamCity 2025.11 — with six end-to-end-verified extension points,
unit tests, hot reload, and a CI/CD pipeline already wired in.

## What you get

- **3-module Gradle project** (`server` + `agent` + `common`) compiling against TeamCity 2025.11 on Java 21 with Kotlin
  2.2.21 — matches the toolchain TeamCity itself uses, ready for the 2026.1 Java-21 mandate.
- **Six working samples**, one per common extension point: health report, build server listener, REST controller,
  build feature, page extension (with JSP), and a full-stack build runner spanning server + agent + shared constants.
  All six are verified working in a live TeamCity 2025.11 (REST + UI + build log).
- **One-command local sandbox.** `./gradlew startTeamcity` boots TeamCity locally with your plugin pre-deployed.
  `./gradlew deployToTeamcity` swaps the plugin in-place without losing the session.
- **TeamCity-native CI in `.teamcity.yml`.** Matrix build against TC 2024.12 / 2025.07 / 2025.11, plus a manual
  `publish` job that uploads to JetBrains Marketplace.
- **Nine passing unit tests** (JUnit 4 + mockk) covering the samples that have non-trivial logic.
- **Repo hygiene** — `.editorconfig`, `dependabot.yml`, issue/PR templates, CHANGELOG with `gradle-changelog-plugin`,
  Apache-2.0 license — everything you'd otherwise add yourself.
- **A "Conventions & gotchas" section below** that documents the load-bearing details the official docs gloss over:
  Bean.xml filename pattern, autowire-by-constructor, the three classloader flags, `min-build` semantics, etc.

## Why this template?

The official [Getting Started with Plugin Development](https://plugins.jetbrains.com/docs/teamcity/getting-started-with-plugin-development.html)
guide is a Maven-archetype tutorial: you end with a single "Hello world" controller targeting TC 2021.2 on Java 8 and
no tests, CI, Marketplace flow, or hot-reload story. It's the right place to *learn* the moving parts.

This template is the next step — start here when you're past the tutorial and want a project that already has all the
pieces wired and verified on a 2026.1-ready stack.

## Quickstart

```bash
# After clicking "Use this template" and cloning your fork
./gradlew check                  # compile + run all 9 tests
./gradlew serverPlugin           # build the plugin zip (server/build/distributions/)

# First-time only: download + unpack the TeamCity distribution
./gradlew installTeamcity

./gradlew startTeamcity          # boot the sandbox at http://localhost:8111
./gradlew deployToTeamcity       # hot-redeploy after edits (no restart)
./gradlew stopTeamcity           # tear down
```

A fresh sandbox stops once at `http://localhost:8111` for the **TeamCity Maintenance** screen — confirm the data dir,
accept the EULA, create an admin user. Plugins (yours included) load only after you click through. Subsequent starts
boot straight to the login page. Find the super-user token in `server/build/servers/TeamCity-<version>/logs/teamcity-server.log`
(grep for `Super user authentication token:`).

## Sample features

Six samples cover ~95% of real-world plugin patterns. Each is a single file with a `// SAMPLE: delete or adapt` banner.

| # | Sample | Demonstrates | How to verify |
|---|---|---|---|
| 1 | [`SampleHealthStatusReport`](server/src/main/kotlin/com/example/template/server/health) | Health report extension, `ItemCategory`, `HealthStatusItemConsumer` | Admin → Server Health Reports → "Sample plugin" row |
| 2 | [`SampleBuildServerListener`](server/src/main/kotlin/com/example/template/server/events) | Server-wide event subscription via `EventDispatcher<BuildServerListener>` | Run any build, then `grep "Sample plugin saw build finish" server/build/servers/*/logs/teamcity-server.log` |
| 3 | [`SampleRestController`](server/src/main/kotlin/com/example/template/server/rest) | Custom HTTP endpoint, `WebControllerManager` registration | `curl -u :<token> http://localhost:8111/sample-plugin/status.html` → `{"status":"ok",...}` |
| 4 | [`SampleBuildFeature`](server/src/main/kotlin/com/example/template/server/feature) | Build feature dropdown entry, default parameters | Build configuration → Build Features → Add → "Sample Build Feature" |
| 5 | [`SamplePageExtension` + JSP](server/src/main/kotlin/com/example/template/server/web) | UI fragment via `SimplePageExtension`, JSP under `buildServerResources/` | Open any finished build's Overview page → fragment at the bottom |
| 6 | [`SampleRunType`][rt] + [`SampleAgentBuildRunner`][ar] + [`SampleConstants`][sc] | Full-stack runner: server registration, agent execution, shared param names | Add the "Sample Runner" step to a build → run → see `Hello from teamcity-plugin-template` in the build log |

[rt]: server/src/main/kotlin/com/example/template/server/runner
[ar]: agent/src/main/kotlin/com/example/template/agent/runner
[sc]: common/src/main/kotlin/com/example/template/common

To remove a sample: delete its file(s), drop the corresponding `<bean>` line from `META-INF/build-*-plugin-*.xml`,
delete its test, then `./gradlew check serverPlugin`.

## Local development

**Debug from your IDE.** The sandbox publishes JVM debug ports — server `5500`, agent `5501`. In IntelliJ:
*Run → Edit Configurations → + → Remote JVM Debug → host `localhost`, port `5500`*. Same for the agent on `5501` if
you need to step through agent-side code.

**Tune memory if the sandbox feels slow.** TeamCity's defaults (1 GB server, 384 MB agent) are skinny for plugin
development. Export before `startTeamcity`:

```bash
export TEAMCITY_SERVER_MEM_OPTS='-Xmx4g -XX:ReservedCodeCacheSize=1024m'
export TEAMCITY_AGENT_MEM_OPTS='-Xmx4g'
```

**Hot-redeploy without losing the session.** With `allowRuntimeReload = true` (default in this template),
`./gradlew deployToTeamcity` swaps the plugin in-place. The server stays up, your browser session stays valid, and
the new code is picked up within ~1 second. If hot reload fails (Spring beans changed signature in a way TC can't
reconcile), restart with `stopTeamcity` + `startTeamcity`.

**Test against an externally-installed TeamCity.** Build `./gradlew serverPlugin` and upload the resulting zip via
*Administration → Plugins List → Upload plugin zip*. TC restarts automatically unless your plugin has
`allowRuntimeReload = true`.

**Multi-node setup** for testing `nodeResponsibilitiesAware = true` — see the
[multi-node guide](https://www.jetbrains.com/help/teamcity/multinode-setup.html). Duplicate the sandbox directory,
shift ports in `conf/server.xml`, point both at the same data dir + external database, and start the second instance
with `-Dteamcity.server.nodeId=…`.

## Post-fork checklist

After clicking **Use this template** (or cloning manually), customize these:

1. **`gradle.properties`** — set `pluginGroup`, `pluginName`, `pluginDisplayName`, `pluginDescription`, `pluginVendor`,
   `pluginVendorUrl`, `pluginVendorEmail`. Reset `pluginVersion` to `0.0.1`.
2. **Bean.xml filenames** — rename `server/src/main/resources/META-INF/build-server-plugin-template.xml` and
   `agent/src/main/resources/META-INF/build-agent-plugin-template.xml`, replacing the `template` suffix with your
   plugin name. The filename pattern (`build-*-plugin-*.xml`) is what TeamCity scans for.
3. **Package name** — rename `com.example.template.*` to your own package (IntelliJ's *Refactor → Rename Package*
   handles it in one step).
4. **`.teamcity.yml`** — change the `marketplace-token` reference to your credentialsJSON token id.
5. **`CODEOWNERS`** — set your team handle (or remove the file).
6. **Delete the samples you don't need** — see the table above.

`find . -type f -exec grep -l 'teamcity-plugin-template\|com\.example\.template' {} +` lists every file that
references the template's identity if you'd rather sweep with `sed`.

## Project layout

```
server/                          Server-side Kotlin code, Spring beans, web resources
agent/                           Agent-side Kotlin code, Spring beans
common/                          Code shared between server and agent (constants, DTOs)
gradle.properties                Plugin metadata + build environment
gradle/libs.versions.toml        Single source of truth for dependency versions
.teamcity.yml                    TeamCity 2026.1+ YAML pipeline (build, test, publish)
CHANGELOG.md                     Keep-a-Changelog format, consumed by gradle-changelog-plugin
.github/                         Repo hygiene (issue/PR templates, CODEOWNERS, dependabot)
```

## Plugin descriptor knobs (`gradle.properties`)

| Key | What it controls |
|---|---|
| `pluginGroup` | Maven `group` of the produced artifacts |
| `pluginName` | The `<name>` in the descriptor — also the zip filename and the Bean.xml suffix |
| `pluginDisplayName` | The `<display-name>` shown in the TeamCity admin UI |
| `pluginVersion` | Semver of the plugin; the changelog plugin reads this |
| `pluginDescription`, `pluginVendor`, `pluginVendorUrl`, `pluginVendorEmail` | Marketplace metadata |
| `pluginMinimumBuild` | The `min-build` requirement; TeamCity refuses to load on older builds |
| `teamcityVersion` | The TeamCity API version compiled against |
| `javaVersion` | JVM toolchain (default 21, matches TeamCity 2026.1+ requirements) |
| `kotlinVersion` | Kotlin tooling version (matches TeamCity's bundled stdlib) |

## Versioning & releasing

1. Describe the change under `[Unreleased]` in [`CHANGELOG.md`](CHANGELOG.md).
2. Bump `pluginVersion` in `gradle.properties` and merge.
3. Run `./gradlew patchChangelog` locally to rotate `[Unreleased]` into a versioned section, and commit.
4. Trigger the `publish` job in your TeamCity pipeline (manual run, or wire a tag-based trigger). The job runs
   `./gradlew publishPlugin` against JetBrains Marketplace using the `marketplace-token` secret.

## CI / CD — `.teamcity.yml`

| Job | Trigger | Purpose |
|---|---|---|
| `build-2024-12` | every commit | `./gradlew check serverPlugin -PteamcityVersion=2024.12` |
| `build-2025-07` | every commit | `./gradlew check serverPlugin -PteamcityVersion=2025.07` |
| `build-2025-11` | every commit | Same against TC 2025.11 (the default). Publishes the resulting zip as a build artifact. |
| `publish` | manual / tag | Depends on `build-2025-11`. Runs `./gradlew publishPlugin` to upload to Marketplace. |

**TeamCity setup**:
1. Create a project pointing at this repo. TeamCity 2026.1+ auto-discovers `.teamcity.yml`.
2. Add a credentialsJSON token at *Project Settings → Tokens* with id matching the reference in `.teamcity.yml`,
   containing your Marketplace publish token.
3. (Optional) Configure a tag-only trigger on the `publish` job so it fires on `v*` tags rather than every commit.

## Compatibility matrix

| TeamCity version | Build number | CI tested | Notes |
|---|---|---|---|
| 2024.12 | 174726+ | ✅ | Older — supported via matrix |
| 2025.07 | — | ✅ | Mid-line |
| 2025.11 | 207946+ | ✅ | Default `pluginMinimumBuild`; default `teamcityVersion` |
| 2026.1 | TBD | — | Will require Java 21 on both server and agent |

## Conventions & gotchas

The load-bearing details the official docs gloss over.

### Spring `Bean.xml` filename pattern is enforced

TeamCity scans plugin classpaths for `META-INF/build-server-plugin-*.xml` (server) and `META-INF/build-agent-plugin-*.xml`
(agent). Use those exact prefixes — the suffix can be anything as long as it's unique. Renaming the template's
`-template` suffix to your plugin name is a post-fork step.

### `default-autowire="constructor"` is the wiring convention

Every Bean.xml in TeamCity's own source uses it. Plugin beans declare TeamCity services as constructor parameters
(`SBuildServer`, `WebControllerManager`, `RunTypeRegistry`, `EventDispatcher<BuildServerListener>`, `PagePlaces`,
`PluginDescriptor`, …) and Spring autowires them in. No `@Autowired` annotation needed.

### Three classloader-related descriptor flags

Set in `server/build.gradle.kts` and propagated into `teamcity-plugin.xml`:

- **`useSeparateClassloader = true`** — gives the plugin its own classloader so its dependencies can't collide with
  TeamCity's or another plugin's. Almost always wanted; turn it off only if you're extending another plugin's classes
  directly.
- **`allowRuntimeReload = true`** — lets `./gradlew deployToTeamcity` (or the admin UI's Reload button) hot-replace
  the plugin without restarting the server.
- **`nodeResponsibilitiesAware = true`** — required for multi-node TeamCity Cloud / on-prem clusters. Default it on.

### `pluginMinimumBuild`

TeamCity uses *only* a lower bound. The TeamCity team commits to backwards compatibility for the open API, so a plugin
that loads on TC `min-build` continues working on later versions until you intentionally use a newer API. Set
`pluginMinimumBuild` to the build number of the oldest TC version you support; look up build numbers at
<https://www.jetbrains.com/teamcity/whatsnew/>.

### `compileOnly` for TeamCity APIs

TeamCity provides its own classes at runtime. Bundling `server-api` / `agent-api` into the plugin zip would cause
classloader conflicts. Keep them on `compileOnly`. Audit `implementation` deps on big plugins to make sure none shadow
a TeamCity-provided jar.

### `agent(project(":agent", configuration = "plugin"))` glues the two zips

This Gradle DSL line in `server/build.gradle.kts` is what makes the produced server zip contain the agent zip nested
inside `agent/`. Without it, the agent module builds in isolation and never reaches the user.

### `buildServerResources/` is the resources convention

Anything under `server/src/main/resources/buildServerResources/` is exposed at runtime under
`<plugin-resources-path>/...`. Use `PluginDescriptor.getPluginResourcesPath("file.jsp")` to resolve URLs — never
hard-code paths.

### JUnit 4, not 5

TeamCity's own `tests-support` library is JUnit 4-based. The template's tests use plain JUnit 4 + mockk for speed.
For deeper integration tests that need a fake server, depend on `org.jetbrains.teamcity:tests-support` — already in
the version catalog under `libs.teamcity.tests`.

### `RunType` vs `BuildFeature` vs `AgentBuildRunner`

Three commonly confused extension points:
- **`RunType`** — server registration of a build runner. Defines the type, default parameters, edit UI, validation.
- **`AgentBuildRunner` / `CommandLineBuildServiceFactory`** — agent-side execution of that runner. Use
  `CommandLineBuildServiceFactory` for runners that exec a binary; use `AgentBuildRunner` for arbitrary work.
- **`BuildFeature`** — orthogonal addition to *any* build (e.g. "send a Slack message when the build finishes").
  Doesn't define a runner; runs alongside whatever runner the build uses.

### INFO-severity health items don't show on the dashboard

The Server Health page filters at WARN minimum by default. INFO items are technically reported but invisible to most
users. The `SampleHealthStatusReport` uses WARN for that reason.

## Further reading

- [Developing TeamCity Plugins](https://plugins.jetbrains.com/docs/teamcity/developing-teamcity-plugins.html) —
  the official handbook. Extension-point reference, packaging rules, and links to bundled open-source plugins.
- [Getting Started with Plugin Development](https://plugins.jetbrains.com/docs/teamcity/getting-started-with-plugin-development.html) —
  the tutorial path. Use it to understand the moving parts; come back here when you want a project that already has
  them wired together.
- [TeamCity Open API Javadoc](https://javadoc.jetbrains.net/teamcity/openapi/current/) — searchable Javadoc for every
  public class and interface.
- [Extending TeamCity](https://www.jetbrains.com/help/teamcity/extending-teamcity.html) — high-level overview that
  links to scripted-build interaction, REST API, and report integration if a plugin isn't actually what you need.
- [JetBrains Marketplace — TeamCity plugins](https://plugins.jetbrains.com/teamcity) — published plugins; many are
  open source and worth reading for idioms beyond what this template covers.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
</content>
</invoke>