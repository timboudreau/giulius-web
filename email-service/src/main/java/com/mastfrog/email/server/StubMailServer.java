package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

/**
 * Actual implementation of EmailServerService which can talk to a real mail
 * server.
 *
 * @author Tim Boudreau
 */
class StubMailServer implements EmailServerService {

    final EmailAddressesConfig addressConfig; //package private for tests
    // Number of Steps before sending an Email
    // 1 - Queued/Unable to Queue. Limit has reached.
    // 2 - Delivering
    // 3 - Delivered/Delivery Failed
    public static final int STEPS = 3;
    // Initial Buffer for the Queue
    public static final int QUEUE_BUFFER = 1000;
    // The Queue Thread
    private EmailSender runner;

    @Inject
    public StubMailServer(EmailAddressesConfig config, @Named("mailqueue") ExecutorService threadPool, Settings settings, ShutdownHookRegistry vmShutdown) {
        if (Dependencies.isProductionMode(settings) && this.getClass() == StubMailServer.class) {
            throw new Error("Will not use a mail server which does not actually send mail in production mode");
        }
        this.addressConfig = config;
        // Start the Thread
        runner = new EmailSender();
        threadPool.execute(runner);
        vmShutdown.add(new Shutdown());
    }

    final class Shutdown implements Runnable {

        @Override
        public void run() {
            StubMailServer.this.shutdown();
        }
    }

    /**
     * End the Queue, by waiting the remaining Emails to be send before finally
     * shuting down
     */
    public void shutdown() {
        runner.shutdown();
    }

    @Override
    public <E extends Email> void send(E email, PublishListener<E> listener) throws QueueFullException {
        if (runner.isShutdown()) {
            throw new IllegalStateException("Already shut down");
        }
        // Add the Email to the Queue

        final EmailAndListener<E> emailAndListener = new EmailAndListener<E>(email, listener);
        try {
            runner.add(emailAndListener);
            // If there is a listener, then set some info
        } catch (QueueFullException e) {
            // If Queue is Full - should never happen in practice
            LOGGER.log(Level.SEVERE, "RealMailServer.send(): Queue is FULL.");
        }

        //1. set up the outgoing server, user name, password if
        //necessary to use SMTP
        //2. check if config.getRecipientRedirectionAddress() is non-null.
        //If so, replace the To: address with that and remove any CC addresses,
        //so that unit tests do not send real email
        //3. Send the email.  See what commons-email actually does.  If
        //email.send() is a synchronous call, use
        //java.util.concurrent.ExecutionService.  Create a background thread and
        //do it there (probably hold the ExecutionService statically and give it 1 thread for now)
        //4. If you can figure out a way to, have the background thread call the
        //PostListener (which may be null) on success or failure
    }

    @Override
    public String getDefaultFromAddress() {
        return addressConfig.getDefaultSender().toString();
    }

    protected void prepareToSend(Email email) throws EmailException {
    }

    protected void reallySend(Email email) throws EmailException {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Send email {0}", email);
    }

    /**
     * In order to control the set of emails to be send, this Queue Class will
     * control de flow of Emails to be send. Eventually, some of sort of
     * Management Mechanism could be attach in here and provide features such
     * as: 1. Prioritize 2. Cancel (before gets send)
     */
    private final class EmailSender implements Runnable {

        // A reference of a Thread, so we can do some locking
        private Thread thread;
        // A reference of a Collection, containing all the Emails
        // Queue containing all the Emails to be send
        // [PENDING: A alternative will be to persist somewhere, instead of having
        //           everything in Memory]
        private ArrayBlockingQueue<EmailAndListener<? extends Email>> emails;

        public EmailSender() {
            emails = new ArrayBlockingQueue<>(QUEUE_BUFFER);
        }

        private volatile boolean shutdown;

        boolean isShutdown() {
            return shutdown;
        }
        
        private <E extends Email> void doOne(EmailAndListener<E> emailAndListener) {
            PublishListener<E> listener = emailAndListener.getListener();
            E email = emailAndListener.getEmail();
            sendOneEmail(email, listener);
            if (Thread.interrupted()) {
                List<EmailAndListener<?>> remaining = new ArrayList<EmailAndListener<?>>(emails);
                for (EmailAndListener<?> rem : remaining) {
                    sendOneEmail(rem);
                }
                return;
            }
        }
        
