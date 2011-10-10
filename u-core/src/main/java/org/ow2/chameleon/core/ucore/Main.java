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

import org.osgi.framework.BundleException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Chameleon Main class.
 * @author <a href="mailto:chameleon-dev@ow2.org">Chameleon Project Team</a>
 */
public final class Main {

    /**
     * Default Admin Port (60900).
     */
    public static final int DEFAULT_ADMIN_PORT = 60900;

    /**
     * Constructor to avoid creating a new Main object.
     */
    private Main() { }

    /**
     * Prints Welcome Banner.
     */
    private static void printWelcomeBanner() {
        StringBuffer banner = new StringBuffer();
        banner.append("\n");
        banner.append("\t==============================\n");
        banner.append("\t|                            |\n");
        banner.append("\t|   Welcome to u-Chameleon   |\n");
        banner.append("\t|                            |\n");
        banner.append("\t==============================\n");
        banner.append("\n");
        System.out.println(banner);
    }

    /**
     * Prints Started Banner.
     */
    private static void printStartedBanner() {
        System.out.println("\n");
        System.out.println("\t=========================");
        System.out.println("\t|   Chameleon started   |");
        System.out.println("\t=========================");
        System.out.println("\n");
    }

    /**
     * Prints Stopped Banner.
     */
    private static void printStoppedBanner() {
        System.out.println("\n");
        System.out.println("\t=========================");
        System.out.println("\t|   Chameleon stopped   |");
        System.out.println("\t=========================");
        System.out.println("\n");
    }



    /**
     * Main method. Supported parameters are:
     * <ul>
     * <li>--debug : enables shell </li>
     * <li>--core=x : set core directory to x</li>
     * <li>--app=x : set the application directory to x </li>
     * <li>--runtime=x : set the runtime directory to x </li>
     * <li>--config=x : use x as the Chameleon properties </li>
     * <li>--deploy=x : use x as the 'deploy' folder </li>
     * <li>--admin=x : enabled / disabled the admin socket
     * (x = {enabled, disabled}). The admin socket is enabled
     * by default.</li>
     * <li>chameleon.admin.port=x : set the admin socket port</li>
     * <li>stop : stop a chameleon</li>

     * </ul>
     * @param args the chameleon parameter.
     */
    public static void main(final String[] args) {

        // STOP MODE
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--stop")) {
                stop(args);
                return;
            }
        }

        // START MODE
        printWelcomeBanner();

        UChameleon chameleon = null;
        try {
            chameleon  = createChameleon(args);
        } catch (Exception e) {
            System.err
                    .println("Cannot initaliaze Chameleon : " + e.getMessage());
        }
        if (chameleon == null) {
            return;
        }

        registerShutdownHook(chameleon);
        try {
            chameleon.start();
        } catch (BundleException e) {
            System.err.println("Cannot start Chameleon : " + e.getMessage());
        }

        printStartedBanner();

