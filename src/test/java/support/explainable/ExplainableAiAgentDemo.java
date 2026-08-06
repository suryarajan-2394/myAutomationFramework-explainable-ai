package support.explainable;

import java.nio.file.Path;
import java.time.Duration;

/** Dependency-free demonstration using Sauce Demo's Add to Cart business flow. */
public final class ExplainableAiAgentDemo {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "demo-output" : args[0]);

        ExplainableAiAgent.Run pass = ExplainableAiAgent.begin("AddToCartTestCase");
        pass.record("Open Sauce Demo", "Login page is displayed", "Login page displayed", ExplainableAiAgent.Outcome.PASS, 422);
        pass.record("Log in as standard_user", "Inventory page is displayed", "Inventory page displayed", ExplainableAiAgent.Outcome.PASS, 681);
        pass.record("Add Sauce Labs Backpack", "Cart badge is 1", "Cart badge is 1", ExplainableAiAgent.Outcome.PASS, 214);
        pass.record("Validate cart", "Sauce Labs Backpack is in cart", "Item is present in cart", ExplainableAiAgent.Outcome.PASS, 176);
        ExplainableAiAgent.Report passReport = pass.complete(ExplainableAiAgent.Outcome.PASS, null);
        passReport.writeHtml(output.resolve("add-to-cart-explanation.html"));
        passReport.writeJson(output.resolve("add-to-cart-explanation.json"));

        ExplainableAiAgent.Run failure = ExplainableAiAgent.begin("CheckoutValidationExample");
        failure.record("Log in as standard_user", "Inventory page is displayed", "Inventory page displayed", ExplainableAiAgent.Outcome.PASS, 610);
        failure.record("Open checkout", "Checkout form is displayed", "Timeout waiting for element: [data-test='firstName']", ExplainableAiAgent.Outcome.FAIL, Duration.ofSeconds(10).toMillis(), "screenshots/checkout-timeout.png");
        ExplainableAiAgent.Report failReport = failure.complete(ExplainableAiAgent.Outcome.FAIL, new RuntimeException("Timeout waiting for checkout form"));
        failReport.writeHtml(output.resolve("checkout-failure-explanation.html"));
        failReport.writeJson(output.resolve("checkout-failure-explanation.json"));

        System.out.println(passReport.explain().summary());
        System.out.println(failReport.explain().summary());
    }
}
