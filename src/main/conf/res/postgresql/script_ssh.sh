#!/bin/bash

home="/var/lib/postgresql"

mkdir $home/.ssh

chmod 700 $home/.ssh

chown postgres:postgres $home/.ssh

sh -c "cat id_rsa > $home/.ssh/id_rsa"

sh -c "cat id_rsa.pub > $home/.ssh/id_rsa.pub"

chown postgres:postgres $home/.ssh/*

chmod 700 $home/.ssh/*
 
sh -c "cat $home/.ssh/id_rsa.pub > $home/.ssh/authorized_keys"

chown postgres:postgres $home/.ssh/authorized_keys


