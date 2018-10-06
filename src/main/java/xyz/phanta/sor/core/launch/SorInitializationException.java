package xyz.phanta.sor.core.launch;

public class SorInitializationException extends Exception {

    public SorInitializationException(String reason) {
        super(reason);
    }

    public SorInitializationException(Throwable cause) {
        super(cause);
    }

    public Wrapped wrap() {
        return new Wrapped(this);
    }

    public static class Wrapped extends RuntimeException {

        private final SorInitializationException value;

        private Wrapped(SorInitializationException value) {
            this.value = value;
        }

        public SorInitializationException unwrap() {
            return value;
        }

    }

}
