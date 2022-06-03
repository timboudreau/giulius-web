package com.mastfrog.email.server;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.email.EmailAddress;

final class SettingsBasedMailServerConfig implements EmailAddressesConfig {

    @Named(EmailServerService.EMAIL_REDIRECT_SETTINGS_KEY)
    @Inject(optional = true)
    String redirectAddress;
    
    @Named(EmailServerService.EMAIL_BOUNCE_ADDRESS_SETTINGS_KEY)
    @Inject(optional = true)
    String bounceAddress;

    private final String defaultSender;
    
    @Inject
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    SettingsBasedMailServerConfig(@Named(EmailServerService.DEFAULT_FROM_EMAIL_ADDRESS_SETTINGS_KEY) String defaultSender) {
        this.defaultSender = defaultSender;
        getDefaultSender().getProblems().throwIfFatalPresent();
        if (getBounceAddress() != null) {
            getBounceAddress().getProblems().throwIfFatalPresent();
        }
        if (getRecipientRedirectionAddress() != null) {
            getRecipientRedirectionAddress().getProblems().throwIfFatalPresent();
        }
    }

    @Override
    public EmailAddress getDefaultSender() {
        return new EmailAddress(defaultSender);
    }

    @Override
    public EmailAddress getRecipientRedirectionAddress() {
        return redirectAddress == null ? null : new EmailAddress(redirectAddress);
    }

    @Override
    public EmailAddress getBounceAddress() {
        return bounceAddress == null ? null : new EmailAddress(bounceAddress);
    }
}
