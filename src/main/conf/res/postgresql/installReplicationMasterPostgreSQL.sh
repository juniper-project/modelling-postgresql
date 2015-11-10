#!/bin/bash

if [ ! $# -eq 3 ]
then

echo " need parameter as : <nameUser> <password> <IP_Slave>+ "

else

# http://stackoverflow.com/questions/13778857/permission-denied-when-trying-to-append-a-file-to-a-root-owned-file-with-sudo
#sh -c 'echo "deb http://debian.univ-lorraine.fr/debian-backports/ squeeze-backports main" >> /etc/apt/sources.list'
apt-get -y install lsb-release
apt-get -y update

apt-get -y install rsync

apt-get -y install postgresql-9.4 postgresql-contrib-9.4

bash ./script_ssh.sh

chown postgres:postgres .
chown postgres:postgres ./*

sudo -u postgres -s psql -c "CREATE USER $1 REPLICATION LOGIN CONNECTION LIMIT 1 ENCRYPTED PASSWORD '$2';"

sudo -u postgres -s sh -c "cat pg_hba.conf > /etc/postgresql/9.4/main/pg_hba.conf"
sudo -u postgres -s sh -c "cat postgresql.conf > /etc/postgresql/9.4/main/postgresql.conf"

service postgresql restart

echo shift 2
shift 2
for ip in $*
do

cd /var/lib/postgresql

sudo -u postgres -s psql -c "select pg_start_backup('initial_backup');"
sudo -u postgres -s rsync -e "ssh -o StrictHostKeyChecking=no" -cva --inplace --exclude=*pg_xlog* /var/lib/postgresql/9.4/main/ $ip:/var/lib/postgresql/9.4/main/
sudo -u postgres -s psql -c "select pg_stop_backup();"

cd -
done
fi
