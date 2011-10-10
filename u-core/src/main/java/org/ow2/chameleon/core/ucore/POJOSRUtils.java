package org.ow2.chameleon.core.ucore;

import de.kalpatec.pojosr.framework.PojoSR;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * A couple of utility methods related to POJOSR
 */
public class POJOSRUtils {

    public static Map<String, String> getHeaders(URL manifestURL) throws Exception {
        InputStream input = manifestURL.openStream();
        byte[] bytes = new byte[1024 * 1024 * 2];
        int size = 0;
        for (int i = input.read(bytes); i != -1; i = input.read(bytes, size, bytes.length - size)) {
            size += i;
            if (size == bytes.length) {
                byte[] tmp = new byte[size * 2];
                System.arraycopy(bytes, 0, tmp, 0, bytes.length);
                bytes = tmp;
            }
        }

        // Now parse the main attributes. The idea is to do that
        // without creating new byte arrays. Therefore, we read through
        // the manifest bytes inside the bytes array and write them back into
        // the same array unless we don't need them (e.g., \r\n and \n are skipped).
        // That allows us to create the strings from the bytes array without the skipped
        // chars. We stopp as soon as we see a blankline as that denotes that the main
        //attributes part is finished.
        String key = null;
        int last = 0;
        int current = 0;

        Map<String, String> headers = new HashMap<String, String>();
        for (int i = 0; i < size; i++) {
            // skip \r and \n if it is follows by another \n
            // (we catch the blank line case in the next iteration)
            if (bytes[i] == '\r') {
                if ((i + 1 < size) && (bytes[i + 1] == '\n')) {
                    continue;
                }
            }
            if (bytes[i] == '\n') {
                if ((i + 1 < size) && (bytes[i + 1] == ' ')) {
                    i++;
                    continue;
                }
            }
            // If we don't have a key yet and see the first : we parse it as the key
            // and skip the :<blank> that follows it.
            if ((key == null) && (bytes[i] == ':')) {
                key = new String(bytes, last, (current - last), "UTF-8");
                if ((i + 1 < size) && (bytes[i + 1] == ' ')) {
                    last = current + 1;
                    continue;
                } else {
                    throw new Exception(
                            "Manifest error: Missing space separator - " + key);
                }
            }
            // if we are at the end of a line
            if (bytes[i] == '\n') {
                // and it is a blank line stop parsing (main attributes are done)
                if ((last == current) && (key == null)) {
                    break;
                }
                // Otherwise, parse the value and add it to the map (we throw an
                // exception if we don't have a key or the key already exist.
                String value = new String(bytes, last, (current - last), "UTF-8");
                if (key == null) {
                    throw new Exception("Manifest error: Missing attribute name - " + value);
                } else if (headers.put(key, value) != null) {
                    throw new Exception("Manifest error: Duplicate attribute name - " + key);
                }
                last = current;
                key = null;
            } else {
                // write back the byte if it needs to be included in the key or the value.
                bytes[current++] = bytes[i];
            }
        }
        return headers;
    }

    public static URL getRootURLFromManifestURL(URL url) throws Exception {
        String externalForm = url.toExternalForm();
        return new URL(externalForm.substring(0, externalForm.length()
                - "META-INF/MANIFEST.MF".length()));
    }

    public static BundleDescriptor createBundleDescriptorFromJarFile(File jar) throws Exception {
        JarFile theJar = new JarFile(jar);
        URL url = new URL("jar:" + jar.toURI().toURL().toExternalForm() + "!/META-INF/MANIFEST.MF");
        Map headers = getHeaders(url);
        BundleDescriptor descriptor = new BundleDescriptor(PojoSR.class.getClassLoader(),
                getRootURLFromManifestURL(url),
                headers);
        return descriptor;
    }

}
