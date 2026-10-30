package support.explainable;

import java.util.List;

/** Ground truth is authored independently from the recovery decision. */
final class BenchmarkScenarios {
    record Scenario(String file, boolean recoverable, String guardedOutcome) { }
    static final List<Scenario> ALL = List.of(
            new Scenario("original.html", false, "ORIGINAL WORKED"),
            new Scenario("changed-class.html", true, "VERIFIED RECOVERY"),
            new Scenario("inventory.html", true, "VERIFIED RECOVERY"),
            new Scenario("nested-cart.html", true, "VERIFIED RECOVERY"),
            new Scenario("changed-data-test.html", true, "BLOCKED SAFELY"),
            new Scenario("duplicate.html", false, "BLOCKED SAFELY"),
            new Scenario("missing.html", false, "BLOCKED SAFELY"),
            new Scenario("hidden.html", false, "BLOCKED SAFELY"),
            new Scenario("disabled.html", false, "BLOCKED SAFELY"),
            new Scenario("wrong-target.html", false, "BLOCKED SAFELY"),
            new Scenario("wrong-role.html", false, "BLOCKED SAFELY"),
            new Scenario("aria-disabled.html", false, "BLOCKED SAFELY"),
            new Scenario("script-redirect.html", false, "FALSE HEAL"));
    static Scenario find(String file) { return ALL.stream().filter(s -> s.file.equals(file)).findFirst().orElseThrow(); }
}
