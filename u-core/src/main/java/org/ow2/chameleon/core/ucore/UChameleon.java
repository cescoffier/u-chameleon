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

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Chameleon Main Class.
 * @author <a href="mailto:chameleon-dev@ow2.org">Chameleon Project Team</a>
 */
public class UChameleon {

    /**
     * The default core directory.
     */
    public static final String CORE_DIR = "core";

    /**
     * The default runtime directory.
     */
    public static final String RUNTIME_DIR = "runtime";

    /**
     * The default application directory.
     */
    public static final String APPLICATION_DIR = "application";

    /**
     * The default deploy directory.
     */
    public static final String DEPLOY_DIR = "deploy";

    /**
     * The default chameleon properties file.
     */
    public static final String CHAMELEON_PROPERTIES = "chameleon.properties";

    /**
     * The default system properties file.
     */
    public static final String SYSTEM_PROPERTIES = "system.properties";

    /**
     * Property to set the log level.
     */
    public static final String CHAMELEON_LOG_LEVEL_PROPERTY =
        "chameleon.log.level";

    /**
     * Property to set the log directory.
     */
    public static final String CHAMELEON_LOG_DIR_PROPERTY = "chameleon.log.dir";

    /**
     * The property to enable / disable the log.
     */
    public static final String CHAMELEON_LOG_PROPERTY = "chameleon.log.enabled";

    /**
     * The chameleon logger name.
     */
    public static final String CHAMELEON_LOGGER_NAME = "org.ow2.chameleon";

    /**
     * The default log directory.
     */
    public static final String CHAMELEON_LOG_DIR_DEFAULT = "log";

    /**
     * The default log level.
     */
    public static final String CHAMELEON_LOG_LEVEL_DEFAULT = "info";

    /**
     * The property to set the runtime directory.
     */
    public static final String CHAMELEON_RUNTIME_PROPERTY = "chameleon.runtime";

    /**
     * The property to set the core directory.
     */
    public static final String CHAMELEON_CORE_PROPERTY = "chameleon.core";

    /**
     * The property to set the application directory.
     */
    public static final String CHAMELEON_APP_PROPERTY = "chameleon.application";

    /**
     * The property to set the deploy directory.
     */
    public static final String CHAMELEON_DEPLOY_PROPERTY = "chameleon.deploy";

    /**
     * OSGi stop timeout.
     */
    private static final int OSGI_STOP_TIMEOUT = 10000;

    /**
     * Embedded framework instance.
     */
    private Framework framework;

    /**
     * Is the debug mode enabled?
     */
    private boolean debug;

    /**
     * The Core directory.
     */
    private String core;

    /**
     * The Runtime directory.
     */
    private String runtime;

    /**
     * The Application directory.
     */
    private String application;

    /**
     * The file install directory.
     */
    private String deploy;

    /**
     * Chameleon Logger.
     */
    private Logger logger;

    /**
     * List of bundle scanner to start during framework startup.
     */
    private List<BundleDescriptor> bundles;
    private List<Scanner> scanners;
    private Map configuration;

    private PojoServiceRegistry registry;

    /**
     * Creates a Chameleon instance. This constructor does not allows to set the
     * core directory (so, uses 'core'), nor the chameleon properties.
     * @param debug is the debug mode enabled.
     * @param runtime the runtime directory
     * @param app the application directory
     * @param fi the file install directory
     * @throws Exception something wrong happens.
     */
    public UChameleon(boolean debug, String runtime, String app, String fi)
            throws Exception {
        this.debug = debug;
        core = CORE_DIR;
        if (runtime != null) {
            this.runtime = runtime;
        }

        if (app != null) {
            this.application = app;
        }

        if (fi != null) {
            this.deploy = fi;
        }

        Map map = new HashMap();
        // Populate the map with properties
        populate(map, null);

        logger = initializeLoggingSystem(map, this.runtime, this.application,
                this.deploy, this.debug);

        configuration = getProperties(map);
        scanners = getScannerList(configuration);
        bundles = getBundleDescriptors();
    }

