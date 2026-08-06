package support.explainable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Guarded self-healing for Selenium locators.
 * A replacement is used only after a live visibility/enabled check and a confidence threshold.
 * It is promoted to a future-run override only after repeated successful validations.
 */
public final class SelfHealingLocatorAgent {
    public enum Strategy { CSS, ID, NAME, XPATH, DATA_TEST }

    public record Locator(Strategy strategy, String value) {
        public Locator {
            Objects.requireNonNull(strategy, "strategy");
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Locator value is required.");
        }
        public String display() { return strategy + ": " + value; }
    }

    public record Candidate(Locator locator, double baseConfidence, List<String> matchingSignals) {
        public Candidate {
            if (baseConfidence < 0 || baseConfidence > 1) throw new IllegalArgumentException("Confidence must be between 0 and 1.");
            matchingSignals = matchingSignals == null ? List.of() : List.copyOf(matchingSignals);
        }
    }

    public record ProbeResult(boolean visible, boolean enabled, String observed) {
        public boolean usable() { return visible && enabled; }
    }

    public record HealingDecision(String elementName, Locator failedLocator, Locator replacementLocator,
                                  String failureReason, double matchConfidence, int successfulRuns,
                                  int validationRuns, double replacementSuccessRate, boolean appliedThisRun,
                                  boolean promotedForFutureRuns, String businessUpdate) {
        public String successRateLabel() {
            return validationRuns == 0 ? "Not yet measured" : String.format(Locale.ROOT, "%.1f%% (%d/%d validated runs)",
                    replacementSuccessRate * 100, successfulRuns, validationRuns);
        }
    }

    public record ResolvedElement(WebElement element, HealingDecision decision) { }
    @FunctionalInterface public interface LocatorProbe { ProbeResult inspect(Locator locator); }

    private static final class Reliability {
        private int successfulRuns;
        private int validationRuns;
        void record(boolean success) { validationRuns++; if (success) successfulRuns++; }
        double rate() { return validationRuns == 0 ? 0 : (double) successfulRuns / validationRuns; }
    }

    private final double confidenceThreshold;
    private final int promotionMinimumRuns;
    private final double promotionSuccessThreshold;
    private final Map<String, Reliability> reliabilityByLocator = new HashMap<>();
    private final Map<String, Locator> promotedOverrides = new HashMap<>();

    public SelfHealingLocatorAgent() { this(0.80, 3, 0.85); }

    public SelfHealingLocatorAgent(double confidenceThreshold, int promotionMinimumRuns, double promotionSuccessThreshold) {
        this.confidenceThreshold = confidenceThreshold;
        this.promotionMinimumRuns = promotionMinimumRuns;
        this.promotionSuccessThreshold = promotionSuccessThreshold;
    }

    /** Use this to retain actual historic outcomes from prior CI/test runs. */
    public void recordHistoricalOutcome(Locator locator, boolean success) {
        reliability(locator).record(success);
    }

    /**
     * Selects and validates a replacement. The returned decision is an audit record and should be
     * written to the Explainable AI/Extent report.
     */
    public HealingDecision recover(String elementName, Locator failedLocator, String failureReason,
                                   List<Candidate> candidates, LocatorProbe probe) {
        Candidate winner = candidates.stream()
                .sorted(Comparator.comparingDouble(this::scoreCandidate).reversed())
                .filter(candidate -> scoreCandidate(candidate) >= confidenceThreshold)
                .filter(candidate -> probe.inspect(candidate.locator()).usable())
                .findFirst().orElse(null);

        if (winner == null) {
            return new HealingDecision(elementName, failedLocator, null, failureReason, 0, 0, 0, 0,
                    false, false, "The journey remains blocked because no safe replacement element was confirmed.");
        }

        double score = scoreCandidate(winner);
        Reliability reliability = reliability(winner.locator());
        reliability.record(true); // live validation passed: visible + enabled
        boolean promoted = reliability.validationRuns >= promotionMinimumRuns
                && reliability.rate() >= promotionSuccessThreshold;
        if (promoted) promotedOverrides.put(elementName, winner.locator());

        return new HealingDecision(elementName, failedLocator, winner.locator(), failureReason, score,
                reliability.successfulRuns, reliability.validationRuns, reliability.rate(), true, promoted,
                businessUpdate(elementName, reliability, promoted));
    }

