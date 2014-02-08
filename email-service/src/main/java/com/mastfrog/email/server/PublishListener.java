package com.mastfrog.email.server;

/**
 * Can be notified of progress of sending an email.  Has no notion of bounces,
 * just steps in trying to get something out the door to an SMTP server.
 *
 * @author Tim Boudreau
 */
public interface PublishListener<T> {

    public void progress(int i, int steps, String string, T email);

    public void onSuccess(T email);

    public void onFailure(Throwable failure, T message);
}
