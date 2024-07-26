#!/bin/bash

##############################################
# EDIT THIS FILE TO MATCH YOUR CONFIGURATION #
##############################################

login="elegallic-22" # Change this to your login
toDeploy="../slave/target/" # Change this to the folder where the jar is located
remoteFolder="/dev/shm/$login/"
nameOfTheJarToExecute="slave-1-jar-with-dependencies.jar"

# The machines.txt file should contain the list of computers to deploy on (one per line)
# The machines.txt file should be the same as the one used in the master
# The deploy script should be in the same folder as the master folder, so that the following path is correct :
computers=($(cat ./master/machines.txt)) 


for c in ${computers[@]}; do
  command0=("ssh" "$login@$c" "lsof -ti | xargs kill -9 2>/dev/null; rm -rf $remoteFolder;mkdir $remoteFolder")
  command1=("scp" "$toDeploy$nameOfTheJarToExecute" "$login@$c:$remoteFolder")
  command2=("ssh" "-tt" "$login@$c" "cd $remoteFolder; java -jar $nameOfTheJarToExecute")
  echo ${command0[*]}
  "${command0[@]}"
  echo ${command1[*]}
  "${command1[@]}"
  echo ${command2[*]}
  "${command2[@]}" &
done