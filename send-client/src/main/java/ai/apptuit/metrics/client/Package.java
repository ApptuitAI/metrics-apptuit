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

package ai.apptuit.metrics.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class Package {

    private static final Logger LOGGER = Logger.getLogger(Package.class.getName());
    public static final String VERSION = loadPackageVersion();

    private Package() {
    }

    private static String loadPackageVersion() {
        Enumeration<URL> resources = null;

        try {
            String version = Package.class.getPackage().getImplementationVersion();
            if (version != null) {
                return version;
            }

            ClassLoader classLoader = Package.class.getClassLoader();
            if (classLoader == null) {
                return "?";
            }
            resources = classLoader.getResources("META-INF/MANIFEST.MF");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error locating manifests.", e);
        }


        while (resources != null && resources.hasMoreElements()) {
            URL manifestUrl = resources.nextElement();
            try (InputStream resource = manifestUrl.openStream()) {
                Manifest manifest = new Manifest(resource);
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes != null) {
                    String agentClass = mainAttributes.getValue("Implementation-Title");
                    //the agent class comes as "metrics-apptuit-send-client"
                    if (agentClass != null && agentClass.contains("metrics-apptuit")) {
                        String packageVersion = mainAttributes.getValue("Implementation-Version");
                        if (packageVersion != null) {
                            return packageVersion;
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading manifest from [" + manifestUrl + "]", e);
            }

        }
        return "?";
    }
}
