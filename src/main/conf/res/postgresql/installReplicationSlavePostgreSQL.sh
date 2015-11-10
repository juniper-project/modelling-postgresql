#!/bin/bash


# http://stackoverflow.com/questions/13778857/permission-denied-when-trying-to-append-a-file-to-a-root-owned-file-with-sudo
#sh -c 'echo "deb http://debian.univ-lorraine.fr/debian-backports/ squeeze-backports main" >> /etc/apt/sources.list'
apt-get -y install lsb-release
apt-get -y update
apt-get -y install rsync
apt-get -y install postgresql-9.4 postgresql-contrib-9.4

service postgresql stop

bash ./script_ssh.sh

chown postgres:postgres .
chown postgres:postgres ./*

sudo -u postgres -s sh -c "cat pg_hba.conf > /etc/postgresql/9.4/main/pg_hba.conf"

sudo -u postgres -s sh -c "cat postgresql.conf > /etc/postgresql/9.4/main/postgresql.conf"

sudo -u postgres -s sh -c "cat recovery.conf > /var/lib/postgresql/9.4/main/recovery.conf"