    /**
     * Creates a Chameleon instance. This constructor allows setting the core
     * directory and the chameleon properties.
     * @param core the core directory
     * @param debug is the debug mode enabled.
     * @param runtime the runtime directory
     * @param app the application directory
     * @param fi the file install directory
     * @param props the chameleon properties
     * @param sys the system properties
     * @throws Exception something wrong happens.
     */
    public UChameleon(String core, boolean debug, String runtime, String app,
                      String fi, String props, String sys) throws Exception {
        this.debug = debug;

        // Load system properties
        loadSystemProperties(sys);

        Map map = new HashMap();
        // Populate the map with properties
        populate(map, props);
        // Compute the directories according to the configuration
        if (core == null) {
            this.core = (String) map.get(CHAMELEON_CORE_PROPERTY);
            if (this.core == null) {
                this.core = CORE_DIR; // Use default
            }
        } else {
            this.core = core;
        }

        if (runtime == null) {
            this.runtime = (String) map.get(CHAMELEON_RUNTIME_PROPERTY);
        } else {
            this.runtime = runtime;
        }

        if (app == null) {
            this.application = (String) map.get(CHAMELEON_APP_PROPERTY);
        } else {
            this.application = app;
        }

        if (fi == null) {
            this.deploy = (String) map.get(CHAMELEON_DEPLOY_PROPERTY);
        } else {
            this.deploy = fi;
        }

        logger = initializeLoggingSystem(map, this.runtime, this.application,
                this.deploy, this.debug);

        configuration = getProperties(map);
        scanners = getScannerList(configuration);
        bundles = getBundleDescriptors();
    }

