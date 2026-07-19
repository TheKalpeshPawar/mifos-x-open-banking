# Test Infrastructure — mifos-x-open-banking

> **Local, untracked doc.** This file is deliberately kept out of version control via
> `.git/info/exclude` (not `.gitignore`). It is a developer map of how testing is wired
> in this repo and where each kind of test lives. Regenerate/update it by hand.

This is a Kotlin Multiplatform + Compose Multiplatform project. Test wiring is driven almost
entirely by **convention plugins** in `build-logic/convention/`, with a few modules opting into
extra test tooling per-module. There is **no** top-level test config file — read the convention
plugins + the per-module `build.gradle.kts` to understand any module's test setup.

---

## 1. The kinds of tests and where they live

| Kind | Source set | Runs under (Gradle) | Device? | What it's for |
|---|---|---|---|---|
| Unit tests (pure) | `src/commonTest` | `:m:desktopTest`, `:m:testDemoDebugUnitTest` | no | ViewModels, repositories, mappers, calculators, parsers — the bulk of coverage |
| Compose UI, headless JVM | `src/desktopTest` | `:m:desktopTest` | no | Compose screens rendered via `runComposeUiTest` on the desktop renderer |
| Compose UI, Robolectric | `src/androidUnitTest` | `:m:testDemoDebugUnitTest` | no | The same Compose surfaces against the **real Android** Compose runtime, on the JVM |
| Compose UI, on-device instrumented | `src/androidInstrumentedTest` | `:m:connectedDemoDebugAndroidTest` | **yes** | Compose surfaces on a real device/emulator (see §7 — added for `feature/home` + `feature/login`) |
| Android manifest/intent | `cmp-android/src/androidTest` | `:cmp-android:connected…AndroidTest` | yes | App-module manifest / intent-filter assertions (not Compose) |

`commonTest` is the default home for anything that does not touch a Composable. Compose UI tests are
**split by renderer**, never placed in `commonTest`, because a `commonTest` Compose test also compiles
into `androidUnitTest`, where — without a Robolectric runner — it NPEs at runtime. This split is
documented inline in `feature/consent-callback/build.gradle.kts:42-46`.

The KMP targets that make `desktopTest` / `androidUnitTest` / `androidInstrumentedTest` available on
every library module come from `configureKotlinMultiplatform()`
(`build-logic/convention/src/main/kotlin/org/convention/KotlinMultiplatform.kt`):
`androidTarget()`, `jvm("desktop")`, `iosSimulatorArm64()`, `iosArm64()`, `js(IR)`, `wasmJs()`.

---

## 2. How it's wired — convention plugins

Every library/feature module applies a convention plugin instead of configuring test deps by hand.

- **`KMPLibraryConventionPlugin`** (`org.convention.kmp.library`) — the base for `core:*` modules.
  Applies `com.android.library` + Kotlin Multiplatform, and wires the baseline test classpath
  (`KMPLibraryConventionPlugin.kt:43-47`):
  - `commonTestImplementation(libs.kotlin.test)`
  - `commonTestImplementation(libs.kotlinx.coroutines.test)`
  It also applies detekt, spotless, **Kover** (`org.convention.kover.plugin`), koin, serialization,
  parcelize, and sets `targetSdk = 36`.
- **`CMPFeatureConventionPlugin`** (`org.convention.cmp.feature`) — the base for `feature:*` modules.
  Applies `org.convention.kmp.library` (so it inherits the baseline above) + koin + Compose. Adds
  production Compose/koin/navigation deps only.

> **Neither convention plugin wires any Compose-UI-test or Robolectric dependency.** Modules that need
> UI tests opt in **inside their own `build.gradle.kts`** (see §5). Only `kotlin.test` +
> `kotlinx.coroutines.test` are automatic.

Android/SDK settings (`KotlinAndroid.kt`): `compileSdk = 36`, `minSdk = 26`, Java 17, core-library
desugaring on.

---

## 3. Coverage — Kover

