package support.explainable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.utility;

/** Controlled characterization: test success means the labelled outcome matched, not that every journey succeeded. */
public class LocatorBenchmarkTest {
    private record Result(String scenario, String mode, String outcome, String reason, long millis, String screenshot) { }
    private final List<Result> results = new ArrayList<>();

    @DataProvider(name = "cases")
    public Object[][] cases() {
        List<Object[]> cases = new ArrayList<>();
        for (String file : List.of("original.html", "changed-class.html", "duplicate.html",
                "missing.html", "hidden.html", "disabled.html", "wrong-target.html")) {
            for (boolean healing : List.of(false, true)) cases.add(new Object[]{file, healing});
        }
        return cases.toArray(Object[][]::new);
    }

    @Test(dataProvider = "cases")
    public void benchmark(String fixture, boolean healing) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1200,800");
        WebDriver driver = new ChromeDriver(options);
        String outcome = "EXECUTION ERROR", reason = "";
        long started = System.nanoTime();
        String screenshot = "";
        SelfHealingLocatorAgent agent = new SelfHealingLocatorAgent();
        SelfHealingLocatorAgent.HealingDecision decision = null;
        try {
            driver.get(Path.of("src/test/resources/locator-fixture", fixture).toAbsolutePath().toUri().toString());
            try {
                if (healing) {
                    var resolved = agent.findWithApprovedRecovery(driver, "Shopping cart",
                            new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "a.shopping_cart_link"),
                            List.of(new SelfHealingLocatorAgent.Candidate(
                                    new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.DATA_TEST, "shopping-cart-link"),
                                    .9, List.of("reviewed stable attribute"))));
                    decision = resolved.decision();
                    resolved.element().click();
                } else driver.findElement(By.cssSelector("a.shopping_cart_link")).click();
                boolean correct = driver.getCurrentUrl().endsWith("/cart.html")
                        && driver.findElements(By.tagName("h1")).stream().anyMatch(e -> e.getText().equals("Cart fixture"));
                if (decision != null) {
                    decision = agent.verifyOutcome(decision, correct);
                    reason = decision.businessUpdate() + " Rule score: " + decision.matchConfidence()
                            + "; " + decision.successRateLabel() + ". Score is not a calibrated probability.";
                }
                outcome = correct ? (decision == null ? "ORIGINAL WORKED" : "VERIFIED RECOVERY") : "FALSE HEAL";
            } catch (NoSuchElementException unavailable) {
                outcome = "BLOCKED SAFELY";
                int matches = driver.findElements(By.cssSelector("[data-test='shopping-cart-link']")).size();
                reason = !healing ? "The original locator was unavailable; baseline does not recover."
                        : matches != 1 ? "Replacement must match exactly one element; found " + matches + "."
                        : "The candidate was not visible and enabled. No click was performed.";
            }
            screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            String expected = fixture.equals("original.html") ? "ORIGINAL WORKED"
                    : fixture.equals("changed-class.html") && healing ? "VERIFIED RECOVERY"
                    : fixture.equals("wrong-target.html") && healing ? "FALSE HEAL" : "BLOCKED SAFELY";
            Assert.assertEquals(outcome, expected, "Unexpected benchmark outcome for " + fixture);
        } finally {
            results.add(new Result(fixture, healing ? "Guarded recovery" : "Selenium baseline", outcome,
                    reason, (System.nanoTime() - started) / 1_000_000, screenshot));
            driver.quit();
        }
    }

    @AfterClass(alwaysRun = true)
    public void writeHtml() throws Exception {
        long healed = results.stream().filter(r -> r.outcome.equals("VERIFIED RECOVERY")).count();
        long falseHeals = results.stream().filter(r -> r.outcome.equals("FALSE HEAL")).count();
        long applied = healed + falseHeals;
        String precision = applied == 0 ? "Not measured" : String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * healed / applied);
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Shopping cart resilience benchmark</title><style>
                body{font:17px system-ui;background:#f1f5f9;color:#183047;margin:0}main{max-width:1000px;margin:auto;padding:32px}
                article,header{background:white;padding:24px;margin:18px 0;border-radius:14px}h1{font-size:32px}summary{cursor:pointer}
                img{max-width:100%;border:1px solid #ddd}small{color:#52657a}.alert{border-left:6px solid #c65d00}b{color:#154c79}
                </style><main><header><h1>Can the shopping cart recover safely?</h1>
                <p>A controlled comparison of ordinary Selenium and reviewed replacement locators.</p>
                <p>Verified recovery means the expected cart page opened. Finding a clickable element alone is insufficient.</p></header>
                """);
        html.append("<article class='alert'><h2>Decision summary</h2><p>").append(healed)
                .append(" verified recoveries · ").append(falseHeals).append(" incorrect recoveries · precision ")
                .append(precision).append("</p><p>Precision = correct recoveries ÷ all applied replacements. ")
                .append("The wrong-target case deliberately exposes a missing destination guard. Do not approve automatic reuse based on element availability.</p>")
                .append("<small>This is a small synthetic benchmark, not evidence of production accuracy. A passing test suite means labelled outcomes were reproduced.</small></article>");
        String advice = falseHeals > 0
                ? "Keep automatic reuse restricted. A replacement opened the wrong page; check its destination and role before clicking, then verify the expected page."
                : applied == 0 ? "Recovery accuracy is not measured: no replacements were applied. Add recoverable changes before assessing readiness."
                : "No incorrect recovery was observed in these cases. This small sample does not establish production safety; broaden the benchmark before rollout.";
        String guidance = "<h2>What should we do next?</h2><p><b>Stakeholder decision:</b> " + advice + "</p>"
                + "<p><b>Tech lead priority:</b> Add a cart-specific destination and element-role check, and keep checks for duplicate, hidden and disabled elements. "
                + "Retest the wrong-destination case: it should be rejected before clicking while the changed-class case should still recover.</p>"
                + "<p><b>Architect priority:</b> Keep candidate selection, safety rules and business verification separate. "
                + "Record why each candidate was accepted or rejected. Reuse only replacements backed by verified business outcomes.</p>"
                + "<p><b>Research improvement:</b> Add more independent mutations and repeat runs. Measure correct recovery, false healing, "
                + "blocked recoverable cases and time. Do not raise precision by simply rejecting every candidate; safe useful recovery is the goal.</p>"
                + "<p>These are rule-based recommendations from observed benchmark outcomes, not model-generated explanations. "
                + "The checks above are proposed improvements; this report does not implement them or guarantee a future score.</p>";
        int endSummary = html.indexOf("</article>");
        html.insert(endSummary, guidance);
        for (Result r : results) {
            html.append("<article><h2>").append(escape(r.scenario)).append(" · ").append(escape(r.mode))
                    .append("</h2><p><b>").append(escape(r.outcome)).append("</b></p><p>").append(escape(r.reason))
                    .append("</p><small>Elapsed ").append(r.millis).append(" ms (navigation, lookup, action, verification and screenshot; excludes browser startup).</small>");
            if (!r.screenshot.isEmpty()) html.append("<details><summary>See the browser evidence</summary><img alt='Final browser state' src='data:image/png;base64,").append(r.screenshot).append("'></details>");
            html.append("</article>");
        }
        html.append("</main></html>");
        Path destination = Path.of("AutomationReports", "benchmark", "locator-benchmark-" + utility.timeStamp() + ".html");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, html, StandardCharsets.UTF_8);
        System.out.println("Open benchmark report: " + destination.toAbsolutePath());
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
