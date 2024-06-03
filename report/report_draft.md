# Hadoop Map Reduce from scratch

## Introduction


Explain quickly the Hadoop Map Reduce algorithm where :
- I have a Master that controls several Slaves
- The master takes as an input a text file to reduce as well as a list of the hostnames corresponding to the computers acting as slaves, and outputs the sorted list of occurence of each word in the input file.

We have 4 phases :
1) The master splits the file into several chunk of size (file_size)/n where n is the number of slaves and sends those splits to the slaves.
2) Each slaves executes the following steps :
a. Mapping : taking a chunk of the initial input file sent by the master and produce a text fil with each word of the chunk file in a newline.
ex : 
[chunk file] 
"The cat is black"

[mapping output] 
"
the
cat
is
black
"

b. Shuffling 1 : for each word of the previous mapping phase output file, the slave computes, thanks to the words hash, an id in between 1 and n where n is the number of slaves. The slave creates then several files containing the words associated to a specific slave id and intended for slave with this specific id. The slave then sends each of those files to the corresponding intended slaves, called then reducers.

c. Reducing 1 : for each shuffling files received by the others slaves, a slave computes a hash map of the words from its intended received shuffling files, and counting the occurrences of each words present in those files. Then, the slave sends the minimum and maximum occurrences from this hash map to the master.

The master then computes reducing groups. After collecting minimum and maximum occurrences from all the slaves, it computes the global minimum (min_global) and maximum (max_global) and then creates n reducing groups responsible for reducing the words with occurences in [min_global + i * (max_global-min_global)/n; min_global + (i+1) * (max_global-min_global)/n] for i in {0,...,n}.

d. Shuffling 2 : the slave receives the group and the associated ranges and then takes the reducing 1 phase hashmaps and creates files for each group containing words from the hash map whose occurences corresponds to the group's associated range. It then sends those files to the slaves associated to those specific group ids.

c. Reducing 2 : the slave takes all the files received from the remaining slaves from the shuffling 2 phase and starts sorting them according to their occurences. It then sends back the result to the master

The master then receives all the sorted parts of each groups associated with the corresponding ranges and just builds the result by aggregating the differents groups outputs.


Using those two codes of the Master.java file, CommunicationHandler.java and SocketThread.java explains quickly the purpose of each class and how they work jointly. Also details some implementations points such as how the file is first splitted with respect to the spaces in order to avoid to cut words. Also explains which protocols are used for each part (ex: FTP for file transfer, sockets for communications, ...)

