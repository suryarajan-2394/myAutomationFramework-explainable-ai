package support.explainable;

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RecoveryEvidenceTest {
    @Test
    public void availabilityDoesNotCountAsBusinessSuccess() {
        var agent = new SelfHealingLocatorAgent();
        var original = new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, ".old");
        var replacement = new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, "cart");
        var candidates = List.of(new SelfHealingLocatorAgent.Candidate(replacement, .9, List.of("stable attribute")));
        var selected = agent.recover("Cart", original, "Missing", candidates,
                locator -> new SelfHealingLocatorAgent.ProbeResult(true, true, "Available"));
        Assert.assertEquals(selected.validationRuns(), 0);
        Assert.assertFalse(selected.promotedForFutureRuns());
        var failed = agent.verifyOutcome(selected, false);
        Assert.assertEquals(failed.validationRuns(), 1);
        Assert.assertEquals(failed.successfulRuns(), 0);
        Assert.assertFalse(failed.promotedForFutureRuns());
        Assert.expectThrows(IllegalStateException.class, () -> agent.verifyOutcome(selected, true));
    }

    @Test
    public void unusableCandidatesNeverBecomeRecoveries() {
        var agent = new SelfHealingLocatorAgent();
        var locator = new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, "cart");
        var decision = agent.recover("Cart", locator, "Missing",
                List.of(new SelfHealingLocatorAgent.Candidate(locator, .9, List.of())),
                ignored -> new SelfHealingLocatorAgent.ProbeResult(false, true, "Hidden"));
        Assert.assertFalse(decision.appliedThisRun());
        Assert.assertEquals(decision.validationRuns(), 0);
    }
}
