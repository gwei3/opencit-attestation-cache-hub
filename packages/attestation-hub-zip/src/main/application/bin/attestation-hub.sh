#!/bin/bash

# chkconfig: 2345 80 30
# description: Intel Attestation Hub

### BEGIN INIT INFO
# Provides:          attestation-hub
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Should-Start:      $portmap
# Should-Stop:       $portmap
# X-Start-Before:    nis
# X-Stop-After:      nis
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# X-Interactive:     true
# Short-Description: attestation-hub
# Description:       Main script to run attestation-hub commands
### END INIT INFO
DESC="ATTESTATION HUB"
NAME=attestation-hub

# the home directory must be defined before we load any environment or
# configuration files; it is explicitly passed through the sudo command
export ATTESTATION_HUB_HOME=${ATTESTATION_HUB_HOME:-/opt/attestation-hub}

# the env directory is not configurable; it is defined as ATTESTATION_HUB_HOME/env and
# the administrator may use a symlink if necessary to place it anywhere else
export ATTESTATION_HUB_ENV=$ATTESTATION_HUB_HOME/env
attestation_hub_load_env() {
  local env_files="$@"
  local env_file_exports
  for env_file in $env_files; do
    if [ -n "$env_file" ] && [ -f "$env_file" ]; then
      . $env_file
      env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
      if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
    fi
  done  
}

if [ -z "$ATTESTATION_HUB_USERNAME" ]; then
  attestation_hub_load_env $ATTESTATION_HUB_HOME/env/attestation-hub-username
fi

###################################################################################################

### THIS NEEDS TO BE UPDATED LATER, MUST NOT REQUIRE USER TO RUN APPLICATION AS ROOT -rksavino

## if non-root execution is specified, and we are currently root, start over; the DIRECTOR_SUDO variable limits this to one attempt
## we make an exception for the uninstall command, which may require root access to delete users and certain directories
#if [ -n "$DIRECTOR_USERNAME" ] && [ "$DIRECTOR_USERNAME" != "root" ] && [ $(whoami) == "root" ] && [ -z "$DIRECTOR_SUDO" ] && [ "$1" != "uninstall" ]; then
#  sudo -u $DIRECTOR_USERNAME DIRECTOR_USERNAME=$DIRECTOR_USERNAME DIRECTOR_HOME=$DIRECTOR_HOME DIRECTOR_PASSWORD=$DIRECTOR_PASSWORD DIRECTOR_SUDO=true director $*
#  exit $?
#fi

###################################################################################################

