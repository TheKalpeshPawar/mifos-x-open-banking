# Feature Infrastructure — mifos-x-open-banking

> **Local, untracked doc.** Kept out of version control via `.git/info/exclude` (not `.gitignore`).
> A developer map of how a data-backed feature is built end-to-end in this repo — the layers, the
> Store5 offline-first flow, the screen-state machinery, DI, navigation, and the module anatomy.
> `feature/home` is the canonical worked example; `feature/accounts` is the second.

This is a Kotlin Multiplatform + Compose Multiplatform app that consumes the HSBC Open Banking
(OBIE AIS) sandbox. A feature is a vertical slice: **network → data (Store5) → ViewModel → Compose
screen**, wired through Koin and a per-feature nav graph. The golden rule is **reuse before writing**
— the `core-base/*` template modules and `core/*` app modules already provide the store engine,
screen-state types, base ViewModel, scaffold, and formatters. Read them before adding anything.

---

## 1. The layers (bottom-up)

| Layer | Module(s) | Responsibility |
|---|---|---|
| Network | `core/network` | Plain-ktor `Aisp`/`OAuth` services returning `NetworkResult<T, NetworkError>`; OBIE DTOs under `model/**` |
| Store engine | `core-base/store` | `StoreFactory`, `asScreenStream`, `DecisionEngine`, `DefaultValidator`, `FetchedAtRepository`, paging kit — the offline-first machinery (template-derived) |
| Screen state | `core-base/common` | `ScreenState<T>` sealed type + combinators (`mapContent`, `combineContent`, `emptyIfContent`, `combineScreenStates`) |
| UI base | `core-base/ui` | `ScreenContent` state-dispatch composable, `BaseViewModel<S,E,A>` |
| Store wiring | `core/store` | `AppStoreRegistry` qualifiers, `StoreCacheManager` (logout clear), `appStoreModule` |
| Persistence | `core/database` | Room (`androidx.room3`) entities + DAOs backing Store5 SourceOfTruth |
| Data | `core/data` | `BankingStores` (Store5 stores), repositories exposing `Flow<ScreenState<T>>`, DTO↔domain↔entity mappers, DI (`BankingModule`) |
| Domain | `core/model` | Plain data classes (`BankAccount`, `AccountBalance`, `TransactionItem`, …) |
| Formatters | `core/common` | `FormatMoney`, `FormatDate`, `FormatAccount`, `FormatNumber` |
| Design system | `core/ui` (`Mifos*`), `core-base/designsystem` (`Kpt*`) | Shared components + `KptTheme`/`KptScaffold` |
| Feature | `feature/*` | Compose screen + state files + components + ViewModel + DI + nav destination + strings |
| Navigation | `cmp-navigation` | Bottom-nav tabs, `NavHost`, per-feature graph registration, placeholder routes |

> **`core-base/**` and `core/{core,core-base}` are template-derived** (from `mifos-x/kmp-project-template`).
> Read/reuse them; treat them as off-limits for new *feature* code. New shared code goes in `core/ui`
> (`Mifos*`), new feature code in `feature/<name>`.

---

## 2. The Store5 offline-first data flow

A feature's data is served by a **Store5 `Store<Key, Value>`** built in `core/data/.../banking/store/BankingStores.kt`:

- **`createStore(fetcher, sourceOfTruth, validator)`** — network + Room cache. The fetcher calls
  `Aisp`, maps the DTO to domain, and calls `validator.markFresh()` on success (mandatory, or the TTL
  never starts). The SourceOfTruth reader **returns `null` for an empty table** so a cold start reads
  as `Loading`, not a premature `Empty` (`BankingStores.kt:68-72`). Example: `accountsStore` (key
  `"self"`, 5-min TTL), `transactionsStore` (keyed by accountId).
- **`createMemoryStore(fetcher)`** — Fetcher-only, in-memory, no Room, no validator. Example:
  `balancesStore` (`Store<String, AccountBalance>`, keyed by accountId — `BankingStores.kt:109-117`).
- **`createMutableStore(...)`** — read/write + offline sync (not used by the banking read screens yet).

A **repository** turns a store into an offline-first `Flow<ScreenState<T>>` via
`store.asScreenStream(...)` (`core-base/store/.../screen/ScreenDataStream.kt`):

