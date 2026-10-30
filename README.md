# Explainable AI agent for myAutomationFramework

## Current implementation and research direction

`BaseTest` starts and finishes an evidence report for each TestNG method. `CartTest` records five business actions; login, add-to-cart, open-cart and logout have separate explicit state checks. Its cart-product validation already asserts the selected product in `CartPage`. Reports are written to `AutomationReports/explainable` and summarized in Extent. A screenshot failure is logged as a warning so the evidence report can still be written. `HomePage` supports opt-in recovery for the cart link only. Test class generation, LIME, ViT, and Healenium integration are proposed research work, not implemented features.

`TestData/testData.xlsx` is tracked in this repository, copied from the original framework. `BaseTest` reads its first sheet at suite startup; the cart scenario looks up `TCID` 3 and the `userName`, `password`, and `productName` columns. Because the repository is public, use this workbook only for public demo data. Keep private credentials in an untracked local replacement or a future secret-backed data source.

At test completion, the explainable HTML and JSON in `AutomationReports/explainable` and the screenshot in `AutomationReports/Screenshots` share a local timestamp, for example `explanation-03-10-2026_12-51-54.html` and `03-10-2026_12-51-54.png`. Windows forbids colons in filenames, so hours, minutes and seconds use hyphens. Parallel tests completed within one second receive an additional `_1`, `_2`, etc. suffix to avoid overwriting files. The Extent index remains `AutomationReports/TestAutomationReport.html`.

This is a deterministic, rule-based baseline. A completed action is recorded separately from an explicit state assertion. Other tests still need assertions before their successful actions can be interpreted as verified business outcomes.

### Verified cart increment

`explainedStep(action, expected, work, verifyState)` records an action result, then runs and records the state assertion independently. An assertion failure is rethrown to TestNG. The cart flow checks the inventory URL, one-item badge, cart URL, product name, and visible login button after logout. These checks target the repository's current SauceDemo journey; they require a working browser and configured test data. The separate local locator fixture below is an initial controlled sample; a broader CI mutation dataset is still needed.

### Opt-in cart locator experiment

Set `-Dframework.healing.cart.enabled=true` to enable recovery for `HomePage.clickOnCart()`. The original locator is `a.shopping_cart_link`; the sole reviewed fallback is `a[data-test='shopping-cart-link']`. Recovery requires the fallback to match exactly one visible, enabled element. Live discovery is disabled for this action. The explainable timeline records the selected locator separately from `CartTest`'s subsequent cart-page assertion. With the switch unset, the original PageFactory behavior is used.

The static pages in `src/test/resources/locator-fixture/` cover the original locator, a changed class, and duplicate fallback matches. `CartLocatorFixtureTest` checks that only the unique fallback reaches the cart and that duplicate candidates do not trigger a click. Run it on a machine with Chrome and ChromeDriver using `mvn -Dtest=CartLocatorFixtureTest test`. This is an initial controlled sample, not a broad benchmark or proof of correct healing on other sites. The existing agent's reliability counter measures visible/enabled probes; it does not establish post-action correctness or persist between runs. Record both the cart-page assertion and any wrong-target result when evaluating recovery.

The Chrome test profile disables password leak warnings alongside the existing password-manager settings so browser-owned prompts do not cover the inventory controls during the demo. A missing cart badge still fails the state assertion; use the captured screenshot and report to identify whether a browser prompt, failed product click, or changed page caused it.

### Proposed build order

1. **Reliable baseline:** add state assertions for login, cart badge, cart item and logout; include screenshots or DOM snippets on failure; run a stable e-commerce benchmark in CI. Record test, element, page, and build IDs in machine-readable reports.
2. **Guarded locator recovery:** route selected page-object locators through `SelfHealingLocatorAgent`; restrict candidates by page and element role, reject ambiguous matches, and validate the intended post-action state. Persist outcomes across CI runs and require review before promoting a replacement. Compare against plain Selenium and Healenium on controlled DOM changes; measure successful recovery, wrong-target/false-heal rate, and latency.
3. **Explainable ranking:** log candidate features, rejected alternatives, model version, and thresholds. Evaluate LIME on a trained locator-ranking model only if local surrogate fidelity is measured; do not label the current rule score as LIME. Consider ViT for visually grounded recovery where DOM signals fail, with screenshot redaction and an ablation against DOM-only ranking.
4. **Test class generator:** generate a TestNG/page-object draft from an approved scenario schema, compile it, run it in a sandbox against a fixture site, and require human review before committing. Measure compilation, assertion validity, coverage, and maintenance cost against handwritten tests.

