package sm.selflearn.samskrtam.samcli.io;

import java.util.regex.Pattern;

/**
 * Validates untrusted schema/table names coming from config or CLI before they
 * are embedded into SQL identifier positions (which cannot be bound as
 * parameters). Only safe PostgreSQL identifiers are allowed.
 */
public final class IdentifierValidator {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private IdentifierValidator() {
    }

    public static String requireValid(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid SQL identifier (allowed: letters, digits, underscore, must not start with a digit): "
                            + value);
        }
        return value;
    }
}
