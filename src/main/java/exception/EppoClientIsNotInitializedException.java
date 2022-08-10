package exception;

public class EppoClientIsNotInitializedException extends RuntimeException {
    public EppoClientIsNotInitializedException(String message) {
        super(message);
    }
}
