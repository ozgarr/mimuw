package lottery.utility;

public final class Formatter {

    private Formatter() {
        throw new AssertionError("Utility class, should not be instantiated.");
    }

    public static String centsToString(long centCount) {
        long dollars = centCount / 100;
        long cents = centCount % 100;
        return String.format("$%d.%02d", dollars, cents);
    }
}