# load environment variables; these may override the defaults set above and 
# also note that attestation-hub-username file is loaded twice, once before sudo and
# once here after sudo.
if [ -d $ATTESTATION_HUB_ENV ]; then
  attestation_hub_load_env $(ls -1 $ATTESTATION_HUB_ENV/*)
fi

# default attestation-hub layout follows the 'home' style
export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-${ATTESTATION_HUB_CONF:-$ATTESTATION_HUB_HOME/configuration}}
export ATTESTATION_HUB_JAVA=${ATTESTATION_HUB_JAVA:-$ATTESTATION_HUB_HOME/java}
export ATTESTATION_HUB_BIN=${ATTESTATION_HUB_BIN:-$ATTESTATION_HUB_HOME/bin}
export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-$ATTESTATION_HUB_HOME/repository}
export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-$ATTESTATION_HUB_HOME/logs}

# needed for if certain methods are called from attestation-hub.sh like java_detect, etc.
ATTESTATION_HUB_INSTALL_LOG_FILE=${ATTESTATION_HUB_INSTALL_LOG_FILE:-"$ATTESTATION_HUB_LOGS/attestation-hub_install.log"}
export INSTALL_LOG_FILE="$ATTESTATION_HUB_INSTALL_LOG_FILE"

###################################################################################################

# load linux utility
if [ -f "$ATTESTATION_HUB_HOME/bin/functions.sh" ]; then
  . $ATTESTATION_HUB_HOME/bin/functions.sh
fi

###################################################################################################

# stored master password
if [ -z "$ATTESTATION_HUB_PASSWORD" ] && [ -f $ATTESTATION_HUB_CONFIGURATION/.attestation-hub_password ]; then
  export ATTESTATION_HUB_PASSWORD=$(cat $ATTESTATION_HUB_CONFIGURATION/.attestation-hub_password)
fi

# all other variables with defaults
ATTESTATION_HUB_APPLICATION_LOG_FILE=${ATTESTATION_HUB_APPLICATION_LOG_FILE:-$ATTESTATION_HUB_LOGS/attestation-hub.log}
touch "$ATTESTATION_HUB_APPLICATION_LOG_FILE"
chown "$ATTESTATION_HUB_USERNAME":"$ATTESTATION_HUB_USERNAME" "$ATTESTATION_HUB_APPLICATION_LOG_FILE"
chmod 600 "$ATTESTATION_HUB_APPLICATION_LOG_FILE"
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.7}
JAVA_OPTS=${JAVA_OPTS:-"-Dlogback.configurationFile=$ATTESTATION_HUB_CONFIGURATION/logback.xml"}

ATTESTATION_HUB_SETUP_FIRST_TASKS=${ATTESTATION_HUB_SETUP_FIRST_TASKS:-"update-extensions-cache-file"}
ATTESTATION_HUB_SETUP_TASKS=${ATTESTATION_HUB_SETUP_TASKS:-"password-vault jetty-tls-keystore shiro-ssl-port"}

# the standard PID file location /var/run is typically owned by root;
# if we are running as non-root and the standard location isn't writable 
# then we need a different place
ATTESTATION_HUB_PID_FILE=${ATTESTATION_HUB_PID_FILE:-/var/run/attestation-hub.pid}
SCHEDULER_PID_FILE=${SCHEDULER_PID_FILE:-/var/run/scheduler.pid}
if [ ! -w "$ATTESTATION_HUB_PID_FILE" ] && [ ! -w $(dirname "$ATTESTATION_HUB_PID_FILE") ]; then
  ATTESTATION_HUB_PID_FILE=$ATTESTATION_HUB_REPOSITORY/attestation-hub.pid
fi
if [ ! -w "$SCHEDULER_PID_FILE" ] && [ ! -w $(dirname "$SCHEDULER_PID_FILE") ]; then
  SCHEDULER_PID_FILE=$ATTESTATION_HUB_REPOSITORY/scheduler.pid
fi

###################################################################################################

# java command
if [ -z "$JAVA_CMD" ]; then
  if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD=$JAVA_HOME/bin/java
  else
    JAVA_CMD=`which java`
  fi
fi

# generated variables
JARS=$(ls -1 $ATTESTATION_HUB_JAVA/*.jar $ATTESTATION_HUB_HOME/features/*/java/*.jar)
CLASSPATH=$(echo $JARS | tr ' ' ':')

if [ -z "$JAVA_HOME" ]; then java_detect; fi
CLASSPATH=$CLASSPATH:$(find "$JAVA_HOME" -name jfxrt*.jar | head -n 1)

# the classpath is long and if we use the java -cp option we will not be
# able to see the full command line in ps because the output is normally
# truncated at 4096 characters. so we export the classpath to the environment
export CLASSPATH
###################################################################################################

# run a attestation-hub command
attestation_hub_run() {
  local args="$*"
  $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main $args
  return $?
}

# run default set of setup tasks and check if admin user needs to be created
attestation_hub_complete_setup() {
  # run all setup tasks, don't use the force option to avoid clobbering existing
  # useful configuration files
  attestation_hub_run setup $ATTESTATION_HUB_SETUP_FIRST_TASKS
  attestation_hub_run setup $ATTESTATION_HUB_SETUP_TASKS
  
}

# arguments are optional, if provided they are the names of the tasks to run, in order
attestation_hub_setup() {
  local args="$*"
  $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main setup $args
  return $?
}

attestation_hub_start() {
    if [ -z "$ATTESTATION_HUB_PASSWORD" ]; then
      echo_failure "Master password is required; export ATTESTATION_HUB_PASSWORD"
      return 1
    fi

    # check if we're already running - don't start a second instance
    if attestation_hub_is_running; then
      echo "Attestation Hub is running"
      return 0
    fi

    # check if we need to use authbind or if we can start java directly
    prog="$JAVA_CMD"
    if [ -n "$ATTESTATION_HUB_USERNAME" ] && [ "$ATTESTATION_HUB_USERNAME" != "root" ] && [ $(whoami) != "root" ] && [ -n $(which authbind) ]; then
      prog="authbind $JAVA_CMD"
      JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
    fi

    # the subshell allows the java process to have a reasonable current working
    # directory without affecting the user's working directory. 
    # the last background process pid $! must be stored from the subshell.
    (
      cd $ATTESTATION_HUB_HOME
      $prog $JAVA_OPTS com.intel.mtwilson.launcher.console.Main jetty-start >>$ATTESTATION_HUB_APPLICATION_LOG_FILE 2>&1 &      
      echo $! > $ATTESTATION_HUB_PID_FILE
    )
    if attestation_hub_is_running; then
      echo_success "Started Attestation Hub"
    else
      echo_failure "Failed to start Attestation Hub"
    fi
}

# returns 0 if Attestation Hub is running, 1 if not running
# side effects: sets ATTESTATION_HUB_PID if Attestation Hub is running, or to empty otherwise
attestation_hub_is_running() {
  ATTESTATION_HUB_PID=
  if [ -f $ATTESTATION_HUB_PID_FILE ]; then
    ATTESTATION_HUB_PID=$(cat $ATTESTATION_HUB_PID_FILE)
    local is_running=`ps -A -o pid | grep "^\s*${ATTESTATION_HUB_PID}$"`
    if [ -z "$is_running" ]; then
      # stale PID file
      ATTESTATION_HUB_PID=
    fi
  fi
  if [ -z "$ATTESTATION_HUB_PID" ]; then
    # check the process list just in case the pid file is stale
    ATTESTATION_HUB_PID=$(ps -A ww | grep -v grep | grep java | grep "com.intel.mtwilson.launcher.console.Main jetty-start" | grep "$ATTESTATION_HUB_CONFIGURATION" | awk '{ print $1 }')
  fi
  if [ -z "$ATTESTATION_HUB_PID" ]; then
    # Attestation Hub is not running
    return 1
  fi
  # Attestation Hub is running and ATTESTATION_HUB_PID is set
  return 0
}

scheduler_is_running() {
  SCHEDULER_PID=
  if [ -f $SCHEDULER_PID_FILE ]; then
    SCHEDULER_PID=$(cat $SCHEDULER_PID_FILE)
    local is_running=`ps -A -o pid | grep "^\s*${SCHEDULER_PID}$"`
    if [ -z "$is_running" ]; then
      # stale PID file
      SCHEDULER_PID=
    fi
  fi
  if [ -z "$SCHEDULER_PID" ]; then
    # check the process list just in case the pid file is stale
    SCHEDULER_PID=$(ps -A ww | grep -v grep | grep java | grep "com.intel.mtwilson.launcher.console.Main attestation-hub-scheduler"  | grep "$ATTESTATION_HUB_CONFIGURATION" | awk '{ print $1 }')
  fi
  if [ -z "$SCHEDULER_PID" ]; then
    # Scheduler is not running
    return 1
  fi
  # SCHEDULER is running and SCHEDULER_PID is set
  return 0
}



attestation_hub_stop() {
  if attestation_hub_is_running; then
    kill -9 $ATTESTATION_HUB_PID
    if [ $? ]; then
      echo "Stopped Attestation Hub"
      # truncate pid file instead of erasing,
      # because we may not have permission to create it
      # if we're running as a non-root user
      echo > $ATTESTATION_HUB_PID_FILE
    else
      echo "Failed to stop Attestation Hub"
    fi
  fi
}


scheduler_start() {	
 if [ -z "$ATTESTATION_HUB_PASSWORD" ]; then
      echo_failure "Master password is required; export ATTESTATION_HUB_PASSWORD"
      return 1
    fi

    # check if we're already running - don't start a second instance
    if scheduler_is_running; then
      echo "Attestation Hub Scheduler is running"
      return 0
    fi

    # check if we need to use authbind or if we can start java directly
    prog="$JAVA_CMD"
    if [ -n "$ATTESTATION_HUB_USERNAME" ] && [ "$ATTESTATION_HUB_USERNAME" != "root" ] && [ $(whoami) != "root" ] && [ -n $(which authbind) ]; then
      prog="authbind $JAVA_CMD"
      JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
    fi

    # the subshell allows the java process to have a reasonable current working
    # directory without affecting the user's working directory. 
    # the last background process pid $! must be stored from the subshell.
    (
      cd $ATTESTATION_HUB_HOME
      $prog $JAVA_OPTS com.intel.mtwilson.launcher.console.Main attestation-hub-scheduler >>$ATTESTATION_HUB_APPLICATION_LOG_FILE 2>&1 &      
      echo $! > $SCHEDULER_PID_FILE
    )
    if scheduler_is_running; then
      echo_success "Started Attestation Hub Scheduler"
    else
      echo_failure "Failed to start Attestation Hub Scheduler"
    fi


}

scheduler_stop() {	
  if scheduler_is_running; then
    kill -9 $SCHEDULER_PID
    if [ $? ]; then
      echo "Stopped Attestation Hub Scheduled process"
      # truncate pid file instead of erasing,
      # because we may not have permission to create it
      # if we're running as a non-root user
      echo > $SCHEDULER_PID_FILE
    else
      echo "Failed to stop Scheduler"
    fi
  fi
}
# removes Attestation Hub home directory (including configuration and data if they are there).
# if you need to keep those, back them up before calling uninstall,
# or if the configuration and data are outside the home directory
# they will not be removed, so you could configure ATTESTATION_HUB_CONFIGURATION=/etc/attestation-hub
# and ATTESTATION_HUB_REPOSITORY=/var/opt/attestation-hub and then they would not be deleted by this.
attestation_hub_uninstall() {
	attestation_hub_stop
	scheduler_stop
	remove_startup_script attestation-hub


if [ "$2" = "--purge" ]; then
	attestation-hub export-config --in=/opt/attestation-hub/configuration/attestation-hub.properties --out=/opt/attestation-hub/configuration/attestation-hub.properties

	ATTESTATION_HUB_PROPERTIES_FILE=${ATTESTATION_HUB_PROPERTIES_FILE:-"/opt/attestation-hub/configuration/attestation-hub.properties"}
	ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH=`cat ${ATTESTATION_HUB_PROPERTIES_FILE} | grep 'tenant.configuration.path' | cut -d'=' -f2`
	
	
	ATTESTATION_HUB_DB_NAME=`cat ${ATTESTATION_HUB_PROPERTIES_FILE} | grep 'attestation-hub.db.name' | cut -d'=' -f2`
	ATTESTATION_HUB_DB_USER=`cat ${ATTESTATION_HUB_PROPERTIES_FILE} | grep 'attestation-hub.db.username' | cut -d'=' -f2`

	sudo -u postgres psql postgres -c "DROP DATABASE ${ATTESTATION_HUB_DB_NAME}" > /dev/null 2>&1
	sudo -u postgres psql postgres -c "DROP USER ${ATTESTATION_HUB_DB_USER}" > /dev/null 2>&1

	echo "Drop database ${ATTESTATION_HUB_DB_NAME}"
	rm -rf $ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH

fi

rm -f /usr/local/bin/attestation-hub
rm -rf /opt/attestation-hub
groupdel attestation-hub > /dev/null 2>&1
userdel attestation-hub > /dev/null 2>&1



		
  }

print_help() {
    echo "Usage: $0 start|stop|uninstall|uninstall --purge|version"
    echo "Usage: $0 setup [--force|--noexec] [task1 task2 ...]"
    echo "Available setup tasks:"
    echo $ATTESTATION_HUB_SETUP_TASKS | tr ' ' '\n'
}

###################################################################################################

# here we look for specific commands first that we will handle in the
# script, and anything else we send to the java application

case "$1" in
  help)
    print_help
    ;;
  start)
    attestation_hub_start
    scheduler_start
    ;;
  stop)
    attestation_hub_stop
    scheduler_stop
    ;;
  restart)
    attestation_hub_stop
    scheduler_stop
    attestation_hub_start
    scheduler_start
    ;;
  status)
    if attestation_hub_is_running; then
      echo "Attestation Hub is running"
      exit 0
    else
      echo "Attestation Hub is not running"
      exit 1
    fi
    ;;
  setup)
    shift
    if [ -n "$1" ]; then
      attestation_hub_setup $*
    else
      attestation_hub_complete_setup
	fi
    ;;
  uninstall)
    attestation_hub_stop
    attestation_hub_uninstall $*
    ;;
  start-scheduler)
    scheduler_start
    ;;    
  stop-scheduler)
	scheduler_stop
    ;;    
  *)
    if [ -z "$*" ]; then
      print_help
    else
      #echo "args: $*"
      $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main $*
    fi
    ;;
esac


exit $?