/**
 * When eliminating redundant statements, field access expressions should be preserved.
 *
 * See https://github.com/plilab/jade/pull/13 for context.
 */
public class FieldAccessExpressionShouldNotBeEliminated {
    private int value;

    void increment(int delta) {
        value += delta;
    }
}
