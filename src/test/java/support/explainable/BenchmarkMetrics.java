package support.explainable;

import java.util.List;
import java.util.Locale;

/** Applied replacements remain in the denominator even if their execution/verification raises an error. */
final class BenchmarkMetrics {
    record Attempt(boolean healingMode, boolean replacementApplied, String outcome) { }
    record Summary(long applied, long correct, long falseHeals, long unverified, long errors, long blocked) {
        String precision() { return applied == 0 ? "Not measured" : String.format(Locale.ROOT, "%.1f%%", 100.0 * correct / applied); }
    }
    static Summary summarize(List<Attempt> attempts) {
        long applied = attempts.stream().filter(a -> a.healingMode && a.replacementApplied).count();
        long correct = attempts.stream().filter(a -> a.healingMode && a.replacementApplied && a.outcome.equals("VERIFIED RECOVERY")).count();
        long falseHeals = attempts.stream().filter(a -> a.healingMode && a.replacementApplied && a.outcome.equals("FALSE HEAL")).count();
        return new Summary(applied, correct, falseHeals, applied - correct - falseHeals,
                attempts.stream().filter(a -> a.outcome.equals("EXECUTION ERROR")).count(),
                attempts.stream().filter(a -> a.healingMode && !a.replacementApplied && a.outcome.equals("BLOCKED SAFELY")).count());
    }
}
