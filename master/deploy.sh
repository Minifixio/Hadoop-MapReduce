#!/bin/bash
login="elegallic-22"
localFolder="../"
todeploy="slave/target/"
remoteFolder="/dev/shm/$login/"
nameOfTheJarToExecute="slave-1-jar-with-dependencies.jar"
computers=($(cat machines.txt))

for c in ${computers[@]}; do
  command0=("ssh" "$login@$c" "lsof -ti | xargs kill -9 2>/dev/null; rm -rf $remoteFolder;mkdir $remoteFolder")
  command1=("scp" "$localFolder$todeploy$nameOfTheJarToExecute" "$login@$c:$remoteFolder")
  command2=("ssh" "-tt" "$login@$c" "cd $remoteFolder; java -jar $nameOfTheJarToExecute")
  echo ${command0[*]}
  "${command0[@]}"
  echo ${command1[*]}
  "${command1[@]}"
  echo ${command2[*]}
  "${command2[@]}" &
done