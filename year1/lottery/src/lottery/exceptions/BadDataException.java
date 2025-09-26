package lottery.exceptions;

public class BadDataException extends IllegalArgumentException {
    public BadDataException(String message) {
        super(message);
    }
}
