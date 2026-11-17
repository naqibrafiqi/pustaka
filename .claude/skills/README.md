# Project skills

Claude Code loads these skills when it works on Pustaka. They come from two repositories, and the set matches the Kuari project's, adjusted for Pustaka's libraries. Don't edit them by hand except as noted below. To update them, copy the folders again from a newer commit and update the hash here.

## Official Android skills (Google)

Unmodified copies from [android/skills](https://github.com/android/skills), Apache 2.0 (see each folder's `LICENSE.txt`, a copy of the repository's licence). Copied from commit `5815d27fda564aa9f07ebdcb5e57ed3e8d904e0d` (2026-11-09). You can also update these with the Android CLI: `android skills update --all`.

| Skill | Upstream path |
| --- | --- |
| `android-cli` | `devtools/android-cli` |
| `testing-setup` | `testing/testing-setup` |
| `jetpack-compose-performance` | `jetpack-compose/jetpack-compose-performance` |
| `edge-to-edge` | `system/edge-to-edge` |
| `android-intent-security` | `security/android-intent-security` |
| `android-permissions-security` | `security/android-permissions-security` |
| `anr-audit-and-fix` | `performance/anr-audit-and-fix` |
| `r8-analyzer` | `performance/r8-analyzer` |
| `navigation-3` | `navigation/navigation-3` |
| `adaptive` | `jetpack-compose/adaptive` |

`navigation-3` and `adaptive` aren't in Kuari. Pustaka needs them because it uses Navigation 3 and because of the tablet layout work in issue #39.

The same skills are also in Google's `android-skills` plugin, which is enabled at user scope. These copies pin a known version for the repository.

## Community Android skills (Ricardo Costeira)

From [rcosteira79/android-skills](https://github.com/rcosteira79/android-skills), MIT (see each folder's `LICENSE`, a copy of the repository's licence). Copied from commit `fa39ae26f224fac45b32280f66ac839ae57b4c64` (2026-11-10), path `plugins/android-skills/skills/<name>`. These are not official Google skills, and none of them duplicates one above.

`android-dev`, `compose`, `kotlin-coroutines`, `kotlin-flows`, `android-data-layer`, `datastore`, `android-testing`, `android-debugging`, `gradle-build-performance`, `android-ux`, `coil-compose`

Local changes (redo them after re-copying):

1. Cross-references were written for the upstream plugin as `android-skills:<name>`. They were rewritten to bare `<name>`, because project skills have no plugin prefix and `android-skills:` is the namespace of Google's plugin.
2. In `android-dev/SKILL.md`, the routing table only lists the skills installed here: the `android-retrofit` row was replaced with rows for `coil-compose`, `navigation-3` and `adaptive`, and the line requiring the `android-skills:` prefix was replaced. The description names `coil-compose` and `navigation-3` instead of `kmp-ktor` and `android-retrofit`.
3. In `android-data-layer/SKILL.md`, the "Related" line no longer points to `android-retrofit`.

Left out because Pustaka doesn't use them: `android-retrofit` (Pustaka has no network layer), `koin`, `kmp-ktor`, `kmp-boundaries`, `modularization`, `android-gradle-logic`, `paging`, `pdf-annotations`, `rxjava-migration`, `android-source-search`. A few skills still mention some of these by name, as they do in Kuari.
