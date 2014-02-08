package com.mastfrog.email.server;

import com.mastfrog.url.Host;
import com.mastfrog.util.Checks;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.validation.api.InvalidInputException;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validating;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.util.NbBundle;

/**
 * Represents an email address.  Supports the full spec of email addresses;
 * equality tests, for practial purposes, use the address part as lower
 * case (RFC 2821 specifies that mailboxes can be case sensitive and
 * case should be <i>preserved</i> - in practice, no mail server treats
 * mailbox names as case sensitive).
 * <p/>
 * Searches that need to match an email address should normalize email
 * addresses using:
 * <pre>
 * EmailAddress addr = new EmailAddress(string);
 * if (addr.isValid()) { //no sense to search for a bad address - we should have none
 *    String searchString = addr.getAddress();
 * }
 * </pre>
 * This will get a normalized email address for maximal matching.
 * <p/>
 * <code>toString()</code> returns the original, unmodified email address as
 * passed to the constructor.
 *
 * @author Tim Boudreau
 */
public final class EmailAddress implements Validating, Comparable<EmailAddress> {
    private final String address;

    public EmailAddress(String address) {
        this (address, false);
    }

    public EmailAddress(String namePart, String addressPart) {
        this (namePart + "<" + addressPart + ">", false);
    }

    /**
     * Create an email address, and immediately validate it, throwing an
     * InvalidInputException w/ a localized message if something is wrong
     * with it.
     *
     * @param address An email address
     * @param failFast If true, an exception will be thrown on invalid
     * email addresses
     */
    public EmailAddress(String address, boolean failFast) {
        Checks.notNull("address", address);
        this.address = address.trim();
        if (failFast) {
            Problems p = getProblems();
            if (p != null && p.hasFatal()) {
                throw new InvalidInputException ("Bad email address " + address, p);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EmailAddress && getAddressPart().equals(
                ((EmailAddress) obj).getAddressPart());
    }

    @Override
    public int hashCode() {
        return getAddressPart().hashCode();
    }

    @Override
    public String toString() {
        return address;
    }

    public Host getHost() {
        String[] s = getAddressPart().split("@");
        if (s.length == 2) {
            return Host.parse(s[1].toLowerCase());
        }
        return null;
    }

    private String getNamePart() {
        String[] s = getAddressPart().split("@");
        return s[0];
    }

    public Host getDomain() {
        Host h = getHost();
        return h == null ? null : h.size() > 1 ? 
            Host.builder().add(h.getTopLevelDomain()).add(h.getDomain()).create() : null;
    }

    /**
     * Get the personal name portion of this email address, i.e. if it is
     * &quot;Tim Boudreau &lt;tim&#064;foo.com&gt;&quot;, returns
     * &quot;Tim Boudreau&quot;
     * @return A name or null if none is present
     */
    public String getPersonalName() {
        Matcher m = ADDRESS_PATTERN.matcher(address);
        if (m.find()) {
            String name = m.group(1).trim();
            if (name != null && name.length() > 1) {
                if (name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
                    name = name.substring (1, name.length() - 1);
                }
                return name;
            }
        } else {
            return getNamePart();
        }
        return null;
    }

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(.*?)<(.*)>$"); //NOI18N
    private String addressPart;
    /**
     * Get the address only portion of an email address.  I.e., if
     * you have &quot;Tim Boudreau &lt;tim&#064;foo.com&gt;&quot; then
     * &quot;tim&#064;foo.com&quot; will be returned.
     * @return The email address portion only
     */
    public synchronized String getAddressPart() {
        //frequently called, and profiling shows that at least for tests,
        //recomputing this is a bottleneck (due to its use in equals()).
        if (addressPart == null) {
            Matcher m = ADDRESS_PATTERN.matcher(address);
            if (m.find()) {
                //technically, the user name portion of the address is
                //case sensitive and the domain is not.  In practice, no
                //mail server except perhaps very old unix inboxes
                //actually enforce case sensitivity.  Since we prefer to
                //err on the side of matching more values, we lower-case it
                addressPart = m.group(2).toLowerCase().trim();
            } else {
                addressPart = address.toLowerCase();
            }
        }
        return addressPart;
    }

    /**
     * Get any problems with this address (i.e. validation errors).  All
     * Problems will have localized messages.
     * @return A list of problems, or null if there are no problems.
     */
    @Override
    public Problems getProblems() {
        Problems p = new Problems();
        StringValidators.EMAIL_ADDRESS.validate(p, 
                NbBundle.getMessage(EmailAddress.class, "EMAIL_ADDRESS"), address);
        return p;
    }

    @Override
    public boolean isValid() {
        return getProblems() == null;
    }

    @Override
    public int compareTo(EmailAddress o) {
        return getAddressPart().compareToIgnoreCase(o.getAddressPart());
    }
}
