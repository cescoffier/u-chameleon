package org.ow2.chameleon.core.ucore;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * TODO Exploded bundle not yet supported
 */
public class DirectoryScanner extends DefaultScanner implements ServiceListener {

    File directory;

    /**
     * The logger.
     */
    Logger logger = LoggerFactory.getLogger(
            UChameleon.CHAMELEON_LOGGER_NAME);
    private List<File> configuration;
    private BundleContext context;

    public DirectoryScanner(File dir) {
        directory = dir;
    }

    public List<BundleDescriptor> scanForBundles()
            throws Exception {
        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();

        List<File> jars = new ArrayList<File>();
        configuration = new ArrayList<File>();

        traverse(jars, configuration, directory);

        byte[] bytes = new byte[1024 * 1024 * 2];

        for (File jar : jars) {
            BundleDescriptor descriptor = POJOSRUtils.createBundleDescriptorFromJarFile(jar);
            logger.info("Adding bundle descriptor for " + jar.getAbsolutePath());
            bundles.add(descriptor);
        }


        return bundles;
    }

    /**
     * Collects bundles to install and begins the ConfigurationAdmin service
     * tracking.
     *
     * @param bundleContext the bundle context
     * @throws Exception if the Bundle Installer cannot be started correctly
     * @see org.osgi.framework.BundleActivator
     *      #start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception {
        context = bundleContext;

        context.addServiceListener(this, "(" + Constants.OBJECTCLASS + "="
                + ConfigurationAdmin.class.getName() + ")");

        if (context.getServiceReference(ConfigurationAdmin.class.getName())
                != null) {
            parseConfigurations(context, configuration);
            logger.info("Configurations from "
                    + directory.getAbsoluteFile() + " parsed");
        }

    }

    /**
     * Stops configuration admin tracking.
     *
     * @param arg0 the bundle context
     * @throws Exception if the Bundle Installer cannot be stopped correctly
     * @see org.osgi.framework.BundleActivator
     *      #stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext arg0) throws Exception {
        context.removeServiceListener(this);
    }

    /**
     * Parses cfg file associated PID. This supports both ManagedService PID and
     * ManagedServiceFactory PID
     *
     * @param path the path
     * @return structure {pid, factory pid} or {pid, <code>null</code> if not a
     *         Factory configuration.
     */
    String[] parsePid(String path) {
        String pid = path.substring(0, path.length() - ".cfg".length());
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    /**
     * Gets a Configuration object.
     *
     * @param pid        the pid
     * @param factoryPid the factory pid
     * @param cm         the config admin service
     * @return the Configuration object (used to update the configuration)
     * @throws Exception if the Configuration object cannot be retrieved
     */
    Configuration getConfiguration(String pid, String factoryPid,
                                   ConfigurationAdmin cm) throws Exception {
        Configuration newConfiguration = null;
        if (factoryPid != null) {
            newConfiguration = cm.createFactoryConfiguration(pid, null);
        } else {
            newConfiguration = cm.getConfiguration(pid, null);
        }
        return newConfiguration;
    }

    /**
     * Service Listener method to track the configuration admin service.
     *
     * @param event the service event
     * @see org.osgi.framework.ServiceListener
     *      #serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            try {
                parseConfigurations(context, configuration);
                logger.info("Configurations from "
                        + directory.getAbsoluteFile() + " parsed");
            } catch (Exception e) {
                logger
                        .error("Configurations from "
                                + directory.getAbsoluteFile()
                                + " can not be parsed correctly : "
                                + e.getMessage(), e);
            }

        }
    }

    /**
     * Parses configuration file.
     *
     * @param context the context
     * @param configs the configuration file list.
     * @throws Exception if the configuration cannot be parsed correctly, or if
     *                   the Configuration Admin is not available.
     */
    private void parseConfigurations(BundleContext context, List<File> configs)
            throws Exception {
        ConfigurationAdmin admin = getConfigurationAdmin(context);
        for(File configuration: configs) {
            Properties p = new Properties();
            InputStream in = new FileInputStream(configuration);
            p.load(in);
            in.close();
            String[] pid = parsePid(configuration.getName());
            Hashtable ht = new Hashtable();
            ht.putAll(p);
            Configuration config = getConfiguration(pid[0], pid[1], admin);
            if (config.getBundleLocation() != null) {
                config.setBundleLocation(null);
            }
            logger.info("A configuration will be pushed " + ht);
            config.update(ht);
        }

    }

    /**
     * Gets the configuration admin service.
     *
     * @param context the bundle context.
     * @return the Configuration Admin service object
     * @throws Exception if the configuration admin is unavailable.
     */
    private ConfigurationAdmin getConfigurationAdmin(BundleContext context)
            throws Exception {
        // Should be there !
        ServiceReference ref = context
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (ref == null) {
            throw new Exception("Configuration Admin unavailable !");
        } else {
            return (ConfigurationAdmin) context.getService(ref);
        }

    }


    /**
     * Traverses the directory structure to find bundles and configuration
     * files.
     *
     * @param jars    the found bundle list
     * @param configs the found .cfg files.
     * @param dir     the directory to traverse.
     */
    private void traverse(List<File> jars, List<File> configs,
                          File dir) {
        String[] list = dir.list();
        for (int i = 0; i < list.length; i++) {
            File file = new File(dir, list[i]);
            if (file.isFile()) {
                if (list[i].endsWith(".jar")) {
                    jars.add(file);
                } else if (list[i].endsWith(".cfg")) {
                    configs.add(file);
                }
            }
        }
    }


}