For a PhD evaluation, keep train/test site families and change types separate, retain a frozen baseline, report confidence intervals, and treat a wrong-target click as a failure even when the test later passes. Keep AI providers optional and avoid sending credentials or customer data to a model.

This add-on makes each test **auditable**, rather than merely pass/fail. It is built for the repository's Selenium + TestNG + ExtentReports setup and does not require an API key or an external service.

## What it records

For each meaningful UI step, the agent stores:

- business action
- expected application state
- observed state or exception
- pass/fail outcome and elapsed time
- optional evidence path (for example, the screenshot your existing framework already captures)

At test completion, it writes an HTML report for humans and JSON for CI/analytics. The report labels the output as either an observed fact or a rule-based inference, so it does not present a guess as test evidence.

## Integration in this repository

The support classes are already in `src/test/java/support/explainable/`. The core files are:

- ExplainableAiAgent.java — evidence model, explanation rules, HTML/JSON reporter
- ExplainableAiIntegration.java — thread-safe TestNG bridge
- ExplainableAiAgentDemo.java — runnable example (not needed in CI)

No pom.xml change is needed.

## BaseTest wiring (already applied)

Add these imports:

~~~java
import java.nio.file.Path;
import support.explainable.ExplainableAiAgent;
import support.explainable.ExplainableAiIntegration;
~~~

`@BeforeMethod` starts the report immediately after creating the Extent test:

~~~java
ExplainableAiIntegration.start(method.getName());
~~~

`BaseTest` includes this helper:

~~~java
@FunctionalInterface
protected interface ExplainedAction { void run() throws Throwable; }

protected void explainedStep(String action, String expected, ExplainedAction work) throws Throwable {
    long started = System.nanoTime();
    try {
        work.run();
        ExplainableAiIntegration.record(action, expected, "Expectation met",
            ExplainableAiAgent.Outcome.PASS, (System.nanoTime() - started) / 1_000_000);
    } catch (Throwable failure) {
        ExplainableAiIntegration.record(action, expected,
            failure.getClass().getSimpleName() + ": " + failure.getMessage(),
            ExplainableAiAgent.Outcome.FAIL, (System.nanoTime() - started) / 1_000_000);
        throw failure;
    }
}
~~~

`@AfterMethod` finishes the report before `driver.quit()`:

~~~java
boolean passed = result.getStatus() == ITestResult.SUCCESS;
boolean skipped = result.getStatus() == ITestResult.SKIP;
Path explainableDir = Path.of(System.getProperty("user.dir"), "AutomationReports", "explainable");
ExplainableAiIntegration.finish(passed, skipped, result.getThrowable(), explainableDir);
~~~

The existing Extent report and screenshot behavior remains unchanged. This agent generates an additional report per test in AutomationReports/explainable.

## Instrumented CartTest

The cart flow now wraps its page-object actions with `explainedStep` and retains the Extent logs. This is an example of the pattern:

~~~java
explainedStep(
    "Log in as standard_user",
    "The inventory page is displayed",
    () -> loginPage.loginFunction(
        testData.getTestData("3", "userName"),
        testData.getTestData("3", "password")));
extentTestThread.get().log(Status.PASS, "Login action performed successfully");

explainedStep(
    "Add configured product to cart",
    "The cart badge reflects the added product",
    () -> homePage.selectProduct(testData.getTestData("3", "productName")));
extentTestThread.get().log(Status.PASS, "Product is selected and is added to Cart successfully");

explainedStep(
    "Validate cart product",
    "The configured product is present in the cart",
    () -> cartPage.validateCartpage(testData.getTestData("3", "productName")));
extentTestThread.get().log(Status.PASS, "Product in Cart validated successfully");
~~~

Use this pattern for clickOnCart(), logout, and the existing login/error-validation tests. Because explainedStep rethrows the original failure, current TestNG/Extent status and screenshots still work.

## Run the demo