        private <E extends Email> void sendOneEmail(EmailAndListener<E> l) {
            sendOneEmail(l.email, l.listener);
        }

        @Override
        public void run() {
            try {
                LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): Delivery Email Queue STARTED");
                synchronized (this) {
                    thread = Thread.currentThread();
                }
                EmailAndListener<?> emailAndListener = null;
                try {
                    while ((emailAndListener = emails.take()) != null) {
                        doOne(emailAndListener);
                    }
                } catch (InterruptedException e) {
                    // IT SHOULD BE EXPECT a InterruptException,
                    // when the shutdown() method is called (the only case)
                    LOGGER.log(Level.WARNING, "RealMailServer.Queue.run(): INTERRUPTED EXCEPTION: {0}", e.getMessage());

                } catch (IllegalMonitorStateException e) {
                    LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): ILLEGAL MONITOR EXCEPTION: {0}", e.getMessage());
                }
                LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): Delivery Email Queue ENDED");
            } finally {
                shutdown = true;
            }
        }

        private <E extends Email> void sendOneEmail(E email, PublishListener<E> listener) {
            // SEND IT
            LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): Sending Email to {0}", email.getToAddresses());
            try {
                prepareToSend(email);

                if (listener != null) {
                    try {
                        listener.progress(1, STEPS, "Delivering", email);
                    } catch (Exception e) {
                        Logger.getLogger(EmailSender.class.getName()).log(Level.INFO, "Exception in send listener", e);
                    }
                }
                reallySend(email);
                if (listener != null) {
                    try {
                        listener.progress(2, STEPS, "Delivered", email);
                        listener.onSuccess(email);
                    } catch (Exception e) {
                        Logger.getLogger(EmailSender.class.getName()).log(Level.INFO, "Exception in send listener", e);
                    }
                }
                LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): Delivery SUCESSFUL {0}", new Object[]{email});

                // EMAIL FAILURE: It might be due several reasons
                // [PENDING: Try to come up with clear ways to handle failure]
            } catch (EmailException e) {
                LOGGER.log(Level.INFO, "RealMailServer.Queue.run(): Email Exception: {0}", e.getMessage());
                if (listener != null) {
                    try {
                        listener.progress(2, STEPS, "Delivery Failed", email);
                        listener.onFailure(e.getCause() != null ? e.getCause() : e, email);
                    } catch (Exception e1) {
                        Logger.getLogger(EmailSender.class.getName()).log(Level.INFO, "Exception in send listener", e1);

                    }
                }
            }
        }

        /**
         * If possible, get the Email out the queue
         */
        public boolean cancel(EmailAndListener<?> emailAndListener) {
            return emails.remove(emailAndListener);
        }

        /**
         * Add an Email to the Queue
         */
        public <E extends Email> void add(EmailAndListener<E> emailAndListener) throws QueueFullException {
            try {
                emails.add(emailAndListener);
            } catch (IllegalStateException e) {
                // there isn't another room for more Emails
                // because the Queue is full
                throw new QueueFullException("Email Queue is full. Limit is " + String.valueOf(QUEUE_BUFFER), e.getCause());
            }
        }

        /**
         * Flag the Thread that it should shutdown, as soon as all the Emails
         * are done being sent
         */
        public void shutdown() {
            synchronized (this) {
                if (thread != null) {
                    thread.interrupt();
                }
            }
        }
    }

    /**
     * This class bundles 2 basic information: Email and PublishListener Once
     * bundled, it's inserted into the Queue, so QueueThread can handle both
     * Email and a (optional) PublishListener
     */
    private final class EmailAndListener<E extends Email> {

        private final E email;
        private final PublishListener<E> listener;

        public EmailAndListener(E email, PublishListener<E> listener) {
            this.email = email;
            this.listener = listener;
        }

        public E getEmail() {
            return email;
        }

        public PublishListener<E> getListener() {
            return listener;
        }
    }
}