Kover (`org.jetbrains.kotlinx.kover` `0.9.1`) is applied to every module by
`KMPLibraryConventionPlugin` (`apply("org.convention.kover.plugin")`). Each leaf module self-registers
into a root aggregate (`KoverConventionPlugin.kt` → `rootProject.dependencies.add("kover", project)`).

Commands (from repo root):
```
./gradlew koverHtmlReport            # aggregated HTML across all modules
./gradlew koverXmlReport
./gradlew koverVerify                # enforces the coverage floor
./gradlew :core:data:koverHtmlReport # per-module — report at core/data/build/reports/kover/
```

**Exclusions** — configured once in `org/convention/Kover.kt:28-58`. The measured surface **excludes**:
`*.di.*` (Koin/DI modules), `*.BuildConfig`, `*ComposableSingletons*`, `*_*Factory*`, generated
`*.generated.*` / `*.ksp.*` packages, `*Preview*`, `*Test*`, and anything annotated
`@androidx.compose.runtime.Composable`.

Consequences worth remembering:
- **DI modules need no tests** to hit 100% (e.g. `core/data/.../banking/di/BankingModule.kt` is excluded).
- **Composables are not line-covered by Kover** — they are validated by the UI tests in §5 instead.
- Current global floor: `minBound(40)` (`Kover.kt:55`) — a phase-1 floor while coverage grows.

---

## 4. Test conventions (mirror these in any new test)

- **Frameworks:** `kotlin.test` (`@Test`, `assertEquals/assertTrue/assertIs/assertNull/...`) +
  `kotlinx.coroutines.test` (`runTest`, `UnconfinedTestDispatcher`, `Dispatchers.setMain`,
  `advanceUntilIdle`). **No JUnit assertions, no Truth, no Turbine** in unit tests.
  Turbine (`libs.turbine`, `1.2.1`) is present in the catalog but **not on any module's test classpath** —
  Flow tests use `flow.first { … }` / `.take(n).toList()` under `runTest`.
- **Naming:** class `<Type>Test`; methods either camelCase or backtick sentences (both used).
  Package **mirrors** the source package.
- **License header:** every file starts with the MPL-2.0 block (copy from any existing test).
- **Line length ≤ 120** (spotless/detekt/ktlint all enforced). Long JSON literals are split across
  concatenated string lines.
- **Fakes:** hand-written in-memory doubles in their own top-level `Fake*.kt` files, implementing the
  real interface and backing a `MutableStateFlow` / counters (e.g.
  `feature/home/src/commonTest/.../FakeHomeRepositories.kt`,
  `feature/login/src/commonTest/.../FakeLoginRepository.kt`,
  `core/data/src/commonTest/.../callback/FakePendingAuthStore.kt`).
  Note: `core-base/store` ships reusable fakes (`FakeNetworkMonitor`, `FakeFetchedAtRepository`) but they
  live in *that* module's `commonTest` and some are `internal`, so they are **not reachable** from other
  modules — copy the shape locally.
- **Faking the network:** build a **real** client/service over a Ktor `MockEngine` rather than mocking the
  service interface — the established pattern in `core/data/src/commonTest/.../login/LoginRepositoryImplTest.kt`
  and `.../callback/ConsentCallbackRepositoryImplTest.kt`
  (`HttpClient(MockEngine { … }) { install(ContentNegotiation) { json(...) } }`).

---

## 5. Compose UI testing — the two-runner pattern

**`feature/consent-callback` is the reference implementation.** It tests the same Compose surfaces two
ways, both **without a device**:

1. **Desktop headless** — `src/desktopTest/.../ConsentCallbackStepsUiTest.kt`:
   `@OptIn(ExperimentalTestApi::class)` + `runComposeUiTest { setContent { … }; onNodeWithTag(TAG).assertIsDisplayed() }`.
