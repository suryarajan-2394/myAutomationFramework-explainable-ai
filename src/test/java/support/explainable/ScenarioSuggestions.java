package support.explainable;

/** Evidence-triggered proposals only: no model call or executable-test generation. */
final class ScenarioSuggestions {
    static String html(BenchmarkMetrics.Summary metrics) {
        StringBuilder html = new StringBuilder("<h2>Suggested next scenarios (rule-based; review required)</h2><ul>");
        if (metrics.falseHeals() > 0) html.append("<li>Observed incorrect recovery: add JavaScript redirect and wrong-result assertions. Expected: the wrong page must fail business verification; do not approve the replacement.</li>");
        if (metrics.missed() > 0) html.append("<li>Observed missed recoverable cases: review changed stable attributes. Expected: recover only after independently confirming product/page identity; preserve ambiguity rejection.</li>");
        if (metrics.errors() > 0) html.append("<li>Execution errors: investigate browser/driver availability before comparing healing algorithms.</li>");
        html.append("<li>Add product-container mutations: another product already selected, missing intended button, and duplicate product names. Expected: never click another product's control.</li>")
            .append("</ul><p>These are proposed requirements for a human reviewer. They do not modify tests or make an AI approval decision.</p>");
        return html.toString();
    }
}
