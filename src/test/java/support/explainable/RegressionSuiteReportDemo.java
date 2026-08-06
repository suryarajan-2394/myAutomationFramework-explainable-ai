package support.explainable;

import java.nio.file.Path;
import java.util.List;

/** Four-test example; the same collector supports 50-100+ parallel TestNG tests. */
public final class RegressionSuiteReportDemo {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "demo-output" : args[0]);
        RegressionSuiteReport.start("Sauce Demo Regression - Release Candidate");

        recordCleanPass("AddToCartTestCase", "Customer can add a product to the cart.");
        recordRecoveredCheckout();
        recordCleanPass("InvalidLoginMessageTest", "The correct login warning is shown to the customer.");
        recordFailure("CheckoutAddressValidationTest", "Checkout validation remained unavailable; release attention is required.");

        RegressionSuiteReport.write(output.resolve("regression-suite-executive-report.html"));
        System.out.println("Created suite report for four test cases.");
    }

    private static void recordCleanPass(String name, String business) {
        ExplainableAiAgent.Run run = ExplainableAiAgent.begin(name);
        ConsolidatedReportSession.start(name);
        ConsolidatedReportSession.setBusinessUpdate(business);
        run.record("Open application", "Page is displayed", "Page displayed", ExplainableAiAgent.Outcome.PASS, 300);
        ConsolidatedReportSession.recordExtentStep("PASS", "Open application", "Page displayed", 300);
        run.record("Validate journey", "Expected result is shown", "Expected result shown", ExplainableAiAgent.Outcome.PASS, 250);
        ConsolidatedReportSession.recordExtentStep("PASS", "Validate journey", "Expected result shown", 250);
        ExplainableAiAgent.Report report = run.complete(ExplainableAiAgent.Outcome.PASS, null);
        RegressionSuiteReport.record(ConsolidatedReportSession.finish(report));
    }

    private static void recordRecoveredCheckout() {
        String name = "CheckoutValidationExample";
        ExplainableAiAgent.Run run = ExplainableAiAgent.begin(name);
        ConsolidatedReportSession.start(name);
        ConsolidatedReportSession.setBusinessUpdate("Checkout continued without interruption after the validated locator recovery.");
        run.record("Open checkout", "First-name field is ready", "Original selector failed; fallback validated", ExplainableAiAgent.Outcome.PASS, 380);
        ConsolidatedReportSession.recordExtentStep("INFO", "Self-heal checkout first-name field", "CSS locator replaced by ID: first-name", 380);
        SelfHealingLocatorAgent healer = new SelfHealingLocatorAgent();
        SelfHealingLocatorAgent.Locator replacement = new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, "first-name");
        for (int i = 0; i < 6; i++) healer.recordHistoricalOutcome(replacement, true);
        healer.recordHistoricalOutcome(replacement, false);
        SelfHealingLocatorAgent.HealingDecision decision = healer.recover("Checkout first-name field",
                new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "[data-test='firstName']"),
                "NoSuchElementException: CSS selector [data-test='firstName'] did not match the current checkout page.",
                List.of(new SelfHealingLocatorAgent.Candidate(replacement, 0.84, List.of("stable-id", "same-field-label", "same-role"))),
                locator -> new SelfHealingLocatorAgent.ProbeResult(true, true, "Replacement visible and enabled."));
        ConsolidatedReportSession.recordHealing(decision);
        run.record("Enter customer first name", "Field accepts input", "First name accepted", ExplainableAiAgent.Outcome.PASS, 120);
        ConsolidatedReportSession.recordExtentStep("PASS", "Enter customer first name", "First name accepted", 120);
        RegressionSuiteReport.record(ConsolidatedReportSession.finish(run.complete(ExplainableAiAgent.Outcome.PASS, null)));
    }

    private static void recordFailure(String name, String business) {
        ExplainableAiAgent.Run run = ExplainableAiAgent.begin(name);
        ConsolidatedReportSession.start(name);
        ConsolidatedReportSession.setBusinessUpdate(business);
        run.record("Open checkout", "Address form is displayed", "Timeout waiting for address form", ExplainableAiAgent.Outcome.FAIL, 10000);
        ConsolidatedReportSession.recordExtentStep("FAIL", "Open checkout address form", "Timeout waiting for address form", 10000);
        ExplainableAiAgent.Report report = run.complete(ExplainableAiAgent.Outcome.FAIL, new RuntimeException(business));
        RegressionSuiteReport.record(ConsolidatedReportSession.finish(report));
    }
}
