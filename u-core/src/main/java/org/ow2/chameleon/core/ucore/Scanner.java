package org.ow2.chameleon.core.ucore;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import org.osgi.framework.BundleActivator;

import java.util.List;

/**
 * Scanners are responsible to find bundles and
 * to manage others artifacts. They will be notified when the
 * underlying framework starts and stops
 */
public interface Scanner extends BundleActivator {

    List<BundleDescriptor> scanForBundles() throws Exception;
}
