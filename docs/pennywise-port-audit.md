# PennyWise contribution audit and Cashiro port

Compared Cashiro upstream `40a387fd` against all 31 user-authored PennyWise PRs. The shared ancestor is `498e098fb5abc24a3d71595c25baf57fb892bffd` (2025-12-14, v2.15.43); Cashiro's first separate commit is `037ae5aa` (2025-12-20). Ancestry and equivalent behavior are distinguished below. The initial port covered PNB/account repair, SMS scanning, GPay PDF parsing/deduplication/statement enrichment, and South Indian Bank parser updates. The same PR now also includes the follow-up scope listed below; earlier scope decisions in the historical table apply to the initial port.

| PennyWise PR | Audit and decision |
|---|---|
| [#66](https://github.com/sarim2000/pennywiseai-tracker/pull/66) | Present in the shared ancestry before the split; no repeat port. |
| [#68](https://github.com/sarim2000/pennywiseai-tracker/pull/68) | Present in the shared ancestry before the split; no repeat port. |
| [#70](https://github.com/sarim2000/pennywiseai-tracker/pull/70) | Present in the shared ancestry before the split; no repeat port. |
| [#73](https://github.com/sarim2000/pennywiseai-tracker/pull/73) | Present in the shared ancestry before the split; no repeat port. |
| [#74](https://github.com/sarim2000/pennywiseai-tracker/pull/74) | Present in the shared ancestry before the split; no repeat port. |
| [#75](https://github.com/sarim2000/pennywiseai-tracker/pull/75) | Present in the shared ancestry before the split; no repeat port. |
| [#76](https://github.com/sarim2000/pennywiseai-tracker/pull/76) | Present in the shared ancestry before the split; no repeat port. |
| [#79](https://github.com/sarim2000/pennywiseai-tracker/pull/79) | Present in the shared ancestry before the split; no repeat port. |
| [#84](https://github.com/sarim2000/pennywiseai-tracker/pull/84) | Present in the shared ancestry before the split; no repeat port. |
| [#85](https://github.com/sarim2000/pennywiseai-tracker/pull/85) | Present in the shared ancestry before the split; no repeat port. |
| [#103](https://github.com/sarim2000/pennywiseai-tracker/pull/103) | Present in the shared ancestry before the split; no repeat port. |
| [#140](https://github.com/sarim2000/pennywiseai-tracker/pull/140) | Present in adapted form: BaseIndianBankParser and regional base parsers already exist. Preserve Cashiro parser architecture. |
| [#181](https://github.com/sarim2000/pennywiseai-tracker/pull/181) | Closed unmerged: worker parallelization proposal. Use merged #220/#366 behavior instead; do not import its 43-file miscellaneous bundle. |
| [#213](https://github.com/sarim2000/pennywiseai-tracker/pull/213) | Closed unmerged worker proposal, superseded by merged #220. Covered by the selected worker port. |
| [#214](https://github.com/sarim2000/pennywiseai-tracker/pull/214) | Closed unmerged PNB proposal, superseded by #220 and current #873. Covered by the selected parser port. |
| [#220](https://github.com/sarim2000/pennywiseai-tracker/pull/220) | Partial: N parser coroutines and one transaction writer already exist, but unlimited queues, IO parsing, parser-side database writes and polling remain. Port bounded queues, CPU parsing and serialized side effects; retain Cashiro subscription handlers. Include the GPay anchor parser and cumulative South Indian Bank parser fixes described below. |
| [#242](https://github.com/sarim2000/pennywiseai-tracker/pull/242) | Present: transaction editor already selects source and target accounts. Add bank-qualified transfer identities only where required by account repair. |
| [#243](https://github.com/sarim2000/pennywiseai-tracker/pull/243) | Outside selected scope: currency-selector UI. Preserve Cashiro currency preferences and existing supported-currency infrastructure. |
| [#244](https://github.com/sarim2000/pennywiseai-tracker/pull/244) | Outside selected scope: Slice parser already exists, but this PR is not claimed as fully equivalent. No unrelated parser replacement. |
| [#245](https://github.com/sarim2000/pennywiseai-tracker/pull/245) | Already present on reinspection: CredParser exists and is registered in Cashiro. No duplicate CRED implementation is needed; this corrects the initial audit. |
| [#246](https://github.com/sarim2000/pennywiseai-tracker/pull/246) | Present: backup already includes rules when app preferences are selected. Extend that existing preference boundary for confirmed account mappings. |
| [#247](https://github.com/sarim2000/pennywiseai-tracker/pull/247) | Outside selected scope: budget-group ordering UI. No budget UI changes. |
| [#248](https://github.com/sarim2000/pennywiseai-tracker/pull/248) | Closed unmerged GPay proposal; superseded by merged #338 and maintainer fixes. Use their final matching/keeper behavior. |
| [#338](https://github.com/sarim2000/pennywiseai-tracker/pull/338) | Partial: three-minute UPI matching and SBI replacement existed; missing currency/account guards, delayed alerts, deterministic cleanup and in-place replacement. Port those behaviors. |
| [#339](https://github.com/sarim2000/pennywiseai-tracker/pull/339) | Partial: PDF parsing and duplicate preview exist, but override deletes/reinserts the bank transaction. Port inline PDF rows and unique same-day fallback as well as conservative statement enrichment retaining bank identity, row ID, balance links, category and hash. |
| [#340](https://github.com/sarim2000/pennywiseai-tracker/pull/340) | Outside selected scope: transaction-list redesign. Preserve Cashiro UI. |
| [#345](https://github.com/sarim2000/pennywiseai-tracker/pull/345) | Already sourced from Cashiro: no reverse port of Cashiro-origin parsers. |
| [#364](https://github.com/sarim2000/pennywiseai-tracker/pull/364) | Missing: RCS feed/count whitelist restricted to PNB and postal senders. Use supported-bank recognition in both paths. |
| [#366](https://github.com/sarim2000/pennywiseai-tracker/pull/366) | Partial: parallel pipeline exists; artificial polling, unbounded queues and uncapped parser workers remain. Port runtime improvements; retain Cashiro build versions. |
| [#862](https://github.com/sarim2000/pennywiseai-tracker/pull/862) | Missing: named UPI payee before generic fallback. Included with PNB parser fixes. |
| [#873](https://github.com/sarim2000/pennywiseai-tracker/pull/873) | Missing/partial: Cashiro already preserved currency symbols and From-name format; add remaining PNB formats, mandate subscriptions, confirmed suffix repair, backup mappings and bank-safe transfer retargeting. No PennyWise billing/profile model copied. |

## Maintainer follow-ups and adaptation

- GPay PDF parsing: user #220 `b92b290c` anchor/block parsing, maintainer #250 `151e7512` rolling date/year/time ownership and final-row handling, user #339 `f0a61e11`/`3b99d99d` inline rows and unique same-day enrichment, and maintainer `ce6eb1ab` diagnostic sanitization. The adapted parser emits no financial-value logs.
- Full South Indian Bank behavior: user #220 `b92b290c` IMPS merchant terminators and flexible references, base-parser/account normalization `c74ed767`/`b74b039b`, and #338 UPI fixes. Retains Cashiro java.time and existing compact PDF extraction compatibility.
- GPay bank reference extraction: #338 `b8f18a14` whitespace-tolerant South Indian Bank UPI merchant/reference patterns.
- Durable deletion: maintainer #714 `97c3e6c2` raw sender/body lookup prevents a deleted in-place replacement from returning when its retained hash differs from a rescan.
- GPay delayed-alert clustering: maintainer #612 `28817e55` uses a 24-hour window anchored to the first row.
- GPay keeper priority/review fixes: `b8f18a14`, `048a2ac3`, `e4fdf407`; statement enrichment `f0a61e11` and merged #339 follow-up.
- Phantom SBI cleanup `bb0755fc` (#715): remove only unlinked transaction-derived balance residue with no live transaction. Preserve manual/balance-only snapshots and rows linked even to soft-deleted transactions. The qualifying delete is atomic.
- Latest PennyWise #873 queued-rule correction `38c918a6`: Cashiro has no deferred balance queue. Its shared writer resolves confirmed aliases under the account mutation lock; reassigned rule destinations receive calculated deltas rather than the source bank's absolute balance. Card issuer/suffix continue to come from the original parsed facts even when a rule changes the transaction bank.
- Database migration registration: both the Hilt and receiver builders now share the complete manual migration list, including 61→62, followed by Room's automatic 62→63 bank-identity migration. Synthetic upgrade tests cover both starting versions.
- Cashiro-specific preservation: wallet/sample accounts are excluded from suggestions; no profile or Pro entitlement introduced; icons, colors, main-account preference, hidden-account preferences, existing parser cases and subscription handlers remain part of the native implementation.
- Worker follow-ups use immutable per-scan merchant/rule snapshots, batched unrecognized inserts that retain reported/deleted tombstones, rolling completion-rate estimates, durable completion counts consumed by the existing progress UI, and successful checkpoints after persistence. All-time scans record their scope and use the same incremental overlap on later runs; the initial all-time cutoff is epoch zero. Live saves retain fresh lookups. Rule-selected financial values remain separate from original account/card resolution facts.
- Rescan preservation adapts PennyWise `2d44231b`/`e4ab40cd` to Cashiro: lend/borrow links, manual/PDF records, deleted rows, attachments, descriptions and independent balance anchors survive rebuilding plain SMS transactions. Cleanup is atomic under the account mutation lock; it does not copy PennyWise-only profile/group models.
- Ledger persistence deliberately remains atomic: transaction and balance changes commit together under Cashiro's account mutation lock. A post-commit balance queue would add a cancellation/process-loss gap while both stages still serialize on that lock; the bounded parsing pipeline already overlaps CPU/IO work with persistence.
- Async hash/rule overlap is not copied: rule evaluation uses the scan snapshot and Room serializes transactional persistence. Existing Cashiro APIs and provider fallbacks are retained.
- Count queries were already narrow projections. The port adds a COUNT-only SMS fast path with provider-compatible fallback; RCS deliberately keeps an exact sender-filtered count using only tr_id and cached parser recognition. Unknown RCS senders are rejected before reading message parts. SMS and RCS readers run concurrently; parser workers use bounded queues and CPU dispatch, while ledger persistence remains atomic and serialized.

All new regression fixtures use synthetic banks, merchants, suffixes and references. No production SMS or device logs are included.


## Expanded port scope (same Cashiro PR)

The follow-up comparison uses PennyWise upstream `dabae2a0` and Cashiro upstream `40a387fd`, including the initial port through `920d3f53`.

| Area | PennyWise sources | Cashiro adaptation and verification focus |
|---|---|---|
| Currency correctness and refresh performance | #672 `5c563e04`, #556 `5555a10e` | Nullable conversion for missing rates; aggregate callers must skip unavailable conversions and retain native labels for individual values. Preserve Cashiro custom rates and share refresh work across concurrent requests. |
| Category rename safety | #858 `62f0d4c0` | Atomic updates across Cashiro transactions, merchant mappings, subscriptions, budget limits, rules and Lend/Borrow category references. Preserve subcategory IDs and unknown rule JSON fields. |
| Notification category actions | #709 `0b5b24ab` | Distinct PendingIntent identity for each full transaction ID and button slot. |
| Recent parser corrections | #836 `45a41b4d`, #863 `8f11627f`, #869 `f4d919b3`, #775 `221964e5` | UCO amount/balance separation; HDFC interest and PIXEL; shared reminder rejection; Karnataka payee/masked reference; Federal sender compatibility. Keep Cashiro's existing supported formats. |
| Statement/CSV import | Paytm #618 `c3d368bc`, Slice #667 `ce6eb1ab`, CSV #627 `cea54c5e` | Register new PDF parsers in the existing preview/import flow and expose CSV import with idempotent persistence. Preserve Cashiro-specific entity fields and avoid raw financial logs. |
| Expanded bank coverage | Latest registered parsers at `dabae2a0` | Add the 30 missing concrete parsers, required regional helpers, factory registration, notification aliases where needed, wallet routing and currency display support. |

The missing concrete parser set is: Apollo, Awash, BSF, Banco Agricola, Banco Cuscatlan, Banco Promerica, Bank of Abyssinia, bKash, Chase UK, Citizens Bank, D360, GTBank, Jana Small Finance Bank, Lumbini Bikash, Machchhapuchhre, Moniepoint, NDB, National Bank of Egypt, National Savings Bank, Nations Trust Bank, Nepal Bank, Nepal SBI, Pasargad, Pluxee, Siket, Standard Chartered Nepal, Standard Chartered Nigeria, Trading 212, VFD and ZamZam. The El Salvador base parser is an additional shared helper, not a thirty-first bank.

All new or adapted fixtures must use synthetic examples. Parser presence is checked through the factory; app integration must also account for notification-only providers and wallets without account suffixes. Verification results are recorded in the PR description after the combined gate runs.

## Additional follow-ups requested after the expanded port

These remain separate commits on the same PR, following the five expansion commits above.

| Port | PennyWise evidence | Cashiro adaptation |
|---|---|---|
| Slice SMS | `07a05b76`, `3edb24d1`, `64d020eb`, `82e41e3b`, `bb97c82c` | Modern bank alerts, SLCBNK sender, successful card/UPI/AutoPay formats and request rejection; retains legacy explicit-card classification. |
| Currency selectors | #243 (`a98a92b8`, `f13eb380`), #626 (`6b883600`) | Offline/custom catalog, selected currency visibility, search and scrolling; preserves native Cashiro selectors. |
| Rule correctness | `c55630fc`, `03976f77` | Left-to-right AND/OR evaluation and grouped preview, conservative type filtering, numeric amount equality. |
| Standalone rule sharing | `2ac58471` | Versioned validated JSON, bounded file reading, atomic import, Cashiro subcategory actions and privacy-safe errors. |
| Shared-text drafts | `a9992f7b` | Inbound plain-text share opens an editable draft after onboarding/app-lock checks; never saves automatically or assumes foreign-currency conversion. |
| Account-backed subscription payments | `0fa5a7cd` | Atomic per-cycle ledger, exact funding account, existing-payment linking, later SMS reconciliation, backup ID remapping and migration 63→64. Balance mutations and uniqueness include currency. |
| Explicit account tracking exclusions | `ccbd530a`, `4f1d43b6` | Separate from Hide; skip new transaction/balance messages, filter transaction listing, retain ignored history during rescans, reversible controls and backup persistence. Existing overview totals and mandate reminders retain their current behavior. |
| Balance discrepancy adjustment | `7028e958` | Bank/currency/transfer-qualified detection, revalidation before atomic adjustment, duplicate/tombstone protection and unchanged explicit balance anchors. |

New regression data is synthetic. Exact cumulative-commit build/test results are recorded in the PR description.