```kotlin
internal class AccountsRepositoryImpl(
    private val store: Store<String, List<BankAccount>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AccountsRepository {
    private var stream: ScreenDataStream<List<BankAccount>>? = null
    private fun stream(scope) = stream ?: store.asScreenStream(
        key = BankingStores.ACCOUNTS_KEY, networkMonitor, fetchedAtRepository,
        cacheKey = CACHE_KEY, scope,
    ).also { stream = it }
    override fun accountsState(scope) = stream(scope).state   // Flow<ScreenState<List<BankAccount>>>
    override fun refresh() { stream?.refresh() }
    private companion object { const val CACHE_KEY = "home:accounts" }
}
```

`asScreenStream` internals you get for free: auto-refresh on network reconnect (debounced 300ms),
last-content preservation during refresh (`DataFreshness.UPDATING`), captive-portal detection, and
`combine(storeFlow, networkStatus) { DecisionEngine.decide(...) }` mapping every Store5 + connectivity
combination to a `ScreenState`. There are two overloads: single `key`, and `keyFlow: Flow<Key>` with
`cacheKeyFor` (re-streams when the key changes — used by `BalancesRepositoryImpl` for the selected
account).

**Conventions:** repository **interface and impl live in separate files** (interface in the feature
package, impl in `impl/`, `internal`); each impl declares a `CACHE_KEY` const in a
`private companion object`; the cacheKey convention is `"<feature>:<store>:<key>"`.

### One-shot per-key read (fan-out)
`Store.stream()` returns an **infinite** Flow. To fetch a single value for a key on demand (e.g. a
per-account balance in a list fan-out), use the house idiom copied from the paging kit
(`core-base/store/.../paging/StorePagingSource.kt:83-123`):
```kotlin
suspend fun <K, V> Store<K, V>.getOnce(key: K, refresh: Boolean): V =
    stream(if (refresh) StoreReadRequest.fresh(key) else StoreReadRequest.cached(key, refresh = false))
        .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData || it is StoreReadResponse.Initial }
        .first()
        .requireData()   // StoreResponseMapper.kt:69 — .value on Data, throws on Error
```
Fan-out: `coroutineScope { items.map { async { store.getOnce(it.id, refresh) } }.awaitAll() }`.

---

## 3. `ScreenState<T>` + combinators (`core-base/common/.../screen/`)

`sealed interface ScreenState<out T>` (`ScreenState.kt`):
- `Loading` · `Empty` · `NoNetwork(isCaptivePortal)` · `Unauthenticated` · `Error(error, isNetworkError)`
- `Content<T>(data, freshness: DataFreshness, fetchedAt: Instant?)` — `DataFreshness ∈ {FRESH, STALE, UPDATING}`.

Combinators in `ScreenStateExtensions.kt` (compose these instead of hand-rolling state logic):
- `.mapContent { data, freshness -> R }` — transform only `Content.data`, pass other states through.
- `.combineContent(other: Flow<S>) { data, extra, freshness -> R }` — combine a content stream with a
  local flow. **This is the filter primitive** — combine the accounts `Content` with a
  `MutableStateFlow<Filter>`.
- `.emptyIfContent { predicate }` — `Content`→`Empty` when a business predicate is empty (e.g. a filter
  yields nothing).
- `.dataOrNull` / `.hasContent` / `.mapError { }`.
- `combineScreenStates(a, b, …, transform)` (2–5 flows) — priority
  `NoNetwork > Loading > Unauthenticated > Error > Empty > Content`, worst freshness wins.
- `T.asLocalScreenState()` / `Flow<T>.asLocalScreenStream()` — for local/settings screens with no network.

---

## 4. ViewModel — `BaseViewModel<State, Event, Action>` (`core-base/ui/.../viewmodel/BaseViewModel.kt`)

```kotlin
class AccountsViewModel(...) : BaseViewModel<AccountsState, AccountsEvent, AccountsAction>(
    initialState = AccountsState(),
) {
    override fun handleAction(action: AccountsAction) { when (action) { ... } }   // synchronous dispatch
}
```
API a subclass uses: `stateFlow: StateFlow<S>` (screen collects this — **note the property is
`stateFlow`, not `uiState`**), `state` (snapshot), `updateState { copy(...) }`, `trySendAction(a)` /
`sendAction(a)`, `eventFlow`/`sendEvent(e)` (one-shot events), and the abstract `handleAction`.

**Formatting is presentation logic — do it in the ViewModel.** Map domain models into a display-ready
UI model (pre-formatted strings, resolved colors/icons/labels); the composables render finished
strings only. See `HomeViewModel.buildHomeData(...)`.

---

## 5. The screen — `ScreenContent` (`core-base/ui/.../screen/ScreenContent.kt`)

