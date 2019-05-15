/*
 * Copyright 2017 Agilx, Inc.
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

package ai.apptuit.metrics.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Rajiv Shivane
 */
public class JInsightReporter implements Closeable, Reporter {

  private Closeable closeable;

  public static Builder forRegistry(MetricRegistry registry) {
    return new Builder(registry);
  }


  private JInsightReporter(MetricRegistry registry, Closeable function) {
    this.closeable = function;
  }

  @Override
  public void close() throws IOException {
    closeable.close();
  }

  public static class Builder {
    private final MetricRegistry registry;

    private Builder(MetricRegistry registry) {
      this.registry = registry;
    }

    /**
     * @return
     * @throws IllegalStateException if JInsight classes cannot be found in system classpath
     *                               or if JInsight could not be initialized
     */
    public JInsightReporter build() {

      final Class<?> aClass;
      try {
        aClass = ClassLoader.getSystemClassLoader().loadClass("ai.apptuit.metrics.jinsight.MetricRegistryCollection");
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        throw new IllegalStateException("Could not find JInsight classes in system classpath", e);
      }

      final Method deRegister;
      final Object delegate;

      try {
        Method getInstance = aClass.getMethod("getInstance");
        Method register = aClass.getMethod("register", Object.class);
        deRegister = aClass.getMethod("deRegister", Object.class);

        delegate = getInstance.invoke(null);
        register.invoke(delegate, this.registry);

      } catch (NoSuchMethodException e) {
        throw new IllegalStateException("Could not locate required methods to register with JInsight. Version mismatch?", e);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Error registering with JInsight.", e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException("Error registering with JInsight.", e.getCause());
      }


      return new JInsightReporter(registry, () -> {
        try {
          deRegister.invoke(delegate, this.registry);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IOException("Error closing reporter.", e);
        }
      });
    }
  }
}
