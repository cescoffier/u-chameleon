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
 * The Debug Activator launches ShellTUI if enabled.
 *
 * @author <a href="mailto:chameleon-dev@ow2.org">Chameleon Project Team</a>
 */
public class DebugScanner extends DefaultScanner {

    /**
     * Shell prefix.
     */
    public static final String SHELL = "org.apache.felix.shell";

    /**
     * Gogo bundle prefix.
     */
    public static final String GOGO = "org.apache.felix.gogo";

    /**
     * Arch prefix.
     */
    public static final String ARCH_SHELL = "org.apache.felix.ipojo.arch-";

    /**
     * Arch Gogo prefix.
     */
    public static final String ARCH_GOGO = "org.apache.felix.ipojo.arch.gogo-";

    /**
     * The core directory.
     */
    private File directory;

    /**
     * The logger.
     */
    Logger logger = LoggerFactory.getLogger(
            UChameleon.CHAMELEON_LOGGER_NAME);

    /**
     * Initializes the activator.
     *
     * @param dir the core directory.
     */
    public DebugScanner(File dir) {
        directory = dir;
    }

    public List<BundleDescriptor> scanForBundles()
            throws Exception {

        File[] files = directory.listFiles();
        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();
        boolean gogo = isGogo(files);
        for (int i = 0; i < files.length; i++) {
            if (gogo) {
                // Gogo
                if (StringUtils.contains(files[i].getName(), GOGO)
                        || StringUtils.contains(files[i].getName(), ARCH_GOGO)) {
                    BundleDescriptor descriptor = POJOSRUtils.createBundleDescriptorFromJarFile(files[i]);
                    logger.info("Adding bundle descriptor for " + files[i].getAbsolutePath());
                    bundles.add(descriptor);

                }
            } else {
                // Shell-TUI.
                if (StringUtils.contains(files[i].getName(), SHELL)
                        || StringUtils.contains(files[i].getName(), ARCH_SHELL)) {
                    BundleDescriptor descriptor = POJOSRUtils.createBundleDescriptorFromJarFile(files[i]);
                    logger.info("Adding bundle descriptor for " + files[i].getAbsolutePath());
                    bundles.add(descriptor);
                }
            }
        }
        return bundles;
    }

    /**
     * Checks whether Gogo bundles are available.
     *
     * @param files the core directory files
     * @return <code>true</code> if the core directory contains
     *         Gogo bundles, <code>false</code> otherwise.
     */
    private boolean isGogo(File[] files) {
        for (int i = 0; i < files.length; i++) {
            if (StringUtils.contains(files[i].getName(), GOGO)) {
                return true;
            }
        }
        return false;
    }

}