    /**
     * Loads system properties.
     * @param sys the given system properties, if null try to use
     * default one.
     * @throws Exception if the file is not found
     */
    private void loadSystemProperties(String sys) throws Exception {
        Properties props = new Properties();
        File file = null;
        if (sys == null) {
            file = new File(SYSTEM_PROPERTIES);
        } else {
            file = new File(sys);
            if (!file.exists()) {
                throw new Exception("The given property file does not exist :"
                        + file.getAbsolutePath());
            }
        }

        if (file.exists()) {
            try {
                props.load(new FileInputStream(file));
                Enumeration e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    String v = StringUtils.substVars((String) props.get(k), k,
                            null, System.getProperties());
                    System.setProperty(k, v);
                }
            } catch (Exception e) {
                throw new Exception("Cannot read the system property file : "
                        + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Initialized the logging framework (backend).
     * @param config configuration
     * @param runtime the runtime directory
     * @param app the application directory
     * @param fi the deploy directory
     * @param debug is debug enabled ?
     * @return the chameleon logger
     * @throws java.io.IOException if something bad happens
     */
    public static Logger initializeLoggingSystem(Map config, String runtime,
            String app, String fi, boolean debug) throws IOException {

        // Set the CHAMELEON_LOG_DIR_PROPERTY property. This will be used by the
        // log configuration.
        // It also creates the parent directories.
        if (System.getProperty(CHAMELEON_LOG_DIR_PROPERTY) == null) {
            // Check if we receive the value inside the configuration.
            if (config.get(CHAMELEON_LOG_DIR_PROPERTY) == null) {
                // If not, use the default value.
                File f = new File(CHAMELEON_LOG_DIR_DEFAULT);
                f.mkdirs();
                System.setProperty(CHAMELEON_LOG_DIR_PROPERTY,
                        f.getAbsolutePath());
            } else {
                // Else use the given value.
                File f = new File((String)
                        config.get(CHAMELEON_LOG_DIR_PROPERTY));
                f.mkdirs();
                System.setProperty(CHAMELEON_LOG_DIR_PROPERTY,
                        f.getAbsolutePath());
            }
        } else {
            // Already set, just create the directory.
            File f = new File(System.getProperty(CHAMELEON_LOG_DIR_PROPERTY));
            f.mkdirs();
        }


        if (System.getProperty(CHAMELEON_LOG_LEVEL_PROPERTY) == null) {
            if (config.get(CHAMELEON_LOG_LEVEL_PROPERTY) == null) {
                System.setProperty(CHAMELEON_LOG_LEVEL_PROPERTY,
                        CHAMELEON_LOG_LEVEL_DEFAULT);
            } else {
                System.setProperty(CHAMELEON_LOG_LEVEL_PROPERTY,
                        (String) config.get(CHAMELEON_LOG_LEVEL_PROPERTY));
            }
        }

        Logger log = LoggerFactory.getLogger(CHAMELEON_LOGGER_NAME);

        if (debug) {
            log.info("debug mode enabled");
        }

        if (runtime != null) {
            log.info("Chameleon runtime set to " + runtime);
        } else {
            log.info("Chameleon runtime set to " + RUNTIME_DIR);
        }

        if (app != null) {
            log.info("Chameleon application set to " + app);
        } else {
            log.info("Chameleon application set to " + APPLICATION_DIR);
        }

        if (fi != null) {
            log.info("Chameleon file install folder set to " + fi);
        } else {
            log.info("Chameleon file install folder set to " + DEPLOY_DIR);
        }

        return log;
    }

    /**
     * Initializes and Starts the Chameleon frameworks. It configure the
     * embedded OSGi framework and deploys bundles
     * @return the Bundle Context.
     * @throws org.osgi.framework.BundleException if a bundle cannot be installed or started
     *         correctly.
     */
    public void start() throws BundleException {
        configuration.put(
                PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, bundles);

        //ServiceLoader<PojoServiceRegistryFactory> loader = ServiceLoader.load(PojoServiceRegistryFactory.class);

        try {
            registry = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(configuration);
            for (Scanner scanner : scanners) {
                scanner.start(registry.getBundleContext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the underlying framework.
     * @throws org.osgi.framework.BundleException should not happen.
     * @throws InterruptedException if the method is interupted during the
     *         waiting time.
     */
    public void stop() throws BundleException, InterruptedException {
         for (Scanner scanner : scanners) {
             try {
                 scanner.stop(registry.getBundleContext());
             } catch (Exception e) {
                 logger.error("Error when stopping scanner " + scanner, e);
             }
         }

    }

    /**
     * Populates the config map with the given properties file.
     * @param map the config map
     * @param properties the properties file path
     * @throws Exception if the file cannot be read
     */
    private void populate(Map map, String properties) throws Exception {
        File props = null;
        if (properties == null) {
            props = new File(CHAMELEON_PROPERTIES);
            if (!props.exists()) {
                return;
            }
        } else {
            props = new File(properties);
            if (!props.exists()) {
                throw new Exception("The given property file does not exist :"
                        + props.getAbsolutePath());
            }
        }

        // Props exits.
        Properties ps = new Properties();
        ps.load(new FileInputStream(props));

        // To support variable substitution, we must read entries one after one
        Properties env = new Properties();
        env.putAll(ps);
        env.putAll(System.getProperties());

        Enumeration keys = ps.keys();
        while (keys.hasMoreElements()) {
            String k = (String) keys.nextElement();
            String v = (String) ps.get(k);
            v = StringUtils.substVars(v, k, null, env);
            map.put(k, v);
        }
    }

    /**
     * computes system properties.
     * @param map the configuration map
     * @return the modified map.
     * @throws Exception if something wrong happen
     */
    public Map getProperties(Map map) throws Exception {
        if (!map.containsKey("org.osgi.framework.storage.clean")) {
            map.put("org.osgi.framework.storage.clean", "onFirstInit");
        }

        if (!map.containsKey("ipojo.log.level")) {
            map.put("ipojo.log.level", "WARNING");
        }

        if (!map.containsKey("org.osgi.framework.storage")) {
            map.put("org.osgi.framework.storage", "chameleon-cache");
        }

        if (!map.containsKey("org.osgi.framework.system.packages.extra")) {
            map.put("org.osgi.framework.system.packages.extra",
                    "org.osgi.service.cm; version=1.3.0,"
                            + "org.osgi.service.log; version=1.3.0, "
                            + "org.slf4j; version=1.6.1,"
                            + "org.slf4j.impl; version=1.6.1,"
                            + "org.slf4j.spi; version=1.6.1,"
                            + "org.slf4j.helpers; version=1.6.1");
        } else {
            String pcks = (String) map.get(
                    "org.osgi.framework.system.packages.extra");
            map.put("org.osgi.framework.system.packages.extra",
                    "org.osgi.service.cm; version=1.3.0,"
                            + "org.osgi.service.log; version=1.3.0, "
                            + "org.slf4j; version=1.6.1,"
                            + "org.slf4j.impl; version=1.6.1,"
                            + "org.slf4j.spi; version=1.6.1,"
                            + "org.slf4j.helpers; version=1.6.1,"
                            + pcks);
        }

        // File install configuration
        // Number of milliseconds between 2 polls of the directory;
        map.put("felix.fileinstall.poll", "2000");
        if (this.deploy != null) {
            // The name of the directory to watch;
            map.put("felix.fileinstall.dir", this.deploy);
        }

        // Debug configuration
        if (debug) {
            map.put("felix.fileinstall.debug", "1");
        } else {
            map.put("felix.fileinstall.debug", "-1");

        }
        // Automatically start newly discovered bundles;
        map.put("felix.fileinstall.bundles.new.start", "true");

        return map;
    }

    public List<Scanner> getScannerList(Map props) throws Exception {
        List<Scanner> list = new ArrayList<Scanner>();

        boolean isLogDisabled = false;
        String e = (String) props.get(CHAMELEON_LOG_PROPERTY);
        if (e != null && e.equalsIgnoreCase("false")) {
            isLogDisabled = true;
        }
        if (!isLogDisabled) {
            list.add(new LogScanner(logger));
        }

        // Core Scanner
        list.add(new CoreScanner(getCoreDirectory()));

        if (debug) {
            list.add(new DebugScanner(getCoreDirectory()));
        }

        if (runtime != null) {
            File file = new File(runtime);
            if (file.exists()) {
                list.add(new DirectoryScanner(file));
            } else {
                throw new Exception("The set runtime folder does not exist : "
                        + runtime);
            }
        } else {
            File file = new File(RUNTIME_DIR);
            if (file.exists()) {
                list.add(new DirectoryScanner(file));
            } else {
                logger.warn("No runtime directory");
            }
        }

        if (this.application != null) {
            File file = new File(this.application);
            if (file.exists()) {
                list.add(new DirectoryScanner(file));
            } else {
                throw new Exception(
                        "The set application folder does not exist : "
                                + this.application);
            }
        } else {
            File file = new File(APPLICATION_DIR);
            if (file.exists()) {
                list.add(new DirectoryScanner(file));
            } else {
                logger.warn("No application directory.");
            }
        }

        return list;
    }

    /**
     * Gets the bundles.
     * @return the list of Bundle Descriptor
     * @throws Exception if a directory does not exist.
     */
    public List<BundleDescriptor> getBundleDescriptors() throws Exception {
        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();

        for (Scanner scanner : scanners) {
            bundles.addAll(scanner.scanForBundles());
        }

        return bundles;
    }

    /**
     * Gets the core directory.
     * @return the Core directory (File)
     * @throws Exception if the directory does not exist.
     */
    private File getCoreDirectory() throws Exception {
        File file = null;
        if (core != null) {
            file = new File(core);
        } else {
            file = new File(CORE_DIR);
        }
        if (file.exists()) {
            return file;
        } else {
            throw new Exception("The Core directory does not exist");
        }
    }

}
