package com.towerdefence.engine.session;

/**
 * A tiny expression language for the auto-prestige rule, so the player can write their own
 * condition instead of picking from fixed triggers. Booleans are doubles: anything non-zero is true.
 *
 * <pre>
 * expr    := or
 * or      := and ( "||" and )*
 * and     := cmp ( "&amp;&amp;" cmp )*
 * cmp     := sum ( ("&lt;" | "&lt;=" | "&gt;" | "&gt;=" | "==" | "!=") sum )?
 * sum     := term ( ("+" | "-") term )*
 * term    := unary ( ("*" | "/") unary )*
 * unary   := ("!" | "-") unary | primary
 * primary := number | identifier | "(" expr ")"
 * </pre>
 */
public final class PrestigeFormula {
    /** Named readings the formula can refer to. */
    public interface Variables {
        double value(String name);
    }

    public static final String[] VARIABLES = {
            "wave", "hp", "hpPct", "coins", "shards", "cores", "sigils", "stall", "tier", "runs"
    };

    private final String source;
    private int cursor;

    private PrestigeFormula(String source) {
        this.source = source;
    }

    /** Throws {@link IllegalArgumentException} with a readable message when the text is broken. */
    public static PrestigeFormula compile(String source) {
        PrestigeFormula formula = new PrestigeFormula(source == null ? "" : source);
        formula.checkSyntax();
        return formula;
    }

    public static boolean isKnownVariable(String name) {
        for (String known : VARIABLES) {
            if (known.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public String source() {
        return source;
    }

    public boolean evaluate(Variables variables) {
        cursor = 0;
        double result = or(variables);
        skipSpace();
        if (cursor < source.length()) {
            throw new IllegalArgumentException("unexpected '" + source.charAt(cursor) + "'");
        }
        return result != 0d;
    }

    /** Parses once against zeroed variables purely to surface syntax errors up front. */
    private void checkSyntax() {
        if (source.isBlank()) {
            throw new IllegalArgumentException("empty formula");
        }
        evaluate(name -> {
            if (!isKnownVariable(name)) {
                throw new IllegalArgumentException("unknown name '" + name + "'");
            }
            return 0d;
        });
    }

    private double or(Variables variables) {
        double left = and(variables);
        while (match("||")) {
            double right = and(variables);
            left = (left != 0d || right != 0d) ? 1d : 0d;
        }
        return left;
    }

    private double and(Variables variables) {
        double left = comparison(variables);
        while (match("&&")) {
            double right = comparison(variables);
            left = (left != 0d && right != 0d) ? 1d : 0d;
        }
        return left;
    }

    private double comparison(Variables variables) {
        double left = sum(variables);
        // Two-character operators must be tried first.
        if (match("<=")) {
            return left <= sum(variables) ? 1d : 0d;
        }
        if (match(">=")) {
            return left >= sum(variables) ? 1d : 0d;
        }
        if (match("==")) {
            return left == sum(variables) ? 1d : 0d;
        }
        if (match("!=")) {
            return left != sum(variables) ? 1d : 0d;
        }
        if (match("<")) {
            return left < sum(variables) ? 1d : 0d;
        }
        if (match(">")) {
            return left > sum(variables) ? 1d : 0d;
        }
        return left;
    }

    private double sum(Variables variables) {
        double left = term(variables);
        while (true) {
            if (match("+")) {
                left += term(variables);
            } else if (match("-")) {
                left -= term(variables);
            } else {
                return left;
            }
        }
    }

    private double term(Variables variables) {
        double left = unary(variables);
        while (true) {
            if (match("*")) {
                left *= unary(variables);
            } else if (match("/")) {
                double divisor = unary(variables);
                left = divisor == 0d ? 0d : left / divisor;
            } else {
                return left;
            }
        }
    }

    private double unary(Variables variables) {
        if (match("!")) {
            return unary(variables) == 0d ? 1d : 0d;
        }
        if (match("-")) {
            return -unary(variables);
        }
        return primary(variables);
    }

    private double primary(Variables variables) {
        skipSpace();
        if (cursor >= source.length()) {
            throw new IllegalArgumentException("formula ends too early");
        }
        if (match("(")) {
            double inner = or(variables);
            if (!match(")")) {
                throw new IllegalArgumentException("missing ')'");
            }
            return inner;
        }
        char c = source.charAt(cursor);
        if (Character.isDigit(c) || c == '.') {
            int start = cursor;
            while (cursor < source.length()
                    && (Character.isDigit(source.charAt(cursor)) || source.charAt(cursor) == '.')) {
                cursor++;
            }
            return Double.parseDouble(source.substring(start, cursor));
        }
        if (Character.isLetter(c)) {
            int start = cursor;
            while (cursor < source.length() && Character.isLetterOrDigit(source.charAt(cursor))) {
                cursor++;
            }
            return variables.value(source.substring(start, cursor));
        }
        throw new IllegalArgumentException("unexpected '" + c + "'");
    }

    private boolean match(String token) {
        skipSpace();
        if (source.startsWith(token, cursor)) {
            // Do not let "<" swallow the "<" of "<=" and friends.
            if (token.length() == 1 && "<>=!".indexOf(token.charAt(0)) >= 0
                    && cursor + 1 < source.length() && source.charAt(cursor + 1) == '=') {
                return false;
            }
            cursor += token.length();
            return true;
        }
        return false;
    }

    private void skipSpace() {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
    }
}
