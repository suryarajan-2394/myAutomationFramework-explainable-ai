package support.explainable;

import java.nio.file.Path;
import java.util.List;

/** Creates one report containing the executed Extent-style steps and the locator recovery audit. */
public final class ConsolidatedReportDemo {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "demo-output" : args[0]);
        ExplainableAiAgent.Run run = ExplainableAiAgent.begin("CheckoutValidationExample");
        ConsolidatedReportSession.start("CheckoutValidationExample");

        run.record("Open Sauce Demo", "Login page displayed", "Login page displayed", ExplainableAiAgent.Outcome.PASS, 400);
        ConsolidatedReportSession.recordExtentStep("PASS", "Open Sauce Demo", "Login page displayed", 400);
        run.record("Log in", "Inventory page displayed", "Inventory page displayed", ExplainableAiAgent.Outcome.PASS, 620);
        ConsolidatedReportSession.recordExtentStep("PASS", "Log in as standard_user", "Inventory page displayed", 620);

        SelfHealingLocatorAgent healer = new SelfHealingLocatorAgent();
        SelfHealingLocatorAgent.Locator replacement = new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, "first-name");
        for (int i = 0; i < 6; i++) healer.recordHistoricalOutcome(replacement, true);
        healer.recordHistoricalOutcome(replacement, false);
        SelfHealingLocatorAgent.HealingDecision decision = healer.recover(
                "Checkout first-name field",
                new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "[data-test='firstName']"),
                "NoSuchElementException: CSS selector [data-test='firstName'] did not match the current checkout page.",
                List.of(new SelfHealingLocatorAgent.Candidate(replacement, 0.84, List.of("stable-id", "same-field-label", "same-role"))),
                locator -> new SelfHealingLocatorAgent.ProbeResult(true, true, "Replacement visible and enabled."));
        ConsolidatedReportSession.recordHealing(decision);
        run.record("Open checkout", "First-name field is ready", "Original locator failed; verified replacement ID: first-name applied", ExplainableAiAgent.Outcome.PASS, 380);
        ConsolidatedReportSession.recordExtentStep("INFO", "Self-heal checkout first-name field", "CSS locator replaced by ID: first-name; recovery validated", 380);
        run.record("Enter customer first name", "Field accepts input", "First name accepted", ExplainableAiAgent.Outcome.PASS, 120);
        ConsolidatedReportSession.recordExtentStep("PASS", "Enter customer first name", "First name accepted", 120);

        ExplainableAiAgent.Report report = run.complete(ExplainableAiAgent.Outcome.PASS, null);
        ConsolidatedReportSession.write(output.resolve("consolidated-extent-explainable-cxo-report.html"), report);
        System.out.println("Created consolidated report with " + decision.successRateLabel() + " replacement reliability.");
    }
}
