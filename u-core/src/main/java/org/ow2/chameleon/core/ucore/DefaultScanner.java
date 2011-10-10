package org.ow2.chameleon.core.ucore;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Default Scanner doing nothing.
 */
public class DefaultScanner implements Scanner {


    public List<BundleDescriptor> scanForBundles() throws Exception {
        return new ArrayList<BundleDescriptor>(0);
    }

    public void start(BundleContext bundleContext) throws Exception { }

    public void stop(BundleContext bundleContext) throws Exception { }
}
