package com.mastfrog.email.server;

/**
 * Indicates that the Email Queue has reached its limits.  In practice, should
 * never be thrown - see javadoc for ArrayBlockingQueue.offer() and its one
 * unlikely failure mode for why this exists at all.
 * 
 * @author Tim Boudreau
 */
public class QueueFullException extends RuntimeException {

    public QueueFullException(Throwable cause) {
        super(cause);
    }

    public QueueFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueueFullException(String message) {
        super(message);
    }

    public QueueFullException() {
    }
}
