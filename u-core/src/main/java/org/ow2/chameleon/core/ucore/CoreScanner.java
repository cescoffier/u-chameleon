/*
 * Copyright 2009 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.chameleon.core.ucore;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Core Activator launches Chameleon Core bundles.
 * <ol>
 * <li>iPOJO</li>
 * <li>File Install</li>
 * <li>Felix OSGi Compendium</li>
 * <li>Felix Configuration Admin</li>
 * </ol>
 *
 * @author <a href="mailto:chameleon-dev@ow2.org">Chameleon Project Team</a>
 */
public class CoreScanner extends DefaultScanner {


    /**
     * iPOJO Prefix.
     */
    public static final String IPOJO = "org.apache.felix.ipojo-";

    /**
     * Config Admin Prefix.
     */
    public static final String CONFIG_ADMIN = "org.apache.felix.configadmin-";

    /**
     * FileInstall Prefix.
     */
    public static final String FILE_INSTALL = "org.apache.felix.fileinstall-";

    /**
     * Compendium Prefix.
     */
    public static final String COMPENDIUM = "org.osgi.compendium-";

    /**
     * Number of bundle composing the core.
     */
    public static final int COUNT = 4;

    /**
     * Core directory.
     */
    private File directory;

    /**
     * The logger.
     */
    Logger logger = LoggerFactory.getLogger(
                UChameleon.CHAMELEON_LOGGER_NAME);

    /**
     * Initializes the Core activator.
     *
     * @param dir the core directory
     */
    public CoreScanner(File dir) {
        directory = dir;
    }

    public List<BundleDescriptor> scanForBundles()
            throws Exception {
        File[] files = directory.listFiles();
        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();
        for (int i = 0; i < files.length; i++) {
            if (match(files[i].getAbsolutePath())) {
                BundleDescriptor descriptor = POJOSRUtils.createBundleDescriptorFromJarFile(files[i]);
                logger.info("Adding bundle descriptor for " + files[i].getAbsolutePath());
                bundles.add(descriptor);
            }
        }
        return bundles;
    }


    /**
     * Checks if the given path is one of the core bundle.
     *
     * @param absolutePath the path
     * @return true if the path contains one of the core bundle
     *         prefix.
     */
    private boolean match(String absolutePath) {
        return (StringUtils.contains(absolutePath, IPOJO)
                || StringUtils.contains(absolutePath, CONFIG_ADMIN)
                || StringUtils.contains(absolutePath, FILE_INSTALL)
                || StringUtils.contains(absolutePath, COMPENDIUM))
                && absolutePath.endsWith(".jar");
    }
}
