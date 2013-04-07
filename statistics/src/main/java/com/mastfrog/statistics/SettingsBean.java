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

import com.google.inject.Provider;
import com.mastfrog.settings.MutableSettings;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

/**
 * MBean for Settings
 *
 * @author Tim Boudreau
 */
public class SettingsBean implements javax.management.DynamicMBean {
    private final Provider<Settings> settings;

    public SettingsBean(Provider<Settings> settings) {
        this.settings = settings;
    }

    @Override
    public Object getAttribute(String key) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (!settings.get().allKeys().contains(key)) {
            throw new AttributeNotFoundException(key);
        }
        return settings.get().getString(key);
    }

    @Override
    public void setAttribute(Attribute atrbt) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        if (settings.get() instanceof MutableSettings) {
            MutableSettings m = (MutableSettings) settings.get();
            if (atrbt.getValue() == null) {
                m.clear(atrbt.getName());
            } else {
                m.setString(atrbt.getName(), atrbt.getValue() + "");
            }
        }
    }

    @Override
    public AttributeList getAttributes(String[] strings) {
        AttributeList l = new AttributeList();
        List<String> keys = new ArrayList<>(settings.get().allKeys());
        Collections.sort(keys);
        for (String key : keys) {
            Attribute attr = new Attribute(key, settings.get().getString(key));

            l.add(attr);
        }
        return l;
    }

    @Override
    public AttributeList setAttributes(AttributeList al) {
        throw new UnsupportedOperationException("Good luck with that.");
    }

    @Override
    public Object invoke(String string, Object[] os, String[] strings) throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException("Nothing to invoke");
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        Settings s;
        try {
            s = settings.get();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            try {
                s = new SettingsBuilder().build();
            } catch (IOException ex1) {
                throw new AssertionError(ex1); //won't happen, we do no I/O
            }
        }
        List<MBeanAttributeInfo> infos = new ArrayList<>();
        List<String> keys = new ArrayList<>(s.allKeys());
        Collections.sort(keys);
        for (String key : keys) {
            infos.add(new MBeanAttributeInfo(key, "String", key, true, s instanceof MutableSettings, false));
        }
        return new MBeanInfo(SettingsBean.class.getName(), "Settings", infos.toArray(new MBeanAttributeInfo[infos.size()]), new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
    }
}
