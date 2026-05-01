# TeamCity Plugin Template

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue)][LICENSE]
[![TeamCity 2025.11](https://img.shields.io/badge/TeamCity-2025.11-blue)][tc:home]
[![Java 17](https://img.shields.io/badge/Java-17-blue)][adoptium]
[![Kotlin 2.3.21](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)][kotlin]

<!-- Optional hero image: drop a PNG/SVG at .github/readme/banner.png and uncomment.
![TeamCity Plugin Template](.github/readme/banner.png) -->

A working starter for TeamCity plugins. Click <kbd>Use this template</kbd>, then `./scripts/sandbox.sh up` to land in
a TC 2025.11 sandbox with the bundled samples loaded.

![Use this template][readme:use-this-template]

> [!TIP]
> Watch this repo to be notified when the toolchain (TC SDK, Kotlin, gradle-teamcity-plugin) is bumped.

### Table of contents

- [What's in the box](#whats-in-the-box)
- [Quickstart](#quickstart)
- [Sample features](#sample-features)
- [How rodm tasks chain](#how-rodm-tasks-chain)
- [Local development](#local-development)
- [Post-fork checklist](#post-fork-checklist)
- [Project layout](#project-layout)
- [Plugin descriptor knobs](#plugin-descriptor-knobs-gradleproperties)
- [Versioning & releasing](#versioning--releasing)
- [CI / CD — `.teamcity.yml`](#ci--cd--teamcityyml)
- [Compatibility matrix](#compatibility-matrix)
- [Conventions & gotchas](#conventions--gotchas)
- [Further reading](#further-reading)

## What's in the box

- 3-module Gradle project (`server` / `agent` / `common`) on Java 17 + Kotlin 2.3.21, compiled against the TeamCity
  2025.11 SDK. Java target stays at 17 so the plugin loads on the full TC 2024.12 → 2025.x range; bump to 21 when
  raising `pluginMinimumBuild` to TC 2026.1+.
- Six extension-point samples: health report, build-server listener, REST controller, build feature, page extension
  (with JSP), and a full-stack build runner — server registration with edit JSP + `PropertiesProcessor`, agent
  execution with a real subprocess and `BuildProblemData` on failure.
- `scripts/sandbox.sh` — drives the first-boot wizard via REST, mints an admin access token, provisions a sample
  project that exercises the bundled runner. Idempotent. Pairs with the [`teamcity` CLI][tc:cli].
- `.teamcity.yml` matrix CI (TC 2024.12 / 2025.07 / 2025.11) and a manual Marketplace publish job.
- 14 unit tests (JUnit 4 + mockk).
- Repo hygiene: `.editorconfig`, dependabot, issue/PR templates, `gradle-changelog-plugin`, Apache-2.0 license.

## Quickstart

```bash
./gradlew check                  # compile + run all 14 tests
./gradlew serverPlugin           # build the plugin zip (server/build/distributions/)
./scripts/sandbox.sh up          # install + start TC, drive its first-boot wizard,
                                 #   mint an admin token, provision a SandboxDemo
                                 #   project that exercises the sample runner
./scripts/sandbox.sh down        # stop server + agent
./scripts/sandbox.sh tc run start SandboxDemo_Run --watch   # trigger via teamcity CLI
```

`sandbox.sh up` is idempotent and replaces the click-through-the-wizard ritual: the maintenance flow
(`goNewInstallation` → `goNewDatabase` → `acceptLicenseAgreement`) is driven via TeamCity's own REST endpoints, then an
admin user (`admin` / `admin`) and a personal access token are minted automatically. The token lands in
`.sandbox/admin.env` (gitignored) and powers `sandbox.sh tc <args>` so you can drive runs from the terminal.

| Command | What it does |
|---|---|
| `sandbox.sh up` | Build + install + start + bootstrap (idempotent) |
| `sandbox.sh down` | Stop server + agent |
| `sandbox.sh reset` | `down` + wipe `server/build/data` (TC binary preserved) |
| `sandbox.sh status` | Where the sandbox is — installed? running? authed? |
| `sandbox.sh log [server\|agent]` | Tail the server (default) or agent log |
| `sandbox.sh tc <args>` | Invoke `teamcity` CLI authenticated against the sandbox |

> [!NOTE]
> If you'd rather skip the helper, the underlying gradle tasks still work — see [How rodm tasks chain](#how-rodm-tasks-chain).

## Sample features

Each sample is one Kotlin file (or a small set), tagged with a `// SAMPLE: delete or adapt` banner.

| # | Sample | Demonstrates | How to verify |
|---|---|---|---|
| 1 | [`SampleHealthStatusReport`](server/src/main/kotlin/com/example/template/server/health) | Health report extension, `ItemCategory`, `HealthStatusItemConsumer` | Admin → Server Health Reports → "Sample plugin" row |
| 2 | [`SampleBuildServerListener`](server/src/main/kotlin/com/example/template/server/events) | Server-wide event subscription via `EventDispatcher<BuildServerListener>` | Run any build, then `grep "Sample plugin saw build finish" server/build/servers/*/logs/teamcity-server.log` |
| 3 | [`SampleRestController`](server/src/main/kotlin/com/example/template/server/rest) | Custom HTTP endpoint, `WebControllerManager` registration | `curl -u :<token> http://localhost:8111/sample-plugin/status.html` → `{"status":"ok",...}` |
| 4 | [`SampleBuildFeature`](server/src/main/kotlin/com/example/template/server/feature) | Build feature dropdown entry, default parameters | Build configuration → Build Features → Add → "Sample Build Feature" |
| 5 | [`SamplePageExtension` + JSP](server/src/main/kotlin/com/example/template/server/web) | UI fragment via `SimplePageExtension`, JSP under `buildServerResources/` | Open any finished build's Overview page → fragment at the bottom |
| 6 | [`SampleRunType`][rt] + [`SampleAgentBuildRunner`][ar] + [`SampleConstants`][sc] | Full-stack runner: server registration with edit-form JSP + `PropertiesProcessor` validation, agent execution that spawns a real subprocess and surfaces `BuildProblemData` on failure | `./scripts/sandbox.sh up && ./scripts/sandbox.sh tc run start SandboxDemo_Run --watch` |

[rt]: server/src/main/kotlin/com/example/template/server/runner
[ar]: agent/src/main/kotlin/com/example/template/agent/runner
[sc]: common/src/main/kotlin/com/example/template/common

To remove a sample: delete its file(s), drop the corresponding `<bean>` line from `META-INF/build-*-plugin-*.xml`,
delete its test, then `./gradlew check serverPlugin`.

## How rodm tasks chain

`./gradlew startTeamcity` is a composite that depends on three other tasks. Knowing the dependency graph lets you skip
or replay individual phases when iterating:

```
startTeamcity ─┬─► startTeamcityServer ◄─── installTeamcity      (extract TC into server/build/servers/)
               └─► startTeamcityAgent  ◄─── deployToTeamcity ◄─── serverPlugin
                                                                  (copy plugin zip into server/build/data/teamcity/plugins/)
```

| Want to… | Run |
|---|---|
| Re-deploy without restart (`allowRuntimeReload = true`) | `./gradlew deployToTeamcity` |
| Boot the server without re-deploying | `./gradlew startTeamcityServer` |
| Restart only the agent | `./gradlew stopTeamcityAgent startTeamcityAgent` |
| Throw away config + DB but keep the TC binary | `./scripts/sandbox.sh reset` |

> [!NOTE]
> The first `deployToTeamcity` against an unbooted server logs `Maintenance token file does not exist. Cannot reload
> plugin.` — this is expected because hot-reload requires a running server. The plugin zip is copied to the data dir
> anyway and is picked up on the next start. Subsequent deploys against a running server hot-reload silently.

## Local development

The sandbox publishes JVM debug ports: server on `5500`, agent on `5501`. Attach via
*Run → Edit Configurations → + → Remote JVM Debug* in IntelliJ.

TeamCity's default heaps (1 GB server, 384 MB agent) are tight for plugin work. Bump them before `sandbox.sh up`:

```bash
export TEAMCITY_SERVER_MEM_OPTS='-Xmx4g -XX:ReservedCodeCacheSize=1024m'
export TEAMCITY_AGENT_MEM_OPTS='-Xmx4g'
```

Hot reload is on by default (`allowRuntimeReload = true`); `./gradlew deployToTeamcity` swaps the plugin in place
and TC picks it up within a second. If a Spring bean signature changes in a way TC can't reconcile, fall back to
`sandbox.sh down && sandbox.sh up`.

To test against an externally-installed TeamCity, build the zip with `./gradlew serverPlugin` and upload via
*Administration → Plugins List → Upload plugin zip*.

For multi-node testing of `nodeResponsibilitiesAware = true`, follow the [multi-node guide][tc:multinode]: duplicate
the sandbox, shift ports in `conf/server.xml`, point both at the same data dir + external database, start the second
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
scripts/sandbox.sh               Local-sandbox lifecycle (up/down/reset/log/status/tc)
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
| `javaVersion` | JVM toolchain (default 17 for the broadest TC 2024.12 → 2025.x compatibility; bump to 21 when raising `pluginMinimumBuild` to TC 2026.1+) |
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

| TeamCity version | Build number | JBR | Plugin compat |
|---|---|---|---|
| 2024.12 | 174726+ | 17 | ✅ default `pluginMinimumBuild` |
| 2025.07 | — | 17 | ✅ |
| 2025.11 | 207946+ | 17 | ✅ default `teamcityVersion` (compiled-against) |
| 2026.1+ | ≈205000+ | 21 | Requires `javaVersion=21` + `pluginMinimumBuild` raised |

## Conventions & gotchas

- **Bean.xml filename pattern.** TeamCity scans `META-INF/build-server-plugin-*.xml` (server) and
  `META-INF/build-agent-plugin-*.xml` (agent). The suffix is arbitrary; renaming `-template` to your plugin name is
  in the post-fork checklist.
- **Constructor autowire.** Every `Bean.xml` here uses `default-autowire="constructor"` — declare TeamCity services
  (`SBuildServer`, `WebControllerManager`, `RunTypeRegistry`, `EventDispatcher<BuildServerListener>`, `PagePlaces`,
  `PluginDescriptor`, …) as constructor parameters and Spring wires them in. No `@Autowired` needed.
- **Three classloader flags** (`server/build.gradle.kts` → `teamcity-plugin.xml`):
  - `useSeparateClassloader = true` — isolates the plugin's deps. Disable only if you explicitly extend another
    plugin's classes.
  - `allowRuntimeReload = true` — `./gradlew deployToTeamcity` and the admin UI's Reload button hot-replace the
    plugin without restarting the server.
  - `nodeResponsibilitiesAware = true` — required for multi-node clusters; harmless on single-node.
- **`pluginMinimumBuild` is a lower bound only.** The open API stays backwards-compatible, so a plugin that loads on
  `min-build` keeps working on later TC versions until you opt into a newer API. Look up build numbers at
  [What's New][tc:whatsnew].
- **`compileOnly` the TeamCity SDK.** Bundling `server-api` / `agent-api` causes classloader collisions. The version
  catalogue declares them `compileOnly` already; audit `implementation` deps on bigger plugins so none shadow a TC jar.
- **`agent(project(":agent", configuration = "plugin"))`** in `server/build.gradle.kts` is what nests the agent zip
  inside the server zip. Without it the agent module builds in isolation and never reaches the user.
- **`buildServerResources/`.** Anything under `server/src/main/resources/buildServerResources/` is served at
  `<plugin-resources-path>/…`. Resolve URLs via `PluginDescriptor.getPluginResourcesPath("file.jsp")` — never
  hard-code.
- **JUnit 4 + mockk.** TC's own `tests-support` is JUnit 4. For integration tests against a fake server, pull in
  `libs.teamcity.tests`.
- **`RunType` vs `AgentBuildRunner` vs `BuildFeature`.** `RunType` is server-side runner registration (id, defaults,
  edit UI, validation). `AgentBuildRunner` (or `CommandLineBuildServiceFactory` for binary-exec runners) is the
  agent-side execution. `BuildFeature` is orthogonal to runners — it attaches to *any* build (e.g. "post a Slack
  message when it finishes").
- **Health-report severity.** The Server Health page filters at WARN by default; INFO items are reported but
  invisible to most users. `SampleHealthStatusReport` uses WARN.

## Further reading

- [Developing TeamCity Plugins][tc:dev-guide] — the official handbook. Extension-point reference, packaging rules,
  and links to bundled open-source plugins.
- [TeamCity Open API Javadoc][tc:javadoc] — searchable Javadoc for every public class and interface.
- [Extending TeamCity][tc:extending] — high-level overview linking to scripted-build interaction, REST API, and
  report integration if a plugin isn't actually what you need.
- [JetBrains Marketplace — TeamCity plugins][tc:marketplace] — published plugins; many are open source and worth
  reading for idioms beyond what this template covers.
- [`gradle-teamcity-plugin`][rodm:repo] — the rodm-maintained Gradle plugin that this template builds on. The
  `teamcity { server { ... } }` and environment DSL come from there.
- [`teamcity` CLI][tc:cli] — JetBrains' command-line client; `scripts/sandbox.sh tc <args>` is a thin wrapper.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

<!-- External links collected here so the body stays scannable. -->

[readme:use-this-template]: .github/readme/use-this-template.png

[LICENSE]: ./LICENSE
[adoptium]: https://adoptium.net/
[kotlin]: https://kotlinlang.org/
[tc:home]: https://www.jetbrains.com/teamcity/
[tc:dev-guide]: https://plugins.jetbrains.com/docs/teamcity/developing-teamcity-plugins.html
[tc:javadoc]: https://javadoc.jetbrains.net/teamcity/openapi/current/
[tc:extending]: https://www.jetbrains.com/help/teamcity/extending-teamcity.html
[tc:marketplace]: https://plugins.jetbrains.com/teamcity
[tc:cli]: https://jb.gg/tc/docs
[tc:multinode]: https://www.jetbrains.com/help/teamcity/multinode-setup.html
[tc:whatsnew]: https://www.jetbrains.com/teamcity/whatsnew/
[rodm:repo]: https://github.com/rodm/gradle-teamcity-plugin
</content>
</invoke>