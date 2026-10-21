package support.explainable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** One identity per Maven test JVM. Supply explicit properties when separate processes share one run. */
final class RunIdentity {
    static final String RUN = System.getProperty("framework.reporting.runId", UUID.randomUUID().toString());
    static final String COMMIT = revision();
    private RunIdentity() { }
    static String attributes() { return " data-run='" + escape(RUN) + "' data-commit='" + escape(COMMIT) + "'"; }
    static boolean matches(String html) {
        return html.contains("data-run='" + escape(RUN) + "'") && html.contains("data-commit='" + escape(COMMIT) + "'");
    }
    static String card() {
        return "<section class='card'><h2>Evidence identity</h2><p>Run: " + escape(RUN)
                + "</p><p>Code revision: " + escape(COMMIT) + "</p></section>";
    }
    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("'", "&#39;").replace("\"", "&quot;");
    }
    private static String revision() {
        String supplied = System.getProperty("framework.reporting.commit");
        if (supplied != null && !supplied.isBlank()) return supplied;
        try {
            String sha = git("rev-parse", "HEAD").strip();
            if (sha.matches("[a-fA-F0-9]{40,64}")) return sha + (git("status", "--porcelain").isBlank() ? "" : "-dirty");
        } catch (Exception ignored) { /* explicitly mark unavailable provenance */ }
        return "UNKNOWN (Git unavailable; do not claim revision verification)";
    }
    private static String git(String... args) throws Exception {
        String[] command = new String[args.length + 1]; command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Git timed out"); }
        if (process.exitValue() != 0) throw new IllegalStateException("Git failed");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
