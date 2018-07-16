/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.statistics;

import com.mastfrog.util.perf.Benchmark;
import com.mastfrog.util.preconditions.Checks;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A handy class to parse back into human readable, base 10 messages, the UDP
 * packets we generate
 *
 * @author Tim Boudreau
 */
public class UDPMessage {
    private static final Pattern PAT = Pattern.compile("(.*):(.)/(.*?)");
    private String name;
    private Benchmark.Kind kind;
    private long[] values;

    public UDPMessage(byte[] bytes) {
        this(new String(bytes, UDPBroadcaster.ascii));
    }

    public UDPMessage(String name, Benchmark.Kind kind, long... values) {
        Checks.notNull("name", name);
        Checks.notNull("kind", kind);
        Checks.notNull("values", values);
        this.name = name;
        this.kind = kind;
        this.values = values;
    }

    public UDPMessage(String string) {
        Matcher m = PAT.matcher(string);
        if (m.matches()) {
            String vals = m.group(1);
            String kind = m.group(2);
            String msg = m.group(3);
            String[] longStrings = vals.split(":");
            values = new long[longStrings.length];
            for (int i = 0; i < longStrings.length; i++) {
                values[i] = Long.parseLong(longStrings[i], 36);
            }
            this.name = msg;
            for (Benchmark.Kind k : Benchmark.Kind.values()) {
                if (kind.equals(k.toString())) {
                    this.kind = k;
                    break;
                }
            }
            this.name = msg;
        }
    }

    public byte[] toByteArray() {
        String stat = kind + "/" + name;
        return toMessage(stat, values);
    }
    
    public String toStringForm() {
        return new String(toByteArray(), UDPBroadcaster.ascii);
    }

    private byte[] toMessage(String stat, long... values) {
        Checks.mayNotContain("stat", stat, ':', '/', '|');
        if (stat.length() > 120) {
            throw new IllegalArgumentException("Stat length too long: '" + stat + "'");
        }
        StringBuilder sb = new StringBuilder(80);
        for (int i = 0; i < values.length; i++) {
            sb.append(Long.toString(values[i], 36));
            if (i < values.length - 1) {
                sb.append(':');
            }
        }
        sb.append(':').append(stat);
        return sb.toString().getBytes(UDPBroadcaster.ascii);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(kind == null ? "" : kind.name()).append(':').append(name).append(" ");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sb.append(values[i]);
                if (i != values.length - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    public static UDPMessage[] parse(byte[] bytes) {
        String[] all = new String(bytes, UDPBroadcaster.ascii).split("\\|");
        UDPMessage[] result = new UDPMessage[all.length];
        for (int i = 0; i < all.length; i++) {
            result[i] = new UDPMessage(all[i]);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UDPMessage other = (UDPMessage) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.kind != other.kind) {
            return false;
        }
        if (!Arrays.equals(this.values, other.values)) {
            return false;
        }
        return true;
    }
}
