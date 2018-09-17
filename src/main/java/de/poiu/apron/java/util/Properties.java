/*
 * Copyright (C) 2018 Marco Herrn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.poiu.apron.java.util;

import de.poiu.apron.Options;
import de.poiu.apron.PropertyFile;
import de.poiu.apron.entry.BasicEntry;
import de.poiu.apron.entry.Entry;
import de.poiu.apron.entry.PropertyEntry;
import de.poiu.apron.escaping.EscapeUtils;
import de.poiu.apron.io.PropertyFileWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * A wrapper around {@link PropertyFile} that extends {@link java.util.Properties} to be
 * used as a drop-in-replacement for <code>java.util.Properties</code>.
 *
 * @author mherrn
 */
public class Properties extends java.util.Properties {

  /////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private PropertyFile propertyFile= new PropertyFile();

  private final java.util.Properties defaults;


  /////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public Properties() {
    this(new java.util.Properties());
  }

  public Properties(final java.util.Properties defaults) {
    this.defaults= defaults;
  }

  public Properties(final PropertyFile propertyFile) {
    this();
    this.propertyFile= propertyFile;
  }

  public Properties(final PropertyFile propertyFile, final java.util.Properties defaults) {
    this(defaults);
    this.propertyFile= propertyFile;
  }


  /////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * This method is not implemented in {@link PropertyFile} and therefore is just
   * delegated to the usual <code>java.util.Properties</code> implementation.
   *
   * @see java.util.Properties#loadFromXML(java.io.InputStream)
   */
  @Override
  public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
    super.loadFromXML(in);
  }


  /**
   * This method loads properties from an input stream and always assumes that the
   * properties are stored with an ISO 8859-1 character encoding.
   *
   * @param inStream
   * @throws IOException
   * @see java.util.Properties#load(java.io.InputStream)
   */
  @Override
  public synchronized void load(InputStream inStream) throws IOException {
    this.propertyFile= PropertyFile.from(inStream, ISO_8859_1);
  }


  /**
   * This method loads properties from an input stream and always assumes that the
   * properties are stored with an UTF-8 character encoding.
   * <p>
   * Be aware that this differs from {@link Properties}!
   *
   * @param inStream
   * @throws IOException
   * @see java.util.Properties#load(java.io.InputStream)
   * @param reader
   * @throws IOException
   */
  @Override
  public synchronized void load(final Reader reader) throws IOException {
    final char[] charBuffer = new char[8 * 1024];
    final StringBuilder builder= new StringBuilder();
    int numCharsRead;
    while ((numCharsRead = reader.read(charBuffer, 0, charBuffer.length)) != -1) {
        builder.append(charBuffer, 0, numCharsRead);
    }
    try (InputStream targetStream = new ByteArrayInputStream(builder.toString().getBytes(UTF_8))) {
      this.propertyFile= PropertyFile.from(targetStream, UTF_8);
      reader.close();
    }
  }


  @Override
  public Object getOrDefault(Object key, Object defaultValue) {
    if (this.propertyFile.containsKey((String) key)) {
      return this.propertyFile.get((String) key);
    } else {
      return defaultValue;
    }
  }


  @Override
  public Collection<Object> values() {
    return new ArrayList<>(this.propertyFile.values());
  }


  @Override
  public Set<Object> keySet() {
    return new LinkedHashSet<>(this.propertyFile.keys());
  }


  @Override
  public synchronized void clear() {
    this.propertyFile.getAllEntries().clear();
  }


  @Override
  public synchronized Object put(Object key, Object value) {
    final String previousValue= this.propertyFile.get((String) key);
    this.propertyFile.setValue((String) key, (String) value);
    return previousValue;
  }


  @Override
  public Object get(Object key) {
    return this.propertyFile.get((String) key);
  }


  @Override
  public boolean containsKey(Object key) {
    return this.propertyFile.containsKey((String) key);
  }


  @Override
  public boolean containsValue(Object value) {
    return this.propertyFile.values().contains(value);
  }


  @Override
  public boolean contains(Object value) {
    return this.containsValue(value);
  }


  @Override
  public Enumeration<Object> keys() {
    return Collections.enumeration(this.keySet());
  }


  @Override
  public boolean isEmpty() {
    return this.propertyFile.keys().isEmpty();
  }


  @Override
  public int size() {
    return this.propertyFile.propertiesSize();
  }


  @Override
  public String getProperty(String key) {
    if (this.propertyFile.containsKey(key)) {
      return this.propertyFile.get(key);
    } else {
      return this.defaults.getProperty(key);
    }
  }


  @Override
  public synchronized Object replace(Object key, Object value) {
    if (this.containsKey(key)) {
      return this.put(key, value);
    } else {
      return null;
    }
  }


  @Override
  public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
    if (this.containsKey(key) && Objects.equals(this.get(key), oldValue)) {
      this.put(key, newValue);
      return true;
    } else {
      return false;
    }
  }


  @Override
  public synchronized boolean remove(Object key, Object value) {
    if (this.containsKey(key) && Objects.equals(this.get(key), value)) {
      this.remove(key);
      return true;
    } else {
      return false;
    }
  }


  @Override
  public synchronized Object putIfAbsent(Object key, Object value) {
    Object v = this.get(key);
    if (v == null) {
      v = this.put(key, value);
    }

    return v;
  }


  @Override
  public synchronized void putAll(Map<?, ?> t) {
    t.forEach((k, v) -> this.put(k, v));
  }


  @Override
  public synchronized Object remove(Object key) {
    final String previousValue= this.propertyFile.get((String) key);
    this.propertyFile.remove((String) key);
    return previousValue;
  }


  @Override
  public Enumeration<Object> elements() {
    return Collections.enumeration(this.values());
  }


  @Override
  public synchronized Object setProperty(String key, String value) {
    final String previousValue= this.propertyFile.get(key);
    this.propertyFile.setValue(key, value);
    return previousValue;
  }


  @Override
  public Enumeration<?> propertyNames() {
    final Enumeration<?> defaultPropertyName= this.defaults.propertyNames();
    final HashSet<String> propertyNames= new HashSet<>();
    propertyNames.addAll(this.propertyFile.keys());
    while (defaultPropertyName.hasMoreElements()) {
      propertyNames.add((String) defaultPropertyName.nextElement());
    }
    return Collections.enumeration(propertyNames);
  }


  @Override
  public String getProperty(String key, String defaultValue) {
    if (this.propertyFile.containsKey(key)) {
      return this.propertyFile.get(key);
    } else {
      return this.defaults.getProperty(key, defaultValue);
    }
  }


  @Override
  public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
    this.propertyFile.toMap().forEach(action);
  }


  /**
   * Always throws an UnsupportetOperationException.
   *
   * @param key
   * @param remappingFunction
   * @return
   */
  @Override
  public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
    throw new UnsupportedOperationException("This operation is not supported at the moment");
  }


  /**
   * Always throws an UnsupportetOperationException.
   *
   * @param key
   * @param remappingFunction
   * @return
   */
  @Override
  public synchronized Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
    throw new UnsupportedOperationException("This operation is not supported at the moment");
  }


  /**
   * Always throws an UnsupportetOperationException.
   *
   * @param key
   * @param remappingFunction
   * @return
   */
  @Override
  public synchronized Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
    throw new UnsupportedOperationException("This operation is not supported at the moment");
  }


  /**
   * Always throws an UnsupportetOperationException.
   *
   * @param key
   * @param remappingFunction
   * @return
   */
  @Override
  public synchronized Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
    throw new UnsupportedOperationException("This operation is not supported at the moment");
  }


  /**
   * Always throws an UnsupportetOperationException.
   *
   * @param key
   * @param remappingFunction
   * @return
   */
  @Override
  public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
    throw new UnsupportedOperationException("This operation is not supported at the moment");
  }


  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    final Map<Object, Object> resultMap= new LinkedHashMap<>();
    this.propertyFile.toMap().forEach((k, v) -> resultMap.put(k, v));
    return resultMap.entrySet();
  }


  @Override
  public Set<String> stringPropertyNames() {
    final Set<String> propertyNames= new LinkedHashSet<>(this.defaults.stringPropertyNames());
    propertyNames.addAll(this.propertyFile.keys());
    return propertyNames;
  }


  @Override
  public void list(PrintWriter out) {
    out.println("-- listing properties --");
    final Map<String, String> h= new LinkedHashMap<>();
    this.defaults.forEach((k, v) -> h.put((String) k, (String) v));
    h.putAll(this.propertyFile.toMap());
    h.entrySet().forEach((e) -> {
      final String key = e.getKey();
      String val = e.getValue();
      if (val.length() > 40) {
        val = val.substring(0, 37) + "...";
      }
      out.println(key + "=" + val);
    });
  }


  @Override
  public void list(PrintStream out) {
    out.println("-- listing properties --");
    final Map<String, String> h= new LinkedHashMap<>();
    this.defaults.forEach((k, v) -> h.put((String) k, (String) v));
    h.putAll(this.propertyFile.toMap());
    h.entrySet().forEach((e) -> {
      final String key = e.getKey();
      String val = e.getValue();
      if (val.length() > 40) {
        val = val.substring(0, 37) + "...";
      }
      out.println(key + "=" + val);
    });
  }


  /**
   * This method is not implemented in {@link PropertyFile} and therefore is just
   * delegated to the usual <code>java.util.Properties</code> implementation.
   *
   * @see java.util.Properties#storeToXML(java.io.OutputStream, String, Charset)
   */
  //@Override
  public void storeToXML(OutputStream os, String comment, Charset charset) throws IOException {
    super.storeToXML(os, comment, charset.name());
  }


  /**
   * This method is not implemented in {@link PropertyFile} and therefore is just
   * delegated to the usual <code>java.util.Properties</code> implementation.
   *
   * @see java.util.Properties#storeToXML(java.io.OutputStream, String, String)
   */
  @Override
  public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
    super.storeToXML(os, comment, encoding);
  }


  /**
   * This method is not implemented in {@link PropertyFile} and therefore is just
   * delegated to the usual <code>java.util.Properties</code> implementation.
   *
   * @see java.util.Properties#storeToXML(java.io.OutputStream, String)
   */
  @Override
  public void storeToXML(OutputStream os, String comment) throws IOException {
    super.storeToXML(os, comment);
  }


  @Override
  public void store(OutputStream out, String comments) throws IOException {
    final Options apronOptions= Options.create().with(ISO_8859_1);
    try (final PropertyFileWriter writer= new PropertyFileWriter(out, apronOptions)) {
      if (comments != null) {
        writer.writeEntry(new BasicEntry(comments));
      }
      for (final Entry e : this.propertyFile.getAllEntries()) {
        writer.writeEntry(e);
      }
    }
  }


  /**
   * This methods always writes in UTF-8 encoding.
   *
   * @param writer
   * @param comments
   * @throws IOException
   */
  @Override
  public void store(Writer writer, String comments) throws IOException {
    final Options apronOptions= Options.create().with(UTF_8);
    try (final ByteArrayOutputStream out= new ByteArrayOutputStream();
      final PropertyFileWriter propertyFileWriter= new PropertyFileWriter(out, apronOptions);) {
      if (comments != null) {
        propertyFileWriter.writeEntry(new BasicEntry(comments));
      }
      for (final de.poiu.apron.entry.Entry e : this.propertyFile.getAllEntries()) {
        propertyFileWriter.writeEntry(e);
      }

      final String s= new String(out.toByteArray(), UTF_8);
      writer.append(s);
    }
  }


  @Override
  @Deprecated
  public void save(OutputStream out, String comments) {
    super.save(out, comments); //To change body of generated methods, choose Tools | Templates.
  }


  @Override
  public synchronized Object clone() {
    final Properties clone= new Properties();

    this.propertyFile.getAllEntries().forEach((e) -> {
      if (e instanceof BasicEntry) {
        final BasicEntry basicEntry= (BasicEntry) e;
        clone.propertyFile.appendEntry(new BasicEntry(basicEntry.toCharSequence()));
      } else if (e instanceof PropertyEntry) {
        final PropertyEntry propertyEntry= (PropertyEntry) e;
        clone.propertyFile.appendEntry(
          new PropertyEntry(propertyEntry.getLeadingWhitespace(),
            propertyEntry.getKey(),
            propertyEntry.getSeparator(),
            propertyEntry.getValue(),
            propertyEntry.getLineEnding()));
      } else {
        throw new RuntimeException("Unexpected Entry type: "+e.getClass());
      }
    });

    return clone;
  }


  @Override
  public synchronized int hashCode() {
    return this.propertyFile.getAllEntries().hashCode();
  }


  @Override
  public synchronized boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Properties)) {
      return false;
    }

    final Properties that= (Properties) o;

    if (this.propertyFile.entriesSize() != (that.propertyFile.entriesSize())) {
      return false;
    }

    return this.propertyFile.getAllEntries()
      .equals(that.propertyFile.getAllEntries());
  }


  @Override
  public synchronized String toString() {
    final StringBuilder sb= new StringBuilder();
    sb.append("{");

    if (!this.propertyFile.getAllEntries().isEmpty()) {
      sb.append("\n");
    }
    this.propertyFile.getAllEntries().forEach(e -> {
      sb.append(EscapeUtils.unescape(e.toCharSequence()));
      sb.append("\n");
    });

    sb.append("}");
    return sb.toString();
  }
}
