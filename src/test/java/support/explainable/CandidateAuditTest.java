package support.explainable;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
public class CandidateAuditTest {
    private SelfHealingLocatorAgent.Candidate candidate(String id, double score) {
        return new SelfHealingLocatorAgent.Candidate(new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.ID, id), score, List.of("stable attribute"));
    }
    @Test public void recordsRejectedSelectedAndUnprobedCandidates() {
        var agent = new SelfHealingLocatorAgent();
        var decision = agent.recover("Cart", candidate("old", .9).locator(), "Original missing",
                List.of(candidate("hidden", .95), candidate("cart", .9), candidate("unused", .85), candidate("weak", .2)),
                locator -> new SelfHealingLocatorAgent.ProbeResult(!locator.value().equals("hidden"), true, locator.value().equals("hidden") ? "Hidden control" : "Safety rule passed"));
        Assert.assertEquals(decision.candidates().size(), 4);
        Assert.assertEquals(decision.candidates().get(0).disposition(), "REJECTED");
        Assert.assertEquals(decision.candidates().get(0).observation(), "Hidden control");
        Assert.assertEquals(decision.candidates().get(1).disposition(), "SELECTED");
        Assert.assertEquals(decision.candidates().get(2).disposition(), "NOT PROBED");
        Assert.assertTrue(decision.candidates().get(3).observation().contains("threshold"));
        Assert.assertEquals(agent.verifyOutcome(decision, true).candidates(), decision.candidates());
    }
}