~~~powershell
javac -d work/classes (Get-ChildItem -Recurse src/test/java/support/explainable/*.java).FullName
java -cp work/classes support.explainable.ExplainableAiAgentDemo demo-output
~~~

The demo intentionally creates a passing cart flow and a failing checkout step. The latter shows how the agent identifies a timeout as the closest observed cause and recommends an explicit, state-based wait.

## Production boundary

This is explainable test intelligence, not an LLM call: it never sends test data, screenshots, or application content outside your environment. If later you want natural-language triage from a hosted model, add it as an **optional** post-processor over the generated JSON, with secret management and PII redaction.

## Show Explainable AI beside Extent

Use integration-snippets/BaseTest-extent-additions.java after the existing result status code. It logs the AI summary, diagnosis, and recommendation into the current Extent test. The complete HTML/JSON evidence is still generated under AutomationReports/explainable. A visual example is in sample/extent-with-explainable-ai.html.


## Self-healing locator agent

SelfHealingLocatorAgent.java adds guarded locator recovery for Selenium. It works as follows:

1. The original locator is tried.
2. If it fails, the agent evaluates approved alternatives and bounded live-page candidates derived from stable attributes such as id, name, data-test, and aria-label.
3. A candidate must be visible, enabled, and score at least 80% match confidence before it is used.
4. The recovered locator is measured on every validation. It is promoted for later runs only after at least 3 validations and an observed success rate of 85% or more.

Use integration-snippets/SelfHealingLocator-Extent-integration.java in your page object or BasePageClass. It logs the original locator, replacement, cause, match confidence, reliability percentage, and business impact into the existing Extent test. It also writes a CXO-friendly executive HTML brief.

The sample assumes the obsolete checkout selector [data-test='firstName'] failed and the agent verified ID: first-name. The 87.5% shown is an observed rate of 7 successful validations from 8 measured runs; it is not a model prediction.


## One consolidated stakeholder report

ConsolidatedReportSession.java produces a single HTML brief containing the test's Extent-style execution timeline, Explainable AI conclusion, and self-healing locator audit. Use integration-snippets/Consolidated-Extent-Explainable-report.java to wire it into BaseTest.

The report deliberately leads with the decision-relevant information: outcome after recovery, number of executed steps, observed replacement reliability, locator match confidence, and business impact. It then shows the exact test steps, original failed locator, verified replacement, and the evidence behind the automated change.

A working demonstration is demo-output/consolidated-extent-explainable-cxo-report.html.


## Regression suites: 4 to 100+ tests

Use RegressionSuiteReport.java and integration-snippets/RegressionSuite-Consolidated-report.java. Start the suite in BeforeSuite, capture the existing Extent-style events and Explainable AI/self-healing result per test, then write one report in AfterSuite.

The suite collector is safe for parallel TestNG runs: each test has an isolated thread-local collection; completed test summaries enter a concurrent suite queue. The executive page gives portfolio-level pass/recovered/blocked metrics and every test can be expanded to show its action timeline, explanation, failed/replaced locator, reliability percentage, and business update.

The working four-test example is demo-output/regression-suite-executive-report.html. It now includes an executive visual summary: an outcome donut chart, Extent-style step-quality bars, and a self-healing effectiveness bar. The same layout scales to 50-100 tests; for very large suites, leaders read the KPI and visual summary first while engineering expands only the relevant test evidence.


### HTML reports and controlled recovery benchmark

HTML is now the primary output. Per-test pages are written to
`AutomationReports/explainable/explanation-DD-MM-YYYY_HH-mm-ss.html`.
JSON is optional: enable it with `-Dframework.reporting.json.enabled=true`.
Hyphens replace colons in filenames for Windows compatibility.

Run these commands individually on the increment branch:

```powershell
mvn "-Dtest=RecoveryEvidenceTest" test
mvn "-Dtest=CartLocatorFixtureTest" test
mvn "-Dtest=LocatorBenchmarkTest" test
mvn "-Dtest=CartTest" "-Dframework.healing.cart.enabled=true" test
```

Open the newest `AutomationReports/benchmark/locator-benchmark-*.html` in a browser.
It is a standalone, shareable page with embedded screenshots, plain-language
outcomes, timing and correct-recovery precision. The benchmark does not require Excel
or the live e-commerce site; it requires local Chrome and a compatible driver.
It compares seven labelled scenarios against plain Selenium (14 executions).
A passing characterization suite means the expected labels matched, **not** that
every customer journey succeeded. The deliberately wrong destination should be
labelled FALSE HEAL: it exposes that uniqueness, visibility and enabled state do
not prove semantic correctness. Execution errors are displayed separately.

Recovery reliability now counts explicit business-outcome checks rather than
element availability. CartTest records the navigation assertion result. A candidate
score is a deterministic rule score, not a calibrated probability or a LIME explanation.
In-memory promotion remains experimental and is not persisted between CI runs.

Next research increments: add destination/role constraints and stale-element cases;
run repeated mutations and report recovery recall, false-healing rate, latency
distributions and confidence intervals; record complete candidate/rejection traces;
compare Healenium on the same fixtures; then add model-based ranking with LIME fidelity
checks and compile/assertion validation for generated test classes. ViT, LIME,
Healenium and AI test generation are proposals, not implemented features of this increment.

### Single stakeholder report

Run LocatorBenchmarkTest, then CartTest. The timestamped per-test explanation HTML
now includes the latest local benchmark decision summary. Share that one explanation
HTML for the live test result and benchmark overview. The section identifies the source
benchmark filename (including its timestamp) and explains that it is separate evidence,
which may come from an earlier code version. Rerun the benchmark after code changes.
Missing or unreadable benchmark summaries show an explicit unavailable message rather
than invented metrics. The detailed benchmark screenshots remain in the standalone
benchmark report; the combined page includes the decision summary only.

### One-command stakeholder run

```powershell
mvn -Pstakeholder-report test
```

This profile runs evidence checks, then the controlled benchmark, then the live
CartTest in one ordered TestNG suite with cart healing enabled. Do not add -Dtest,
which overrides suite selection. It requires Chrome, live SauceDemo access and
the tracked Excel workbook. Share the newest timestamped explanation HTML.
The benchmark decision summary now includes plain-English stakeholder, tech-lead
and architect guidance plus proposed research improvements. Recommendations are
rule-based and do not change locator behavior. The current wrong-target case
still intentionally demonstrates false healing. A passing live journey is separate
from the readiness decision for recovery.

### Safer cart recovery and readiness targets

Cart recovery now checks anchor/link role, aria-disabled state and the exact expected
cart destination on the same origin before selecting a replacement, and rechecks
uniqueness and usability before returning it. Post-click business verification remains
mandatory: JavaScript redirects can still defeat a static href check. The wrong-target
fixture now expects rejection before clicking, while changed-class must still recover.
The combined report uses metric cards and separated improvement paragraphs. Proposed
project acceptance targets are >=95% recovery precision, zero observed incorrect
recoveries, correct outcomes on every safety fixture, and at least 100 independent
mutations before broader rollout. These are proposed targets, not an external standard.
Rerun the benchmark to replace previous 50% results; improved scores must come from
correct safe recovery, never relaxed assertions or removal of difficult scenarios.

### JavaScript cart fix and automatic repair history

SauceDemo's cart component is an anchor with role=button and an onClick router
handler, without href. The guard now supports this reviewed pattern only when
data-test is shopping-cart-link, its accessible name starts with Cart, and the
current page is inventory.html. Explicit href links still require the expected
same-origin cart path. Source reference:
https://github.com/saucelabs/sample-app-web/blob/main/src/components/CartButton.jsx

The benchmark now includes this JavaScript navigation pattern (8 scenarios, 16
executions). Runtime repair history records the original failure, replacement
locator, source call-site file and line, and verified second locator attempt.
This is a bounded locator retry within the same test, not a full test rerun or
a source file rewrite. It avoids repeating earlier shopping actions. When no
locator breaks, the report explicitly states no repair was required. If no
approved safe candidate exists, the test remains failed with a blocked-repair
reason. Missing candidates and business/assertion failures cannot be healed by
blind retries. Cart healing remains opt-in outside the stakeholder profile;
other page actions are not yet wired into this runtime recovery integration.

Run `mvn -Pstakeholder-report test` after pulling. Inspect both the live outcome
and benchmark evidence; a benchmark score does not override a failed live test.


### Jenkins stakeholder pipeline

The Jenkinsfile runs `mvn -B -Pstakeholder-report clean test`, replacing the old
testng.xml-only command. Current generated report output is cleared before tests
so an earlier successful benchmark cannot be mistaken for this build's evidence.
Timestamped explanation HTML, detailed benchmark HTML, screenshots, the report ZIP
and Surefire reports are archived in post.always, including on a failed Maven run.
JUnit-format Surefire XML is published for Jenkins test results. Missing XML is
allowed for a build that fails before tests; Maven's failure status is retained.

Configure an agent with Java 17 or newer, Chrome, and access to SauceDemo and
ChromeDriver (or an already provisioned compatible driver). The Maven tool name
must match `MavenLocal` in Manage Jenkins > Tools. On a Linux agent without a
display, the live CartTest additionally needs a virtual display; its browser
setup currently remains headed. Windows agents need a usable desktop session.

Required pipeline steps include JUnit and Email Extension, plus Timestamper for
the existing timestamps option. SMTP credentials belong in Jenkins's Extended
E-mail Notification settings; `credentialsId` is not an emailext step parameter.
The existing email recipient is retained. This change configures future pipeline
notifications; no Jenkins build or email was executed during implementation.

After merging this pipeline PR, configure Pipeline script from SCM with branch
main and script path Jenkinsfile, then Build Now. Download the current timestamped
explanation HTML from build artifacts for the combined stakeholder overview.

### Verified recovery evidence increment (21 October 2026)

Continue using one command: `mvn -Pstakeholder-report test`.

* The benchmark records replacement application before click and verification.
  Applied repairs that end in execution errors remain in the conservative verified
  recovery-rate denominator. False heals, unverified applied repairs, execution
  errors, guarded abstentions and incomplete/failed cases are reported separately.
  An original-locator failure is not counted as a false heal. Chrome startup errors
  are now recorded as execution errors rather than silently missing executions.
* Live recovery is credited only after the exact cart URL and Your Cart title,
  configured product, quantity=1 and absence of extra product rows are verified.
  Navigation success is logged separately while business verification is pending.
* Every proposed candidate retains its locator, rule score, matching signals,
  selection/rejection reason and probe result. Lower-ranked candidates that were
  not examined are explicitly marked NOT PROBED. Rejected recovery retains this
  evidence in the stakeholder repair history; original failures remain visible.
* Benchmark and live reports must have the same process run ID and code revision.
  Old/legacy reports do not silently supply current metrics. The default identity
  is one UUID per Maven test JVM plus Git HEAD (with a dirty-working-tree label).
  Unknown Git provenance is explicitly labelled; matching IDs alone do not prove
  revision verification when Git is unavailable. Prefer the single stakeholder
  invocation; separate commands intentionally do not share benchmark evidence.
  Advanced callers can provide matching `framework.reporting.runId` and
  `framework.reporting.commit` properties, taking responsibility for provenance.

Optional CI settings: `-Dframework.browser.headless=true` for the live Chrome test
and `-Dframework.browser.binary=/path/to/chrome` for a provisioned browser. The
fixture benchmark remains headless. No browser sandbox settings are disabled by
the repository. Timestamped HTML remains primary; JSON remains opt-in.

Run fast regression checks without Chrome or the live site:

```powershell
mvn "-Dtest=RecoveryEvidenceTest,CandidateAuditTest,BenchmarkMetricsTest,BenchmarkSummaryTest,CartRecoveryPolicyTest,RepairTraceTest,CartPageEvidenceTest" test
```

The benchmark still uses eight controlled scenarios rather than a production
accuracy sample. LIME, ViT, Healenium comparison and AI test generation remain
future research. Safety checks and business assertions are never weakened to
raise a reported score.

### Product scoping and labelled recovery coverage (30 October 2026)

`HomePage.selectProduct` now finds one inventory card by exact product name, then
uses only that card's button. Missing/duplicate product cards, missing/ambiguous
buttons, unavailable controls and unexpected action text fail without touching a
neighbouring product. An already-selected product is left selected. Inventory
readiness uses an explicit wait; the former global Remove-button precheck and
product-action implicit-wait assignment are removed. Runtime improvements must
still be measured on the actual browser/site.

The locator benchmark now has 13 labelled scenarios, each run in baseline and
guarded mode (26 executions). Labels are authored in BenchmarkScenarios,
independently from observed decisions. Recovery coverage is correctly recovered
eligible original-locator failures divided by all eligible failures in guarded
mode. Eligible cases ending in refusal or execution error remain missed cases.
Original-unchanged cases and unsafe replacements are excluded from this denominator.
The previous all-applied verified recovery rate and incorrect-recovery reporting
remain alongside coverage. The combined explanation report includes coverage.

Two deliberate characterization cases prevent misleading perfect scores:
* changed-data-test.html is recoverable in principle, but the current reviewed
  fallback cannot identify it. It should be recorded as a missed eligible case.
* script-redirect.html has a plausible href but JavaScript opens the wrong page.
  The current static guard cannot predict this; business verification must record
  FALSE HEAL. A passing characterization test confirms that this unsafe outcome
  was detected; it does not approve production readiness. The case remains in
  the benchmark until the policy is improved and its independent expectation reviewed.

ScenarioSuggestions provides rule-based, evidence-triggered proposals for missing
scenario families and expected behaviours. They appear in the benchmark and the
combined stakeholder report, require human review, and never generate or alter
executable tests. No LLM/AI generator has been added in this increment.

Run `mvn -Pstakeholder-report test` as before. For fast checks, add
`ProductSelectionTest` and `ScenarioSuggestionsTest` to the existing unit-test
command. Browser fixture regressions also cover a selected neighbouring product,
repeated selection, and a missing intended action. Existing CI report archiving
continues to collect the timestamped HTML.
