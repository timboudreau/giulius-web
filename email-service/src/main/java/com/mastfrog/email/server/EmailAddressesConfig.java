package com.mastfrog.email.server;

import com.google.inject.ImplementedBy;

/**
 * Email addresses involved in sending mail
 *
 * @author Tim Boudreau, Mauricio Leal */
@ImplementedBy(SettingsBasedMailServerConfig.class)
public interface EmailAddressesConfig {
    //PENDING:  Add methods needed for actually sending mail - get the
    //mail server, etc.
    
    //Should probably use an injected

    /**
     * Get the default From: address that should be used if sending email
     * from a user that we don't have an email address from.
     * @return An email address.  Should not be null.
     */
    public EmailAddress getDefaultSender();
    /**
     * Get the <i>redirection</i> address.  If this method returns non-null,
     * all emails the system sends should be sent from this address, not
     * whoever they are really to (so unit tests don't send real emails!).
     *
     * @return The address emails should go to *instead* of where the caller
     * thinks they should go
     */
    public EmailAddress getRecipientRedirectionAddress();
    
    /**
     * Get the address that bounced emails should be sent to.  This is the
     * address messages which bounce should be sent to.
     * 
     * @return An email address or null
     */
    public EmailAddress getBounceAddress();
}
