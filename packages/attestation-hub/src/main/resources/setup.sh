#!/bin/bash

# Attestation Hub install script
# Outline:
# 1. source the "functions.sh" file:  mtwilson-linux-util-3.0-SNAPSHOT.sh
# 2. load existing environment configuration
# 3. look for ~/attestation-hub.env and source it if it's there
# 4. prompt for installation variables if they are not provided
# 5. determine if we are installing as root or non-root user; set paths
# 6. detect java
# 7. if java not installed, and we have it bundled, install it
# 8. unzip attestation-hub archive attestation-hub-zip-0.1-SNAPSHOT.zip into /opt/attestation-hub, overwrite if any files already exist
# 9. link /usr/local/bin/attestation-hub -> /opt/attestation-hub/bin/attestation-hub, if not already there
# 10. add attestation-hub to startup services
# 11. look for ATTESTATION_HUB_PASSWORD environment variable; if not present print help message and exit:
#     Attestation Hub requires a master password
#     losing the master password will result in data loss
# 12. attestation-hub setup
# 13. attestation-hub start

#####

# default settings
# note the layout setting is used only by this script
# and it is not saved or used by the app script
export ATTESTATION_HUB_HOME=${ATTESTATION_HUB_HOME:-/opt/attestation-hub}
ATTESTATION_HUB_LAYOUT=${ATTESTATION_HUB_LAYOUT:-home}

# the env directory is not configurable; it is defined as ATTESTATION_HUB_HOME/env and
# the administrator may use a symlink if necessary to place it anywhere else
export ATTESTATION_HUB_ENV=$ATTESTATION_HUB_HOME/env

# load application environment variables if already defined

EXTENSIONS_CACHE_FILE=$ATTESTATION_HUB_HOME/configuration/extensions.cache
echo "EXTENSIONS_CACHE_FILE:: $EXTENSIONS_CACHE_FILE"
if [ -f $EXTENSIONS_CACHE_FILE ] ; then
echo "removing existing extension cache file"
    rm -rf $EXTENSIONS_CACHE_FILE
fi

