# SLR207 : Quick setup

## Getting started

To set up and run the Hadoop MapReduce system, follow these instructions:

### 1. Sequential Counter
The sequential counter is located in the `sequential` folder.

**Usage:** 
```sh
java -jar ./target/sequential-1-jar-with-dependencies.jar <input file>
```

### 2. Slave Node
The slave JAR file is located at `slave/target/slave-1-jar-with-dependencies.jar`.

### 3. Master Node
The master JAR file is located at `master/target/master-1-jar-with-dependencies.jar`.

- The `master/machines.txt` file lists the hostnames of the slave nodes.
- The `master/data` folder stores some data samples used in tests.

### 4. Deployment and Execution
To deploy the slave JAR on the remote hosts, edit the deploy.sh script to add your login and the path to the .jar and then run :
```sh
sh deploy.sh
```

To start the MapReduce procedure, use the following command:
```sh
java -jar ./target/master-1-jar-with-dependencies.jar <hosts file> <input file>
```
where `<hosts file>` is the file listing the hostnames of the slaves, one per line.

**Example (run from the /master directory):**
```sh
java -jar ./target/master-1-jar-with-dependencies.jar machines.txt data/CC-MAIN-20220116093137-20220116123137-00001.warc.wet
```

## Development tips
- Find available machines : https://tp.telecom-paris.fr/

- Build a jar : `mvn clean compile assembly:`

- Trasnfer a jar for testing purpose : `scp ./target/slave-1-jar-with-dependencies.jar <login>@tp-1a201-17.enst.fr:/tmp/<login>`

- Find and kill running processes on Socket and FTP ports :
```sh
lsof -i :3456
lsof -i :9999

kill -9 <PID_du_port_3456>
kill -9 <PID_du_port_9999>
```

- Deploy manually :
```sh
scp ./target/slave-1-jar-with-dependencies.jar elegallic-22@tp-XXXX-XX.enst.fr:/tmp/elegallic-22
ssh elegallic-22@tp-XXXX-XX.enst.fr
cd /tmp/elegallic-22/
java -jar slave-1-jar-with-dependencies.jar
```