//        boolean admin = isAdminEnabled(args);
//        if (admin) {
//            final UChameleon cham = chameleon;
//            Runnable runnable = new Runnable() {
//                public void run() {
//                    openAdminSocket(cham, args);
//                }
//            };
//            new Thread(runnable).start();
//        }

    }

    /**
     * Stops a chameleon by connecting to the admin socket and sending
     * the stop command. For security reason this does not work for remote
     * chameleon.
     * @param args the arguments.
     */
    private static void stop(String[] args) {
        int port = DEFAULT_ADMIN_PORT;
        try {
            String p = System.getProperty("chameleon.admin.port");
            if (p != null) {
                port = Integer.parseInt(p);
            }
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--chameleon.admin.port=")) {
                    p = args[i].substring("--chameleon.admin.port=".length());
                    port = Integer.parseInt(p);
                }
            }

            Socket client =
                new Socket(InetAddress.getLocalHost(), port);

            PrintWriter out =
                new PrintWriter(
                        new OutputStreamWriter(client.getOutputStream()), true);
            InputStream input = client.getInputStream();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(input));
            out.println("STOP");
            out.flush();
            reader.readLine();
            client.close();

        } catch (Exception e) {
            System.err.println("Cannot connect to "
                   + "the admin port (" + port + ")");
        }
    }

    /**
     * Open the admin socket.
     * @param chameleon the created chameleon
     * @param args the command line arguments.
     */
    private static void openAdminSocket(final UChameleon chameleon,
            String[] args) {
        int port = DEFAULT_ADMIN_PORT;
        try {
            String p = System.getProperty("chameleon.admin.port");
            if (p != null) {
                port = Integer.parseInt(p);
            }
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--chameleon.admin.port=")) {
                    p = args[i].substring("--chameleon.admin.port=".length());
                    port = Integer.parseInt(p);
                }
            }

            ServerSocket socket = new ServerSocket(port);
            while (true) {
                Socket client = socket.accept();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(client.getOutputStream()), true);
                String str = reader.readLine();

                // STOP Command
                if (str != null && str.equals("STOP")) {
                    try {
                        chameleon.stop();
                        out.println("OK");
                    } catch (Exception e) {
                        out.println("KO : " + e.getMessage());
                    } finally {
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                    }
                    return;
                } else {
                    out.println("KO : Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot start the admin socket on "
                    + "(" + port + ")");
        }
    }

    /**
     * Creates the Chameleon instance.The instance is not started.
     * @param args the command line parameters.
     * @return the Chameleon instance
     * @throws Exception if the chameleon instance cannot be created correctly.
     */
    public static UChameleon createChameleon(String[] args) throws Exception {
        boolean debug = isDebugModeEnabled(args);
        String core = getCore(args);
        String app = getApp(args);
        String runtime = getRuntime(args);
        String fileinstall = getDeployDirectory(args);
        String config = getProps(args);
        String sys = getSystemProps(args);

        return new UChameleon(core, debug, app, runtime, fileinstall,
                config, sys);

    }

    /**
     * Parses the --deploy parameter.
     * @param args the parameters.
     * @return the deploy folder or <code>null</code>
     *  if not found.
     */
    private static String getDeployDirectory(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--deploy=")) {
                return arg.substring("--deploy=".length());
            }
        }
        return null;
    }

    /**
     * Registers a shutdown hook to stop nicely the embedded framework.
     * @param chameleon the stopped chameleon
     */
    private static void registerShutdownHook(final UChameleon chameleon) {
        Runtime runtime = Runtime.getRuntime();
        Runnable hook = new Runnable() {
            public void run() {
                try {
                    if (chameleon != null) {
                        chameleon.stop();
                        printStoppedBanner();

                    }
                } catch (BundleException e) {
                    System.err.println("Cannot stop Chameleon correctly : "
                            + e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Unexpected Exception : "
                            + e.getMessage());
                    // nothing to do
                }
            }
        };
        runtime.addShutdownHook(new Thread(hook));

    }

    /**
     * Parses the --debug parameter.
     * @param args the parameters.
     * @return true if the debug mode is enabled.
     */
    private static boolean isDebugModeEnabled(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--debug")) {
                return true;
            }
        }
        return false;

    }

    /**
     * Parses the --admin parameter.
     * @param args the parameters.
     * @return true if the admin mode is enabled.
     */
    private static boolean isAdminEnabled(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--admin=disabled")) {
                return false;
            }
        }
        return true;

    }

    /**
     * Parses the --core parameter.
     * @param args the parameters.
     * @return the core folder or <code>null</code>
     *  if not found.
     */
    private static String getCore(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--core=")) {
                return arg.substring("--core=".length());
            }
        }
        return null;
    }

    /**
     * Parses the --app parameter.
     * @param args the parameters.
     * @return the application folder or <code>null</code>
     *  if not found.
     */
    private static String getApp(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--app=")) {
                return arg.substring("--app=".length());
            }
        }
        return null;
    }

    /**
     * Parses the --config parameter.
     * @param args the parameters.
     * @return the configuration file path or <code>null</code>
     *  if not found.
     */
    private static String getProps(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--config=")) {
                return arg.substring("--config=".length());
            }
        }
        return null;
    }

    /**
     * Parses the --system parameter.
     * @param args the parameters.
     * @return the configuration file path or <code>null</code>
     *  if not found.
     */
    private static String getSystemProps(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--system=")) {
                return arg.substring("--system=".length());
            }
        }
        return null;
    }

    /**
     * Parses the --runtime parameter.
     * @param args the parameters.
     * @return the runtime folder or <code>null</code>
     *  if not found.
     */
    private static String getRuntime(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.contains(arg, "--runtime=")) {
                return arg.substring("--runtime=".length());
            }
        }
        return null;
    }

}
