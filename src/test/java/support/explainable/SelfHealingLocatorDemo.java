package support.explainable;

import java.nio.file.Path;
import java.util.List;

/** Demonstrates a failed checkout locator being recovered by a verified replacement. */
public final class SelfHealingLocatorDemo {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "demo-output" : args[0]);
        SelfHealingLocatorAgent agent = new SelfHealingLocatorAgent();

        SelfHealingLocatorAgent.Locator replacement =
                new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, "first-name");

        // Simulate seven prior real validations: six successes and one miss.
        for (int i = 0; i < 6; i++) agent.recordHistoricalOutcome(replacement, true);
        agent.recordHistoricalOutcome(replacement, false);

        SelfHealingLocatorAgent.Locator failed =
                new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "[data-test='firstName']");
        SelfHealingLocatorAgent.Candidate candidate = new SelfHealingLocatorAgent.Candidate(
                replacement, 0.84, List.of("stable-id", "same-field-label", "same-role"));

        SelfHealingLocatorAgent.HealingDecision decision = agent.recover(
                "Checkout first-name field", failed,
                "NoSuchElementException: CSS selector [data-test='firstName'] did not match the current checkout page.",
                List.of(candidate),
                locator -> new SelfHealingLocatorAgent.ProbeResult(true, true, "Replacement is visible and enabled."));

        SelfHealingLocatorAgent.writeExecutiveHtml(output.resolve("self-healing-cxo-report.html"),
                "CheckoutValidationExample", decision);
        System.out.println(decision.businessUpdate());
        System.out.println("Replacement: " + decision.replacementLocator().display());
        System.out.println("Success rate: " + decision.successRateLabel());
    }
}