2. **Robolectric (real Android runtime, JVM)** — `src/androidUnitTest/.../ConsentCallbackStepsRobolectricTest.kt`:
   ```kotlin
   @RunWith(RobolectricTestRunner::class)
   @Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])   // ROBOLECTRIC_SDK = 34
   class … {
       @get:Rule val composeRule = createComposeRule()
       @Test fun render() { composeRule.setContent { SomeStep() }
           composeRule.onNodeWithTag(TestTags.X).assertExists() }
   }
   ```
   Click assertions capture the dispatched action into a `mutableListOf` and `assertEquals`.

> There is **no `robolectric.properties`** file — the SDK is pinned inline via `@Config(sdk = [34])`.

**Central tag object** — `src/commonMain/.../ConsentCallbackTestTags.kt`
(`object ConsentCallbackTestTags { const val LOADING_STEP = "callback_loading" … }`). Screens expose
`Modifier.testTag(...)`; tests assert on the object's constants. Prefer a `*TestTags` object per feature
over inline tag strings.

**Testability requirement:** a state composable must be at least `internal` (not `private`) for a
same-module test to invoke it directly with fake data. consent-callback's `steps/*` are public;
`feature/home`'s state composables are `internal`; `feature/login`'s state composables were made
`internal` for the same reason.

### Per-module UI-test opt-in (what a feature module's `build.gradle.kts` adds)

Mirrors `feature/consent-callback/build.gradle.kts:38-70`:
```kotlin
kotlin { sourceSets {
    androidUnitTest.dependencies {          // Robolectric path
        implementation(libs.robolectric)
        implementation(libs.bundles.androidx.compose.ui.test)
    }
    desktopTest.dependencies {              // headless-desktop path (optional)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(compose.uiTest)
        implementation(compose.desktop.uiTestJUnit4)
        implementation(compose.desktop.currentOs)
    }
} }
android { testOptions { unitTests {
    isIncludeAndroidResources = true        // required for Robolectric + Compose resources
    isReturnDefaultValues = true
} } }
```

---

## 6. Dependency catalog map (`gradle/libs.versions.toml`)

| Alias | Artifact | Notes |
|---|---|---|
| `libs.robolectric` | `org.robolectric:robolectric` (4.16) | Robolectric runner |
| `libs.bundles.androidx.compose.ui.test` | `ui-test-junit4` + `ui-test-manifest` | Compose test bundle (Robolectric + instrumented) |
| `libs.androidx.compose.ui.test` | `androidx.compose.ui:ui-test-junit4` | the junit4 half of the bundle |
| `libs.androidx.compose.ui.test.manifest` | `androidx.compose.ui:ui-test-manifest` | manifest half; instrumented needs it as a debug dep |
| `libs.androidx.test.ext.junit` | `androidx.test.ext:junit` (1.3.0) | brings `AndroidJUnit4` runner for instrumented tests |
| `libs.androidx.test.runner` | `androidx.test:runner` (1.7.0) | supplies `AndroidJUnitRunner` (the `testInstrumentationRunner`) |
| `libs.androidx.test.espresso.core` | `androidx.test.espresso:espresso-core` (3.7.0) | forced upgrade — see §7 (Android 16 fix) |
| `libs.turbine` | `app.cash.turbine:turbine` (1.2.1) | **present but unused** — house style avoids it |
| `compose.uiTest` / `compose.desktop.uiTestJUnit4` / `compose.desktop.currentOs` | JetBrains Compose plugin | desktop `runComposeUiTest` artifacts (not from the catalog) |

Added for the on-device instrumented path (§7): `androidx-test-runner` (1.7.0) and
`androidx-test-espresso-core` (3.7.0). `androidx-test-ext-junit` (1.3.0, already present) supplies
`AndroidJUnit4`.

---

## 7. On-device instrumented Compose tests

`feature/home` and `feature/login` add a true on-device instrumented path
(`src/androidInstrumentedTest` → `:m:connectedDemoDebugAndroidTest`), which requires a running
device/emulator. This is **new** infra — before it, the repo tested Compose only via Robolectric +
desktop (JVM, device-free), and only `cmp-android/src/androidTest` used a real device (for a
manifest/intent test, not Compose). It is **live and green** — `HomeScreenInstrumentedTest` (5) +
`LoginScreenInstrumentedTest` (5) pass on-device, mirroring the Robolectric suites verbatim.

