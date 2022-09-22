/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.jackson.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.jackson.configuration.impl.JavaTimeConfigurer;
import com.mastfrog.jackson.configuration.impl.LocaleJacksonConfigurer;
import com.mastfrog.jackson.configuration.impl.JavaOptionalSerializer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author timb
 */
public class JacksonConfigurerTest {

    @Test
    public void testMonthDay() throws JsonProcessingException {
        MonthDay md = MonthDay.of(2, 29);
        MonthDay nue = interconvert(MonthDay.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }
    
    @Test
    public void testYear() throws JsonProcessingException {
        Year md = Year.of(2011);
        Year nue = interconvert(Year.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }
    

    @Test
    public void testLocalDate() throws JsonProcessingException {
        LocalDate md = LocalDate.of(2004, 2, 29);
        LocalDate nue = interconvert(LocalDate.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }

    @Test
    public void testLocalTime() throws JsonProcessingException {
        LocalTime md = LocalTime.of(14, 29, 23);
        LocalTime nue = interconvert(LocalTime.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }
    
    @Test
    public void testYearMonth() throws JsonProcessingException {
        YearMonth md = YearMonth.of(2007, 7);
        YearMonth nue = interconvert(YearMonth.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }
    
    @Test
    public void testCharset() throws JsonProcessingException {
        Charset md = UTF_8;
        Charset nue = interconvert(Charset.class, md, JacksonConfigurer.javaTimeConfigurer());
        System.out.println("MD " + md);
        System.out.println("Reconstituted " + nue);
        assertEquals(md, nue);
    }

    private <T> T interconvert(Class<T> type, T value, JacksonConfigurer cf) throws JsonProcessingException {
        System.out.println("\n" + type.getSimpleName());
        ObjectMapper mapper = cf.configure(new ObjectMapper());
        String val = mapper.writeValueAsString(value);
        System.out.println("Written to " + val);
        class TR extends TypeReference<T> {

            public TR() {
            }

            @Override
            public Type getType() {
                return type;
            }
        }
        TR tr = new TR();
        T re = mapper.readValue(val, tr);
        return re;
    }

    @Test
    public void testMetaInfServicesTypesAreFound() {
        Set<Class<?>> types = new HashSet<>();
        JacksonConfigurer.metaInfServices()
                .forEach(item -> types.add(item.getClass()));

        assertFalse(types.isEmpty(), "Got no configurers");

        assertTrue(types.containsAll(Arrays.asList(JavaTimeConfigurer.class,
                LocaleJacksonConfigurer.class, JavaOptionalSerializer.class)),
                types::toString);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLocalesAreSerializedCorrectly() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfigurer.configureFromMetaInfServices(new ObjectMapper());
        Map<String, Object> mo = new HashMap<>();
        mo.put("us", Locale.US);

        String s = mapper.writeValueAsString(mo);
        System.out.println("US is " + s);

        Map<String, Object> recovered = mapper.readValue(s, Map.class);

        assertEquals("en-US", recovered.get("us"));

    }

}
