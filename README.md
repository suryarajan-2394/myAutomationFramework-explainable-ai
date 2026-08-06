# Explainable AI agent for myAutomationFramework

This add-on makes each test **auditable**, rather than merely pass/fail. It is built for the repository's Selenium + TestNG + ExtentReports setup and does not require an API key or an external service.

## What it records

For each meaningful UI step, the agent stores:

- business action
- expected application state
- observed state or exception
- pass/fail outcome and elapsed time
- optional evidence path (for example, the screenshot your existing framework already captures)

At test completion, it writes an HTML report for humans and JSON for CI/analytics. The report labels the output as either an observed fact or a rule-based inference, so it does not present a guess as test evidence.

## Add the files

Copy the three Java files in src/test/java/support/explainable/ into the same path in the repository:

- ExplainableAiAgent.java — evidence model, explanation rules, HTML/JSON reporter
- ExplainableAiIntegration.java — thread-safe TestNG bridge
- ExplainableAiAgentDemo.java — runnable example (not needed in CI)

No pom.xml change is needed.

## Wire into BaseTest

Add these imports:

~~~java
import java.nio.file.Path;
import support.explainable.ExplainableAiAgent;
import support.explainable.ExplainableAiIntegration;
~~~

At the end of the existing @BeforeMethod setup(..., Method method), immediately after the framework creates its Extent test, add:

~~~java
ExplainableAiIntegration.start(method.getName());
~~~

Add this helper to BaseTest:

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

At the start of the existing @AfterMethod getResult(ITestResult result), after you determine its TestNG status but before driver.quit(), add:

~~~java
boolean passed = result.getStatus() == ITestResult.SUCCESS;
boolean skipped = result.getStatus() == ITestResult.SKIP;
Path explainableDir = Path.of(System.getProperty("user.dir"), "AutomationReports", "explainable");
ExplainableAiIntegration.finish(passed, skipped, result.getThrowable(), explainableDir);
~~~

The existing Extent report and screenshot behavior remains unchanged. This agent generates an additional report per test in AutomationReports/explainable.

## Instrument the existing CartTest

Replace a direct page-object action with explainedStep; retain the existing Extent log immediately after it:

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

The working four-test example is demo-output/regression-suite-executive-report.html. The same layout scales to 50-100 tests; for very large suites, leaders read the KPI and portfolio sections first while engineering expands only the relevant test evidence.