if [ -d $ATTESTATION_HUB_ENV ]; then
  ATTESTATION_HUB_ENV_FILES=$(ls -1 $ATTESTATION_HUB_ENV/*)
  for env_file in $ATTESTATION_HUB_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi
# functions script (mtwilson-linux-util-3.0-SNAPSHOT.sh) is required
# we use the following functions:
# java_detect java_ready_report 
# echo_failure echo_warning
# register_startup_script
UTIL_SCRIPT_FILE=$(ls -1 mtwilson-linux-util-*.sh | head -n 1)
if [ -n "$UTIL_SCRIPT_FILE" ] && [ -f "$UTIL_SCRIPT_FILE" ]; then
  . $UTIL_SCRIPT_FILE
fi

ATTESTATION_HUB_UTIL_SCRIPT_FILE=$(ls -1 attestation-hub-functions.sh | head -n 1)
if [ -n "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ] && [ -f "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ]; then
  . $ATTESTATION_HUB_UTIL_SCRIPT_FILE
fi

# load installer environment file, if present
if [ -f ~/attestation-hub.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/attestation-hub.env"
  . ~/attestation-hub.env
  env_file_exports=$(cat ~/attestation-hub.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi

# determine if we are installing as root or non-root
if [ "$(whoami)" == "root" ]; then
  # create a attestation-hub user if there isn't already one created
  ATTESTATION_HUB_USERNAME=${ATTESTATION_HUB_USERNAME:-attestation-hub}
  if ! getent passwd $ATTESTATION_HUB_USERNAME 2>&1 >/dev/null; then
    useradd --comment "Mt Wilson Attestation Hub" --home $ATTESTATION_HUB_HOME --system --shell /bin/false $ATTESTATION_HUB_USERNAME
    usermod --lock $ATTESTATION_HUB_USERNAME
    # note: to assign a shell and allow login you can run "usermod --shell /bin/bash --unlock $ATTESTATION_HUB_USERNAME"
  fi
else
  # already running as attestation-hub user
  ATTESTATION_HUB_USERNAME=$(whoami)
  echo_warning "Running as $ATTESTATION_HUB_USERNAME; if installation fails try again as root"
  if [ ! -w "$ATTESTATION_HUB_HOME" ] && [ ! -w $(dirname $ATTESTATION_HUB_HOME) ]; then
    export ATTESTATION_HUB_HOME=$(cd ~ && pwd)
  fi
fi

# if an existing attestation-hub is already running, stop it while we install
if which attestation-hub; then
  attestation-hub stop
fi

# define application directory layout
if [ "$ATTESTATION_HUB_LAYOUT" == "linux" ]; then
  export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-/etc/attestation-hub}
  export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-/var/opt/attestation-hub}
  export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-/var/log/attestation-hub}
elif [ "$ATTESTATION_HUB_LAYOUT" == "home" ]; then
  export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-$ATTESTATION_HUB_HOME/configuration}
  export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-$ATTESTATION_HUB_HOME/repository}
  export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-$ATTESTATION_HUB_HOME/logs}
fi
export ATTESTATION_HUB_BIN=$ATTESTATION_HUB_HOME/bin
export ATTESTATION_HUB_JAVA=$ATTESTATION_HUB_HOME/java

# note that the env dir is not configurable; it is defined as "env" under home
export ATTESTATION_HUB_ENV=$ATTESTATION_HUB_HOME/env

attestation_hub_backup_configuration() {
  if [ -n "$ATTESTATION_HUB_CONFIGURATION" ] && [ -d "$ATTESTATION_HUB_CONFIGURATION" ]; then
    datestr=`date +%Y%m%d.%H%M`
	mkdir -p /var/backup/
    backupdir=/var/backup/attestation-hub.configuration.$datestr
    cp -r $ATTESTATION_HUB_CONFIGURATION $backupdir
  fi
}

attestation_hub_backup_repository() {
  if [ -n "$ATTESTATION_HUB_REPOSITORY" ] && [ -d "$ATTESTATION_HUB_REPOSITORY" ]; then
	mkdir -p /var/backup/
    datestr=`date +%Y%m%d.%H%M`
    backupdir=/var/backup/attestation-hub.repository.$datestr
    cp -r $ATTESTATION_HUB_REPOSITORY $backupdir
  fi
}

# backup current configuration and data, if they exist
attestation_hub_backup_configuration
attestation_hub_backup_repository

if [ -d $ATTESTATION_HUB_CONFIGURATION ]; then
  backup_conf_dir=$ATTESTATION_HUB_REPOSITORY/backup/configuration.$(date +"%Y%m%d.%H%M")
  mkdir -p $backup_conf_dir
  cp -R $ATTESTATION_HUB_CONFIGURATION/* $backup_conf_dir
fi

# create application directories (chown will be repeated near end of this script, after setup)
for directory in $ATTESTATION_HUB_HOME $ATTESTATION_HUB_CONFIGURATION $ATTESTATION_HUB_ENV $ATTESTATION_HUB_REPOSITORY $ATTESTATION_HUB_LOGS; do
  mkdir -p $directory
  chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $directory
  chmod 700 $directory
done

# store directory layout in env file
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_HOME=$ATTESTATION_HUB_HOME" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_CONFIGURATION=$ATTESTATION_HUB_CONFIGURATION" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_REPOSITORY=$ATTESTATION_HUB_REPOSITORY" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_JAVA=$ATTESTATION_HUB_JAVA" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_BIN=$ATTESTATION_HUB_BIN" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_LOGS=$ATTESTATION_HUB_LOGS" >> $ATTESTATION_HUB_ENV/attestation-hub-layout

# store attestation-hub username in env file
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-username
echo "export ATTESTATION_HUB_USERNAME=$ATTESTATION_HUB_USERNAME" >> $ATTESTATION_HUB_ENV/attestation-hub-username

# store the auto-exported environment variables in env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name=$env_file_var_value" >> $ATTESTATION_HUB_ENV/attestation-hub-setup
done


# if properties file exists
ATTESTATION_HUB_PROPERTIES_FILE=${ATTESTATION_HUB_PROPERTIES_FILE:-"$ATTESTATION_HUB_CONFIGURATION/attestation-hub.properties"}

if [ -e $ATTESTATION_HUB_PROPERTIES_FILE ]; then
	attestation-hub export-config --in=/opt/attestation-hub/configuration/attestation-hub.properties --out=/opt/attestation-hub/configuration/attestation-hub.properties
fi




touch "$ATTESTATION_HUB_PROPERTIES_FILE"
chown "$ATTESTATION_HUB_USERNAME":"$ATTESTATION_HUB_USERNAME" "$ATTESTATION_HUB_PROPERTIES_FILE"
chmod 600 "$ATTESTATION_HUB_PROPERTIES_FILE"


ATTESTATION_HUB_INSTALL_LOG_FILE=${ATTESTATION_HUB_INSTALL_LOG_FILE:-"$ATTESTATION_HUB_LOGS/attestation-hub_install.log"}
export INSTALL_LOG_FILE="$ATTESTATION_HUB_INSTALL_LOG_FILE"
touch "$ATTESTATION_HUB_INSTALL_LOG_FILE"
chown "$ATTESTATION_HUB_USERNAME":"$ATTESTATION_HUB_USERNAME" "$ATTESTATION_HUB_INSTALL_LOG_FILE"
chmod 600 "$ATTESTATION_HUB_INSTALL_LOG_FILE"

echo "install log file is" $INSTALL_LOG_FILE

# load existing environment; set variables will take precendence
load_attestation_hub_conf
load_attestation_hub_defaults




#-------------------


#required database properties

prompt_with_default ATTESTATION_HUB_DB_NAME "Attestation-Hub db name:" "$ATTESTATION_HUB_DB_NAME"
update_property_in_file "attestation-hub.db.name" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_NAME"

prompt_with_default ATTESTATION_HUB_DB_HOSTNAME "Attestation-Hub db Hostname:" "$ATTESTATION_HUB_DB_HOSTNAME"
update_property_in_file "attestation-hub.db.hostname" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_HOSTNAME"

prompt_with_default ATTESTATION_HUB_DB_PORTNUM "Attestation-Hub db Portno:" "$ATTESTATION_HUB_DB_PORTNUM"
update_property_in_file "attestation-hub.db.portnum" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_PORTNUM"

prompt_with_default ATTESTATION_HUB_DB_USERNAME "Attestation-Hub db username:" "$ATTESTATION_HUB_DB_USERNAME"
update_property_in_file "attestation-hub.db.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_USERNAME"

prompt_with_default ATTESTATION_HUB_DB_PASSWORD "Attestation-Hub db password:" "$ATTESTATION_HUB_DB_PASSWORD"
update_property_in_file "attestation-hub.db.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_PASSWORD"

prompt_with_default ATTESTATION_HUB_DB_DRIVER "Attestation-Hub db driver:" "$ATTESTATION_HUB_DB_DRIVER"
update_property_in_file "attestation-hub.db.driver" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_DRIVER"


export ATTESTATION_HUB_DB_URL="jdbc:postgresql://${ATTESTATION_HUB_DB_HOSTNAME}:${ATTESTATION_HUB_DB_PORTNUM}/${ATTESTATION_HUB_DB_NAME}"
update_property_in_file "attestation-hub.db.url" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_URL"

export POSTGRES_HOSTNAME=${ATTESTATION_HUB_DB_HOSTNAME}
export POSTGRES_PORTNUM=${ATTESTATION_HUB_DB_PORTNUM}
export POSTGRES_DATABASE=${ATTESTATION_HUB_DB_NAME}
export POSTGRES_USERNAME=${ATTESTATION_HUB_DB_USERNAME}
export POSTGRES_PASSWORD=${ATTESTATION_HUB_DB_PASSWORD}


# modifying after mtwilson api client built
prompt_with_default MTWILSON_SERVER "Mtwilson Server:" "$MTWILSON_SERVER"
update_property_in_file "mtwilson.server" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER"
prompt_with_default MTWILSON_SERVER_PORT "Mtwilson Server Port:" "$MTWILSON_SERVER_PORT"
update_property_in_file "mtwilson.server.port" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER_PORT"
prompt_with_default MTWILSON_USERNAME "Mtwilson Username:" "$MTWILSON_USERNAME"
update_property_in_file "mtwilson.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_USERNAME"
prompt_with_default_password MTWILSON_PASSWORD "Mtwilson Password:" "$MTWILSON_PASSWORD"
update_property_in_file "mtwilson.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_PASSWORD"



prompt_with_default MTWILSON_API_URL "Mtwilson API Url:" "$MTWILSON_API_URL"
update_property_in_file "mtwilson.api.url" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_API_URL"
update_property_in_file "mtwilson.api.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_USERNAME"
update_property_in_file "mtwilson.api.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_PASSWORD"
prompt_with_default MTWILSON_TLS "Mtwilson TLS:" "$MTWILSON_TLS"
update_property_in_file "mtwilson.api.tls.policy.certificate.sha1" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_TLS"

prompt_with_default ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH "Tenant Configurations Path:" "$ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH"
update_property_in_file "tenant.configuration.path" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH"

prompt_with_default ATTESTATION_HUB_POLL_INTERVAL "Attestation Hub Poll Interval:" "$ATTESTATION_HUB_POLL_INTERVAL"
update_property_in_file "attestation-hub.poll.interval" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_POLL_INTERVAL"

update_property_in_file "attestation-hub.saml.timeout" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_SAML_TIMEOUT"


mkdir -p $ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH



# attestation-hub requires java 1.7 or later
# detect or install java (jdk-1.7.0_51-linux-x64.tar.gz)
echo "Installing Java..."
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.7}
JAVA_PACKAGE=`ls -1 jdk-* jre-* java-*.bin 2>/dev/null | tail -n 1`
# check if java is readable to the non-root user
if [ -z "$JAVA_HOME" ]; then
  java_detect > /dev/null
fi
if [ -n "$JAVA_HOME" ]; then
  if [ $(whoami) == "root" ]; then
    JAVA_USER_READABLE=$(sudo -u $ATTESTATION_HUB_USERNAME /bin/bash -c "if [ -r $JAVA_HOME ]; then echo 'yes'; fi")
  else
    JAVA_USER_READABLE=$(/bin/bash -c "if [ -r $JAVA_HOME ]; then echo 'yes'; fi")
  fi
fi
if [ -z "$JAVA_HOME" ] || [ -z "$JAVA_USER_READABLE" ]; then
  JAVA_HOME=$ATTESTATION_HUB_HOME/share/jdk1.7.0_79
fi
mkdir -p $JAVA_HOME
java_install_in_home $JAVA_PACKAGE
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-java
echo "export JAVA_HOME=$JAVA_HOME" >> $ATTESTATION_HUB_ENV/attestation-hub-java
echo "export JAVA_CMD=$JAVA_HOME/bin/java" >> $ATTESTATION_HUB_ENV/attestation-hub-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> $ATTESTATION_HUB_ENV/attestation-hub-java

# libguestfs packages has a custom prompt about installing supermin which ignores the “-y” option we provide to apt-get. Following code will help to avoid that prompt 
#export DEBIAN_FRONTEND=noninteractive
#echo libguestfs-tools libguestfs/update-appliance boolean true | debconf-set-selections

# make sure unzip and authbind are installed
ATTESTATION_HUB_YUM_PACKAGES="zip unzip authbind"
ATTESTATION_HUB_APT_PACKAGES="zip  unzip authbind"
ATTESTATION_HUB_YAST_PACKAGES="zip unzip authbind"
ATTESTATION_HUB_ZYPPER_PACKAGES="zip  unzip authbind"
auto_install "Installer requirements" "ATTESTATION_HUB"
if [ $? -ne 0 ]; then echo_failure "Failed to install prerequisites through package installer"; exit -1; fi


export postgres_required_version=${POSTGRES_REQUIRED_VERSION:-9.3}

 postgres_write_connection_properties "$ATTESTATION_HUB_CONFIGURATION/attestation-hub.properties" attestation-hub.db
 

 
  postgres_installed=1
  touch ${ATTESTATION_HUB_HOME}/.pgpass
  chmod 0600 ${ATTESTATION_HUB_HOME}/.pgpass
  chown ${ATTESTATION_HUB_USERNAME}:${ATTESTATION_HUB_USERNAME} ${ATTESTATION_HUB_HOME}/.pgpass
  export POSTGRES_HOSTNAME POSTGRES_PORTNUM POSTGRES_DATABASE POSTGRES_USERNAME POSTGRES_PASSWORD
  if [ "$POSTGRES_HOSTNAME" == "127.0.0.1" ] || [ "$POSTGRES_HOSTNAME" == "localhost" ]; then
    PGPASS_HOSTNAME=localhost
  else
    PGPASS_HOSTNAME="$POSTGRES_HOSTNAME"
  fi
  echo "$POSTGRES_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" > ${ATTESTATION_HUB_HOME}/.pgpass
  echo "$PGPASS_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" >> ${ATTESTATION_HUB_HOME}/.pgpass
  if [ $(whoami) == "root" ]; then cp ${ATTESTATION_HUB_HOME}/.pgpass ~/.pgpass; fi
 

 if [ "$(whoami)" == "root" ]; then

    # Copy the www.postgresql.org PGP public key so add_postgresql_install_packages can add it later if needed
    if [ -d "/etc/apt" ]; then
      mkdir -p /etc/apt/trusted.gpg.d
      chmod 755 /etc/apt/trusted.gpg.d
      if [ -e ACCC4CF8.asc ]; then
	cp ACCC4CF8.asc "/etc/apt/trusted.gpg.d"
      fi
      POSTGRES_SERVER_APT_PACKAGES="postgresql-9.3"
      add_postgresql_install_packages "POSTGRES_SERVER"
    fi
	echo "opt postgres value is:: $opt_postgres"
         if [[ "$POSTGRES_HOSTNAME" == "127.0.0.1" || "$POSTGRES_HOSTNAME" == "localhost" || -n `echo "$(hostaddress_list)" | grep "$POSTGRES_HOSTNAME"` ]]; then
        echo "Installing postgres server..."
        # when we install postgres server on ubuntu it prompts us for root pw
        # we preset it so we can send all output to the log
        aptget_detect; dpkg_detect; yum_detect;
        if [[ -n "$aptget" ]]; then
          echo "postgresql app-pass password $POSTGRES_PASSWORD" | debconf-set-selections 
        fi
        postgres_installed=0 #postgres is being installed
        # don't need to restart postgres server unless the install script says we need to (by returning zero)
        if postgres_server_install; then
          postgres_restart >> $INSTALL_LOG_FILE
          sleep 10
        fi
        # postgres server end
      fi 
      # postgres client install here
      echo "Installing postgres client..."
      postgres_install
      # do not need to restart postgres server after installing the client.
      #postgres_restart >> $INSTALL_LOG_FILE
      #sleep 10
      echo "Installation of postgres client complete" 
      # postgres client install end
    
  
  fi
  
if [ -z "$SKIP_DATABASE_INIT" ]; then
    # postgres db init here
	echo "Creating logs for mtwilson"
	mkdir -p /opt/mtwilson/logs
	chmod 777 /opt/mtwilson/*
	
    postgres_create_database
    if [ $? -ne 0 ]; then
      echo_failure "Cannot create database"
      exit 1
    fi
    #postgres_restart >> $INSTALL_LOG_FILE
    #sleep 10
    #export is_postgres_available postgres_connection_error
    if [ -z "$is_postgres_available" ]; then
      echo_warning "Run 'attestation-hub setup' after a database is available"; 
    fi
    # postgress db init end
  else
    echo_warning "Skipping init of database"
  fi
  if [ $postgres_installed -eq 0 ]; then
    postgres_server_detect
    has_local_peer=`grep "^local.*all.*all.*peer" $postgres_pghb_conf`
    if [ -n "$has_local_peer" ]; then
      echo "Replacing PostgreSQL local 'peer' authentication with 'password' authentication..."
      sed -i 's/^local.*all.*all.*peer/local all all password/' $postgres_pghb_conf
    fi
    #if [ "$POSTGRESQL_KEEP_PGPASS" != "true" ]; then   # Use this line after 2.0 GA, and verify compatibility with other commands
    if [ "${POSTGRESQL_KEEP_PGPASS:-true}" == "false" ]; then
      if [ -f ${ATTESTATION_HUB_HOME}/.pgpass ]; then
        echo "Removing .pgpass file to prevent insecure database password storage in plaintext..."
        rm -f ${ATTESTATION_HUB_HOME}/.pgpass
        if [ $(whoami) == "root" ]; then rm -f ~/.pgpass; fi
      fi
    fi
  fi
 
 
 
ATTESTATION_HUB_PORT_HTTP=${ATTESTATION_HUB_PORT_HTTP:-${JETTY_PORT:-80}}
ATTESTATION_HUB_PORT_HTTPS=${ATTESTATION_HUB_PORT_HTTPS:-${JETTY_SECURE_PORT:-443}}
# setup authbind to allow non-root director to listen on ports 80 and 443
if [ -n "$ATTESTATION_HUB_USERNAME" ] && [ "$ATTESTATION_HUB_USERNAME" != "root" ] && [ -d /etc/authbind/byport ] && [ "$ATTESTATION_HUB_PORT_HTTP" -lt "1024" ]; then
  touch /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTP
  chmod 500 /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTP
  chown $ATTESTATION_HUB_USERNAME /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTP
fi
if [ -n "$ATTESTATION_HUB_USERNAME" ] && [ "$ATTESTATION_HUB_USERNAME" != "root" ] && [ -d /etc/authbind/byport ] && [ "$ATTESTATION_HUB_PORT_HTTPS" -lt "1024" ]; then
  touch /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTPS
  chmod 500 /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTPS
  chown $ATTESTATION_HUB_USERNAME /etc/authbind/byport/$ATTESTATION_HUB_PORT_HTTPS
fi

# delete existing java files, to prevent a situation where the installer copies
# a newer file but the older file is also there
if [ -d $ATTESTATION_HUB_HOME/java ]; then
  rm $ATTESTATION_HUB_HOME/java/*.jar
fi

# extract attestation-hub  (attestation-hub-zip-0.1-SNAPSHOT.zip)
echo "Extracting application..."
ATTESTATION_HUB_ZIPFILE=`ls -1 attestation-hub-*.zip 2>/dev/null | head -n 1`
unzip -oq $ATTESTATION_HUB_ZIPFILE -d $ATTESTATION_HUB_HOME

# copy utilities script file to application folder
cp $UTIL_SCRIPT_FILE $ATTESTATION_HUB_HOME/bin/functions.sh

# set permissions
chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $ATTESTATION_HUB_HOME
chmod 755 $ATTESTATION_HUB_HOME/bin/*

# link /usr/local/bin/attestation-hub -> /opt/attestation-hub/bin/attestation-hub
EXISTING_ATTESTATION_HUB_COMMAND=`which attestation-hub`
if [ -z "$EXISTING_ATTESTATION_HUB_COMMAND" ]; then
  ln -s $ATTESTATION_HUB_HOME/bin/attestation-hub.sh /usr/local/bin/attestation-hub
fi


 



# register linux startup script
register_startup_script $ATTESTATION_HUB_HOME/bin/attestation-hub.sh attestation-hub

disable_tcp_timestamps

# setup the attestation-hub, unless the NOSETUP variable is defined
if [ -z "$ATTESTATION_HUB_NOSETUP" ]; then
  # the master password is required
  if [ -z "$ATTESTATION_HUB_PASSWORD" ] && [ ! -f $ATTESTATION_HUB_CONFIGURATION/.attestation-hub_password ]; then
    attestation-hub generate-password > $ATTESTATION_HUB_CONFIGURATION/.attestation-hub_password
  fi

  attestation-hub config mtwilson.extensions.fileIncludeFilter.contains "${MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS:-mtwilson,attestation-hub}" >/dev/null
  attestation-hub config mtwilson.extensions.packageIncludeFilter.startsWith "${MTWILSON_EXTENSIONS_PACKAGEINCLUDEFILTER_STARTSWITH:-com.intel,org.glassfish.jersey.media.multipart}" >/dev/null


  attestation-hub config jetty.port $ATTESTATION_HUB_PORT_HTTP >/dev/null
  attestation-hub config jetty.secure.port $ATTESTATION_HUB_PORT_HTTPS >/dev/null

  attestation-hub setup
fi


attestation-hub setup apply-database-patches

# delete the temporary setup environment variables file
rm -f $ATTESTATION_HUB_ENV/attestation-hub-setup

# ensure the attestation-hub owns all the content created during setup
for directory in $ATTESTATION_HUB_HOME $ATTESTATION_HUB_CONFIGURATION $ATTESTATION_HUB_JAVA $ATTESTATION_HUB_BIN $ATTESTATION_HUB_ENV $ATTESTATION_HUB_REPOSITORY $ATTESTATION_HUB_LOGS; do
  chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $directory
done

attestation-hub import-config --in=/opt/attestation-hub/configuration/attestation-hub.properties --out=/opt/attestation-hub/configuration/attestation-hub.properties

# start the server, unless the NOSETUP variable is defined
if [ -z "$ATTESTATION_HUB_NOSETUP" ]; then attestation-hub start; fi
echo_success "Installation complete"