What the instrumented path needs beyond the Robolectric opt-in (wired per-module in `build.gradle.kts`):
- `android { defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }`.
- `androidInstrumentedTest.dependencies { … }` — `libs.kotlin.test` (the instrumented source set does
  NOT inherit `commonTest`'s `kotlin.test`, so it is added explicitly), `libs.bundles.androidx.compose.ui.test`
  (`ui-test-junit4` + `ui-test-manifest`), `libs.androidx.test.ext.junit` (`AndroidJUnit4`),
  `libs.androidx.test.runner`, and `libs.androidx.test.espresso.core` (3.7.0).
- Test classes mirror the Robolectric bodies but run under `@RunWith(AndroidJUnit4::class)` with
  `createComposeRule()` — identical `onNodeWithTag(...)` assertions.

> **Android 16 (API 36) note:** Compose's `createComposeRule` synchronises through Espresso's
> `onIdle`, which reflectively calls `android.hardware.input.InputManager.getInstance()` — a method
> **removed in Android 16**. The transitive `espresso-core` pulled by `ui-test-junit4` is `3.5.0`,
> which crashes every instrumented test on an Android 16 device with `NoSuchMethodException`. The fix
> is the explicit `espresso-core 3.7.0` dependency above, which force-upgrades off `3.5.0` and
> restores Android 16 support.

> **Note on `disableUnnecessaryAndroidTests`:**
> `build-logic/.../org/convention/AndroidInstrumentedTests.kt#disableUnnecessaryAndroidTests()` would
> disable the `androidTest` variant unless a `src/androidTest` folder exists — but it is **dead code**
> (no call site in `build-logic`), so the `androidInstrumentedTest` variant is not suppressed and
> `connectedDemoDebugAndroidTest` runs normally.

---

## 8. Per-module test inventory (snapshot)

| Module | commonTest | Compose UI tests | Notes |
|---|---|---|---|
| `core/data` | mappers, `SpendingCalculator`, repositories (login, callback, banking) via MockEngine + fakes | — | primary unit-test module |
| `core/common` | money/date/account formatters | — | pure functions |
| `core/database` | DAO tests (desktop) | — | Room `androidx.room3`, `exportSchema = true` |
| `core/network` | `HsbcSandboxHttpClientTest` (token round-trip) | — | MockEngine |
| `feature/home` | `HomeViewModelTest` + `FakeHomeRepositories` | Robolectric + instrumented (§7) | state composables are `internal` |
| `feature/login` | `LoginViewModelTest` + `FakeLoginRepository` | Robolectric + instrumented (§7) | state composables made `internal` |
| `feature/consent-callback` | `ConsentCallbackViewModelTest` + fake | **desktop + Robolectric** (reference) | copy this module's setup |
| `cmp-navigation` | nav/tab tests | — | |
| `cmp-android` | — | — | `src/androidTest` manifest/intent test |

---

## 9. Verify commands (cheat-sheet)

```bash
cd /home/kalpesh/OpenSource/Mifos/mifos-x-open-banking

# Format + static analysis (always keep green; never --no-verify)
./gradlew spotlessApply spotlessCheck detekt

# Coverage
./gradlew :core:data:koverHtmlReport         # per-module → core/data/build/reports/kover/
./gradlew koverHtmlReport                     # aggregated

# Unit tests (JVM, no device)
./gradlew :core:data:desktopTest :core:data:testDemoDebugUnitTest
./gradlew :feature:home:desktopTest :feature:home:testDemoDebugUnitTest

# Compose UI — Robolectric (JVM, no device)
./gradlew :feature:home:testDemoDebugUnitTest :feature:login:testDemoDebugUnitTest

# Compose UI — on-device instrumented (needs a connected device/emulator)
./gradlew :feature:home:connectedDemoDebugAndroidTest :feature:login:connectedDemoDebugAndroidTest

# App assembly
./gradlew :cmp-android:assembleDemoDebug
```