    /**
     * Selenium entry point. The recovered element is used in this run. Its decision can be passed to
     * ExplainableAiIntegration/Extent reporting. An exception is rethrown when there is no safe candidate.
     */
    public ResolvedElement findWithRecovery(WebDriver driver, String elementName, Locator original,
                                             List<Candidate> candidates) {
        Locator primary = promotedOverrides.getOrDefault(elementName, original);
        try {
            WebElement element = driver.findElement(toBy(primary));
            if (element.isDisplayed() && element.isEnabled()) return new ResolvedElement(element, null);
            throw new NoSuchElementException("Locator resolved an unavailable element: " + primary.display());
        } catch (WebDriverException failure) {
            LocatorProbe seleniumProbe = locator -> {
                List<WebElement> matches = driver.findElements(toBy(locator));
                for (WebElement match : matches) {
                    try {
                        if (match.isDisplayed() && match.isEnabled())
                            return new ProbeResult(true, true, "Candidate is visible and enabled.");
                    } catch (WebDriverException ignored) { /* candidate did not remain usable */ }
                }
                return new ProbeResult(false, false, "Candidate not visible/enabled on the current page.");
            };
            List<Candidate> allCandidates = new ArrayList<>(candidates);
            allCandidates.addAll(discoverCandidates(driver, elementName));
            HealingDecision decision = recover(elementName, primary, rootMessage(failure), allCandidates, seleniumProbe);
            if (!decision.appliedThisRun()) throw failure;
            return new ResolvedElement(driver.findElement(toBy(decision.replacementLocator())), decision);
        }
    }


    /**
     * Bounded discovery: scans standard interactive controls and proposes only candidates whose
     * stable attributes semantically overlap with the logical element name. Every proposal still
     * must pass the live visible/enabled validation in recover().
     */
    public List<Candidate> discoverCandidates(WebDriver driver, String elementName) {
        List<Candidate> discovered = new ArrayList<>();
        List<String> targetTokens = tokens(elementName);
        for (WebElement element : driver.findElements(By.cssSelector("input,textarea,select,button,a"))) {
            try {
                String id = element.getAttribute("id");
                String dataTest = element.getAttribute("data-test");
                String name = element.getAttribute("name");
                String aria = element.getAttribute("aria-label");
                String attributes = String.join(" ", nonBlank(id), nonBlank(dataTest), nonBlank(name), nonBlank(aria));
                double overlap = overlap(targetTokens, tokens(attributes));
                if (overlap < 0.50) continue;
                Locator locator = !blank(id) ? new Locator(Strategy.ID, id)
                        : !blank(dataTest) ? new Locator(Strategy.DATA_TEST, dataTest)
                        : !blank(name) ? new Locator(Strategy.NAME, name) : null;
                if (locator == null) continue;
                List<String> signals = new ArrayList<>(List.of("semantic-token-match"));
                if (!blank(id) || !blank(dataTest)) signals.add("stable-attribute");
                if (!blank(aria)) signals.add("accessible-name");
                discovered.add(new Candidate(locator, 0.65 + (0.25 * overlap), signals));
            } catch (WebDriverException ignored) { /* element changed during DOM scan */ }
        }
        return discovered;
    }

    /** Executive-friendly HTML with technical evidence retained below the business summary. */
    public static void writeExecutiveHtml(Path destination, String testName, HealingDecision decision) throws IOException {
        Files.createDirectories(destination.getParent());
        String replacement = decision.replacementLocator() == null ? "No replacement applied" : decision.replacementLocator().display();
        String status = decision.appliedThisRun() ? "RECOVERED" : "BLOCKED";
        String content = """
            <!doctype html><html><head><meta charset="utf-8"><title>Test Resilience Executive Brief</title>
            <style>
            body{margin:0;background:#f4f7fb;color:#172033;font:15px Arial,sans-serif}.hero{background:#102a43;color:#fff;padding:34px 9%%}.hero h1{margin:0;font-size:28px}.hero p{color:#cbd5e1;margin:8px 0 0}.wrap{max-width:1180px;margin:28px auto;padding:0 24px}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.kpi,.panel{background:#fff;border-radius:12px;box-shadow:0 2px 8px #cbd5e166;padding:20px}.number{font-size:28px;font-weight:700;margin-bottom:7px}.label{color:#64748b}.good{color:#14804a}.warn{color:#b45309}.bad{color:#b42318}.panel{margin-top:18px}.panel h2{margin:0 0 14px;font-size:19px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.box{padding:15px;border-radius:8px;background:#f8fafc}.box b{display:block;margin-bottom:6px}.business{border-left:5px solid #2563eb;background:#eff6ff;padding:16px;margin:14px 0}.technical{font-family:Consolas,monospace;font-size:13px;background:#0f172a;color:#e2e8f0;padding:14px;border-radius:7px;overflow:auto}.badge{padding:5px 10px;border-radius:16px;background:#dcfce7;color:#166534;font-weight:bold}.blocked{background:#fee2e2;color:#991b1b}@media(max-width:760px){.kpis,.grid{grid-template-columns:1fr}}</style>
            </head><body><section class="hero"><h1>Test Resilience Executive Brief</h1><p>%s | Automated UI recovery and business impact summary</p></section>
            <main class="wrap"><section class="kpis"><div class="kpi"><div class="number %s">%s</div><div class="label">Test outcome after recovery</div></div><div class="kpi"><div class="number">%s</div><div class="label">Replacement reliability</div></div><div class="kpi"><div class="number">%s</div><div class="label">Locator match confidence</div></div><div class="kpi"><div class="number">%s</div><div class="label">Future-run status</div></div></section>
            <section class="panel"><h2>Business update</h2><div class="business"><b>%s</b><br>%s</div></section>
            <section class="panel"><h2>What changed automatically</h2><div class="grid"><div class="box"><b>Failed element</b>%s</div><div class="box"><b>Verified replacement</b>%s</div></div></section>
            <section class="panel"><h2>Why the original element failed</h2><p>%s</p><h2>Validation and governance</h2><p>The replacement was used only after it was present, visible, and enabled. It is %s for future runs.</p><div class="technical">Failure evidence: %s\nMatch signals and confidence: %.1f%%\nValidated replacement success: %s</div></section>
            </main></body></html>
            """.formatted(html(testName), decision.appliedThisRun() ? "good" : "bad", status,
                html(decision.successRateLabel()), String.format(Locale.ROOT, "%.1f%%", decision.matchConfidence() * 100),
                decision.promotedForFutureRuns() ? "Promoted" : "Monitoring", html(status), html(decision.businessUpdate()),
                html(decision.failedLocator().display()), html(replacement), html(plainFailureReason(decision.failureReason())),
                decision.promotedForFutureRuns() ? "approved for automatic reuse" : "being monitored before permanent reuse",
                html(decision.failureReason()), decision.matchConfidence() * 100, html(decision.successRateLabel()));
        Files.writeString(destination, content, StandardCharsets.UTF_8);
    }

