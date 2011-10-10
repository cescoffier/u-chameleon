#!/bin/sh
#
# Copyright 2009 OW2 Chameleon
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

DIRNAME=`dirname $0`
PROGNAME=`basename $0`

#
# Check/Set up some easily accessible MIN/MAX params for JVM mem usage
#

if [ "x$JAVA_MIN_MEM" = "x" ]; then
    JAVA_MIN_MEM=128M
    export JAVA_MIN_MEM
fi

if [ "x$JAVA_MAX_MEM" = "x" ]; then
    JAVA_MAX_MEM=512M
    export JAVA_MAX_MEM
fi

warn() {
    echo "${PROGNAME}: $*"
}

die() {
    warn "$*"
    exit 1
}

detectOS() {
    # OS specific support (must be 'true' or 'false').
    cygwin=false;
    darwin=false;
    aix=false;
    os400=false;
    case "`uname`" in
        CYGWIN*)
            cygwin=true
            ;;
        Darwin*)
            darwin=true
            ;;
        AIX*)
            aix=true
            ;;
        OS400*)
            os400=true
            ;;
    esac
    # For AIX, set an environment variable
    if $aix; then
         export LDR_CNTRL=MAXDATA=0xB0000000@DSA
         export IBM_JAVA_HEAPDUMP_TEXT=true
         echo $LDR_CNTRL
    fi
}

unlimitFD() {
    # Use the maximum available, or set MAX_FD != -1 to use that
    if [ "x$MAX_FD" = "x" ]; then
        MAX_FD="maximum"
    fi

    # Increase the maximum file descriptors if we can
    if [ "$os400" = "false" ] && [ "$cygwin" = "false" ]; then
        MAX_FD_LIMIT=`ulimit -H -n`
        if [ "$MAX_FD_LIMIT" != 'unlimited' ]; then
            if [ $? -eq 0 ]; then
                if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
                    # use the system max
                    MAX_FD="$MAX_FD_LIMIT"
                fi

                ulimit -n $MAX_FD > /dev/null
                # echo "ulimit -n" `ulimit -n`
                if [ $? -ne 0 ]; then
                    warn "Could not set maximum file descriptor limit: $MAX_FD"
                fi
            else
                warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
            fi
        fi
    fi
}

locateHome() {
    CHAMELEON_HOME=`cd $DIRNAME; pwd`
    if [ ! -d "$CHAMELEON_HOME" ]; then
        die "CHAMELEON_HOME is not valid: $CHAMELEON_HOME"
    fi
}

setupNativePath() {
    # Support for loading native libraries
    LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:$CHAMELEON_HOME/lib"

    # For Cygwin, set PATH from LD_LIBRARY_PATH
    if $cygwin; then
        LD_LIBRARY_PATH=`cygpath --path --windows "$LD_LIBRARY_PATH"`
        PATH="$PATH;$LD_LIBRARY_PATH"
        export PATH
    fi
    export LD_LIBRARY_PATH
}

locateJava() {
    # Setup the Java Virtual Machine
    if $cygwin ; then
        [ -n "$JAVA" ] && JAVA=`cygpath --unix "$JAVA"`
        [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    fi

    if [ "x$JAVA" = "x" ]; then
        if [ "x$JAVA_HOME" = "x" ] && [ "$darwin" = "true" ]; then
            JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
        fi
        if [ "x$JAVA_HOME" != "x" ]; then
            if [ ! -d "$JAVA_HOME" ]; then
                die "JAVA_HOME is not valid: $JAVA_HOME"
            fi
            JAVA="$JAVA_HOME/bin/java"
        else
            warn "JAVA_HOME not set; results may vary"
            JAVA="java"
        fi
    fi
}

detectJVM() {
   #echo "`$JAVA -version`"
   # This service should call `java -version`,
   # read stdout, and look for hints
   if $JAVA -version 2>&1 | grep "^IBM" ; then
       JVM_VENDOR="IBM"
   # on OS/400, java -version does not contain IBM explicitly
   elif $os400; then
       JVM_VENDOR="IBM"
   else
       JVM_VENDOR="SUN"
   fi
   # echo "JVM vendor is $JVM_VENDOR"
}

setupDebugOptions() {
    if [ "x$JAVA_OPTS" = "x" ]; then
        JAVA_OPTS="$DEFAULT_JAVA_OPTS"
    fi
    export JAVA_OPTS
}

setupDefaults() {
    DEFAULT_JAVA_OPTS="-Xms$JAVA_MIN_MEM -Xmx$JAVA_MAX_MEM "

    #Set the JVM_VENDOR specific JVM flags
    if [ "$JVM_VENDOR" = "SUN" ]; then
        DEFAULT_JAVA_OPTS="-server $DEFAULT_JAVA_OPTS -Dcom.sun.management.jmxremote"
    elif [ "$JVM_VENDOR" = "IBM" ]; then
        if $os400; then
            DEFAULT_JAVA_OPTS="$DEFAULT_JAVA_OPTS"
        elif $aix; then
            DEFAULT_JAVA_OPTS="-Xverify:none -Xlp $DEFAULT_JAVA_OPTS"
        else
            DEFAULT_JAVA_OPTS="-Xverify:none $DEFAULT_JAVA_OPTS"
        fi
    fi

    # Add the jars in the core dir
    for file in $CHAMELEON_HOME/core/*.jar
    do
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$file"
        else
            CLASSPATH="$CLASSPATH:$file"
        fi
    done

    # Add the jars in the runtime dir
    for file in $CHAMELEON_HOME/runtime/*.jar
    do
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$file"
        else
            CLASSPATH="$CLASSPATH:$file"
        fi
    done

    # Add the jars in the application dir
    for file in $CHAMELEON_HOME/application/*.jar
    do
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$file"
        else
            CLASSPATH="$CLASSPATH:$file"
        fi
    done

    DEFAULT_JAVA_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
}

init() {
    # Determine if there is special OS handling we must perform
    detectOS

    # Unlimit the number of file descriptors if possible
    unlimitFD

    # Locate the Chameleon home directory
    locateHome

    # Setup the native library path
    setupNativePath

    # Locate the Java VM to execute
    locateJava

    # Determine the JVM vendor
    detectJVM

    # Setup default options
    setupDefaults

    # Install debug options
    setupDebugOptions

}

run() {
    if $cygwin; then
        CHAMELEON_HOME=`cygpath --path --windows "$CHAMELEON_HOME"`
        CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
    fi

	cd $CHAMELEON_HOME
    exec $JAVA $JAVA_OPTS $OPTS -classpath "$CLASSPATH" org.ow2.chameleon.core.ucore.Main "$@"
}

main() {
    init
    run "$@"
}

main "$@"