The screen entry collects `stateFlow` and dispatches through `ScreenContent`, supplying per-state slots:
```kotlin
@Composable
internal fun AccountsScreen(onNavigate…: () -> Unit, viewModel: AccountsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    KptScaffold(modifier) {                                    // whole screen in a Scaffold, not bare Surface
        ScreenContent(
            state = state.uiState,
            onRetry = { viewModel.trySendAction(AccountsAction.RetryLoad) },
            loading = { AccountsSkeleton() },
            empty = { AccountsEmpty(onManageConsents = …) },
            error = { AccountsError(onRetry = …) },
        ) { data, _ -> AccountsContent(data = data, onSelectAccount = …, …) }
    }
}
```
`ScreenContent` params: `state`, `onRetry`, `showFreshnessIndicator`, and `loading`/`empty`/`noNetwork`/
`error`/`content` slots (all have sensible defaults; `Unauthenticated` routes through the `error` slot).
It animates `Content` changes with a stable transition key so data updates don't re-trigger the fade.

**Screen structure convention:** one **state per file** — a thin `<Feature>Screen.kt` entry wiring the
ViewModel, plus `<Feature>Content.kt` / `<Feature>Skeleton.kt` (loading) / `<Feature>Empty.kt` /
`<Feature>Error.kt`. Each **reusable component gets its own file** under `components/` (`internal`, so
sibling files can use it). Enclose the screen in `KptScaffold` (its 3rd overload is safe under
`KptRootScaffold` — the outer scaffold consumes the nav-bar inset).

---

## 6. DI wiring (Koin)

- **Store + repo binding** — `core/data/.../banking/di/BankingModule.kt`:
  ```kotlin
  single<Store<String, List<BankAccount>>>(AppStoreRegistry.Accounts) {
      BankingStores.accountsStore(get(), get()).registerForLogout(get())    // register for logout-clear
  }
  single<AccountsRepository> { AccountsRepositoryImpl(get(AppStoreRegistry.Accounts), get(), get()) }
  ```
  `AppStoreRegistry` (`core/store`) holds the qualifiers (`Accounts`/`Balances`/`Transactions` = named
  StringQualifiers); `registerForLogout` casts `StoreCacheManager as? StoreCacheManagerImpl` and
  registers the store so `clearAll()` wipes it on logout.
- **Feature module** — each feature exposes `val <Feature>Module = module { viewModelOf(::<Feature>ViewModel) }`
  (`feature/home/.../di/HomeModule.kt`), included in `cmp-navigation/.../di/KoinModules.kt`'s
  `featureModule.includes(...)`.
- **Infra seam** — `appStoreModule` (`core/store/.../di/StoreModule.kt`) binds `StoreCacheManager`.

---

## 7. Navigation

