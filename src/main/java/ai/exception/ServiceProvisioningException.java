package ai.exception;

public class ServiceProvisioningException extends RuntimeException {

    public ServiceProvisioningException(String message) {
        super(message);
    }

    public ServiceProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
