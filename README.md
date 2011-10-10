U-Chameleon
===========

U-Chameleon is a fork of [OW2 Chameleon Core](http://wiki.chameleon.ow2.org/xwiki/bin/view/Main/Core) using
[POJOSR](http://pojosr.googlecode.com/) instead of full OSGi. The resulting stack exhibits the service-level
dyanmism and modularity, the classpath organization, but does not have the deployement dynamism of OSGi. The main
advantage is the simplification of the classloading policy as it's using the Java _classpath_ instead of the OSGi
import/export policy.

Project
-------

* _pojosr-installer_ installs the POJOSR library into your local maven repository
* _u-core_ is the fork of OW2 Chameleon Core using pojosr
* _web-distribution_ is a fork of the chameleon web distribution to use u-core

Building and running
--------------------

* Compilation:

    mvn clean install

* Running the web distribution

    cd web-distribution/target/chameleon
    sh chameleon-start.sh --debug
    // ... you get the Gogo shell