- **Destination** — `feature/<name>/<Feature>Destination.kt`: `@Serializable data object <Feature>Destination`
  / `<Feature>Route`, a `NavController.navigateTo<Feature>()` helper, and
  `fun NavGraphBuilder.<feature>Graph(onNavigate…: () -> Unit)` that hosts the screen and passes the
  nav callbacks down (the screen stays decoupled from the app's route table).
- **Bottom-nav tabs** — `cmp-navigation/.../authenticatednavbar/AuthenticatedNavBarTabItem.kt` defines the
  4 consumer tabs (Home · Accounts · Transactions · More). A real feature's tab points its
  `graphRoute`/`startDestinationRoute` at the feature's `Destination` (see `HomeTab`); an
  unimplemented tab points at a `cmp.navigation.placeholder` route rendering `PlaceholderScreen("…")`.
- **Placeholder routes** — `cmp-navigation/.../placeholder/BankingDestinations.kt` holds ~28 arg-less
  `@Serializable data object` routes + `bankingPlaceholderDestinations()` registering them as
  `PlaceholderScreen(title)`. As each feature ships, its route is removed here and owned by the feature.

### The 4 registration sites for a new feature
1. `settings.gradle.kts` → `include(":feature:<name>")`.
2. `cmp-navigation/.../di/KoinModules.kt` → add `<Feature>Module` to `featureModule.includes(...)`.
3. `cmp-navigation/.../authenticatednavbar/AuthenticatedNavbarNavigationScreen.kt` → add
   `<feature>Graph(...)` inside the `NavHost` (and remove the feature's route from
   `bankingPlaceholderDestinations()`).
4. `AuthenticatedNavBarTabItem.kt` → repoint the tab from the placeholder route to the feature's
   `Destination` (only for bottom-nav features).

---

## 8. Formatters (`core/common`) — reuse, don't duplicate

- `FormatMoney.kt` — `parseMinorUnits(str): Long?` (decimal string → pence, exact), `formatMinorUnits(pence, currency)` (→ `"£2,847.63"`), `formatMoney(str, currency)`, `formatSignedMoney(str, currency, isCredit)`, `currencySymbol(code)` (GBP→£, EUR→€, USD→$).
- `FormatDate.kt` — `formatShortMonthDay(iso)` (→ `"27 Jun"`), `formatDate(millis)`.
- `FormatAccount.kt` — `formatSortCode("400515")` → `"40-05-15"`.
- `FormatNumber.kt` — `Long.formatGrouped()` (→ `"2,847"`), `Double.formatDecimal/formatGrouped`.

Sums of decimal-string amounts are done as `amounts.sumOf { parseMinorUnits(it) ?: 0L }` then
`formatMinorUnits(...)` — sums are only valid within one currency.

---

## 9. Feature-module anatomy (canonical: `feature/home`)

```
feature/<name>/
  build.gradle.kts                      # alias(libs.plugins.cmp.feature.convention); compose{resources{packageOfResClass=…}}
  src/commonMain/kotlin/.../<name>/
    <Feature>Screen.kt                  # entry: koinViewModel + KptScaffold + ScreenContent
    <Feature>Content.kt / Skeleton / Empty / Error.kt   # one state per file
    <Feature>Destination.kt             # @Serializable data object + <feature>Graph() + navigateTo<Feature>()
    <Feature>TestTags.kt                # stable testTag object (see TEST_INFRASTRUCTURE.md)
    components/*.kt                      # reusable sub-components, own file each, internal
    ui/<Feature>ViewModel.kt            # BaseViewModel<State, Event, Action> + display-model mapping
    di/<Feature>Module.kt               # module { viewModelOf(::<Feature>ViewModel) }
  src/commonMain/composeResources/values/strings.xml    # feature_<name>_* keys
  src/commonTest/…                      # Fake repos + ViewModel test
  src/androidUnitTest/…  src/androidInstrumentedTest/…   # Compose UI tests (see TEST_INFRASTRUCTURE.md)
```
`build.gradle.kts` deps mirror `feature/home`: `projects.core.{common,data,model,ui}`,
`projects.coreBase.store`, coroutines, and the compose bundle; set
`packageOfResClass = "org.mifosx.openbanking.feature.<name>.generated.resources"`.

---

## 10. House conventions (enforced by spotless/detekt + review)

- **No `//` line comments** — KDoc `/** */` only, used sparingly; no-op bodies are `= Unit`.
- **No same-package fully-qualified type refs** — import and use the simple name.
- **Shared/brand components → `core/ui` only** (`Mifos*` prefix, KDoc + private `@Preview` in
  `MifosXOpenBankingTheme`, `modifier` right after required params). Never `core-base/ui` for new code.
- **Domain models in `core/model`, network DTOs in `core/network/model`, Room entities in
  `core/database`, repositories + mappers in `core/data`.** A domain enum never lives in `core/network`.
- **A Store is owned by its repository and never exposed above the data layer** — repos expose
  `Flow<ScreenState<Domain>>` (+ `refresh()`), keeping the ViewModel fakeable.
- **Formatting/derivation lives in the ViewModel**; composables render finished strings.
- **All user-facing text via `stringResource(Res.string.feature_<name>_*)`** over
  `commonMain/composeResources/values/strings.xml` (multi-language). Formatting-internal literals
  (month abbreviations, currency symbols) are data in the formatter, not string resources.
- Detekt: `ReturnCount` (max 2), `MaxLineLength` (120), `MagicNumber` (named consts),
  `LongParameterList` (justify a nav-callback list with `@Suppress`). Run `spotlessApply` + `detekt`
  continuously; **never `--no-verify`**.
- **Confirm module-boundary/placement decisions before writing** when unsure.

---

## 11. Verify commands

```bash
cd /home/kalpesh/OpenSource/Mifos/mifos-x-open-banking
./gradlew spotlessApply spotlessCheck detekt
./gradlew :core:data:koverHtmlReport                              # data-layer coverage
./gradlew :core:data:desktopTest :core:data:testDemoDebugUnitTest
./gradlew :feature:<name>:testDemoDebugUnitTest                   # ViewModel + Robolectric UI (JVM)
./gradlew :feature:<name>:connectedDemoDebugAndroidTest          # on-device instrumented (needs device)
./gradlew :cmp-android:assembleDemoDebug
```

See `TEST_INFRASTRUCTURE.md` for the full test-layer detail (Robolectric + instrumented setup, Kover,
conventions).
