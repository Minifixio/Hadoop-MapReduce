#!/bin/bash
login="elegallic-22"
localFolder="./"
todeploy="folderToDeploy"
remoteFolder="/dev/shm/$login/"
nameOfTheJarToExecute="Server.jar"
#create a machines.txt file with the list of computers
computers=($(cat machines.txt))
#computers=("tp-1a226-01" "tp-1a226-02" "tp-1a226-03" "tp-1a226-04" "tp-1a226-05")

for c in ${computers[@]}; do
  #this command is used to kill all the user processes (in case the program is already running)
  #then it removes the remote folder and creates a new one
  command0=("ssh" "$login@$c" "lsof -ti | xargs kill -9 2>/dev/null; rm -rf $remoteFolder;mkdir $remoteFolder")
  #this command copies the folder to the remote folder
  command1=("scp" "-r" "$localFolder$folderName" "$login@$c:$remoteFolder")
  #this command goes to the remote folder, waits 3 seconds and executes the jar
  command2=("ssh" "-tt" "$login@$c" "cd $remoteFolder$folderName;sleep 3; java -jar $remoteFolder$folderName/$nameOfTheJarToExecute")
  echo ${command0[*]}
  "${command0[@]}"
  echo ${command1[*]}
  "${command1[@]}"
  echo ${command2[*]}
  "${command2[@]}" &
done