    public static List<String> extentLogLines(HealingDecision decision) {
        List<String> lines = new ArrayList<>();
        lines.add("SELF-HEALING: " + (decision.appliedThisRun() ? "Replacement applied" : "No safe replacement"));
        lines.add("BUSINESS UPDATE: " + decision.businessUpdate());
        lines.add("LOCATOR CHANGE: " + decision.failedLocator().display() + " -> "
                + (decision.replacementLocator() == null ? "none" : decision.replacementLocator().display()));
        lines.add("REPLACEMENT SUCCESS: " + decision.successRateLabel());
        return lines;
    }

    private double scoreCandidate(Candidate candidate) {
        double signalBonus = Math.min(0.12, candidate.matchingSignals().stream().distinct().count() * 0.03);
        return Math.min(0.99, candidate.baseConfidence() + signalBonus);
    }
    private Reliability reliability(Locator locator) { return reliabilityByLocator.computeIfAbsent(locator.display(), ignored -> new Reliability()); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String nonBlank(String value) { return blank(value) ? "" : value; }
    private static List<String> tokens(String value) {
        if (blank(value)) return List.of();
        return List.of(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).stream()
                .filter(token -> token.length() > 1)
                .filter(token -> !List.of("field", "button", "input", "checkout", "page").contains(token))
                .distinct().toList();
    }
    private static double overlap(List<String> target, List<String> candidate) {
        if (target.isEmpty()) return 0;
        long matches = target.stream().filter(candidate::contains).count();
        return (double) matches / target.size();
    }
    private static By toBy(Locator locator) {
        return switch (locator.strategy()) {
            case CSS -> By.cssSelector(locator.value());
            case ID -> By.id(locator.value());
            case NAME -> By.name(locator.value());
            case XPATH -> By.xpath(locator.value());
            case DATA_TEST -> By.cssSelector("[data-test='" + locator.value().replace("'", "\\'") + "']");
        };
    }
    private static String rootMessage(Throwable error) { Throwable root = error; while (root.getCause() != null) root = root.getCause(); return root.getClass().getSimpleName() + ": " + root.getMessage(); }
    private static String plainFailureReason(String reason) {
        String lower = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        if (lower.contains("no such element") || lower.contains("timeout")) return "The page no longer exposed the expected checkout field when the test reached it.";
        return reason == null || reason.isBlank() ? "The original locator did not identify a usable page element." : reason;
    }
    private static String businessUpdate(String elementName, Reliability reliability, boolean promoted) {
        String outcome = "The customer journey continued without a manual test-script change.";
        String governance = promoted ? " The verified replacement is now approved for automatic reuse." : " The replacement is being monitored before automatic reuse.";
        return "The \"" + elementName + "\" control was recovered automatically. " + outcome + governance
                + " Current validated reliability: " + String.format(Locale.ROOT, "%.1f%%", reliability.rate() * 100) + ".";
    }
    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
