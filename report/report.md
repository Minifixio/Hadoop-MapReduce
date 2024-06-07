# Hadoop Map Reduce from scratch

# Introduction

## Explanation of the Hadoop MapReduce Algorithm

In the Hadoop MapReduce algorithm:

- A Master node controls several Slave nodes.
- The Master node takes a text file as input and a list of hostnames corresponding to the Slave nodes. It outputs a sorted list of word occurrences in the input file.

The process involves four phases:

1. **Splitting**:
   - The Master splits the file into several chunks, each of size (file_size)/n, where n is the number of Slaves. It then sends these chunks to the Slaves.

2. **Processing by Slaves**:
   - Each Slave executes the following steps:

     a. **Mapping**:
        - Each Slave takes its assigned chunk of the initial input file and produces a text file where each word from the chunk is on a new line.
        - Example:
          - [Chunk File]: "The cat is black"
          - [Mapping Output]:
            ```
            the
            cat
            is
            black
            ```

     b. **Shuffling 1**:
        - For each word in the Mapping output file, the Slave computes an ID based on the word's hash, ranging between 1 and n (where n is the number of Slaves). 
        - The Slave creates several files, each containing words associated with a specific Slave ID, and sends these files to the corresponding Slaves (called Reducers).

     c. **Reducing 1**:
        - Each Slave processes the shuffling files received from other Slaves, creating a hash map of word occurrences from these files.
        - The Slave then sends the minimum and maximum occurrences from its hash map to the Master.

3. **Group Reduction**:
   - The Master computes reducing groups after collecting minimum and maximum occurrences from all Slaves.
   - It determines the global minimum (min_global) and maximum (max_global) and creates n reducing groups, each responsible for a range of word occurrences.
   - The range for each group i is defined as [min_global + i * (max_global - min_global)/n; min_global + (i + 1) * (max_global - min_global)/n], for i in {0,...,n}.

4. **Final Processing by Slaves**:
   - Each Slave performs the following steps:

     a. **Shuffling 2**:
        - Each Slave receives its assigned group and associated range, then processes the hash maps from the Reducing 1 phase.
        - It creates files for each group containing words from the hash map whose occurrences fall within the group's range, and sends these files to the Slaves associated with those group IDs.

     b. **Reducing 2**:
        - Each Slave receives the files from other Slaves (from the Shuffling 2 phase) and sorts the words according to their occurrences.
        - It then sends the sorted result back to the Master.

5. **Aggregation**:
   - The Master receives all the sorted parts from each group and aggregates the results to build the final sorted list of word occurrences.




## Implementation details

## `Master.java`
The `Master` class orchestrates the entire MapReduce process, managing the communication with slave nodes, distributing work, and coordinating different phases of the MapReduce job (map, shuffle, reduce). It initializes communication handlers, splits the input file, sends tasks to slaves, and aggregates the final results.

**Key Responsibilities:**
- **Reading Slave Hostnames:** It reads the hostnames of the slave nodes from a file.
- **Splitting the File:** The `splitAndSendChunks` method splits the input file into chunks based on the number of slaves, ensuring that words are not split between chunks.
- **Sending Tasks to Slaves:** It uses `CommunicationHandler` to send file chunks and protocol messages to slave nodes.
- **Managing MapReduce Phases:** It transitions between different MapReduce phases (map, shuffle, reduce) by sending corresponding commands to the slaves.
- **Aggregating Results:** It gathers the results from slaves and compiles them into a final output file.

#### CommunicationHandler.java
The `CommunicationHandler` class handles the communication between the `Master` and each slave node. It establishes socket and FTP connections to send commands and transfer files.

**Key Responsibilities:**
- **Connecting to Slaves:** Establishes socket and FTP connections with the slave nodes.
- **Sending Files:** Uses FTP to send file chunks to slaves.
- **Sending Commands:** Uses sockets to send protocol messages to the slaves, instructing them to start different phases of the MapReduce process.
- **Receiving Files:** Retrieves result files from slaves after the reduce phase.

#### SocketThread.java
The `SocketThread` class is responsible for maintaining the socket communication with a single slave node. It listens for messages from the slave and updates the `Master` about the progress of different phases.

**Key Responsibilities:**
- **Listening for Messages:** Continuously reads messages from the slave to monitor the progress of the MapReduce phases.
- **Updating Master:** Notifies the `Master` when a slave completes a phase (e.g., map, shuffle, reduce) by calling relevant update methods in the `Master`.


### Detailed Implementation Points

#### File Splitting
The `splitAndSendChunks` method in `Master` handles the splitting of the input file. Key points include:
- **Chunk Calculation:** The file is divided into chunks based on the total file size and the number of slaves.
- **Avoiding Word Splits:** When splitting, the method ensures that chunks do not end in the middle of a word by checking if the last character of the buffer is a space. If it’s not, it reads additional bytes until a space is found.

#### Protocols Used
- **FTP (File Transfer Protocol):** Used for transferring file chunks from the `Master` to the slaves and for retrieving result files from the slaves to the `Master`. FTP ensures reliable file transfer.
- **Sockets:** Used for communication between the `Master` and the slaves. Through sockets, the `Master` sends protocol messages (e.g., start map, start shuffle) and receives updates on the progress of each phase. 

### How the Classes Work Jointly
1. **Initialization:** The `Master` reads the hostnames of the slaves and initializes `CommunicationHandler` instances for each slave.
2. **File Splitting and Distribution:**
   - The `Master` splits the input file and sends chunks to each slave using `CommunicationHandler`'s FTP functionality.
   - It also sends a `START_MAP` message via sockets to each slave to initiate the map phase.
3. **Progress Tracking:**
   - Each `CommunicationHandler` establishes a socket connection and starts a `SocketThread` to listen for progress messages from the slaves.
   - As slaves complete each phase, they send messages (e.g., `MAP_DONE`), which `SocketThread` receives and processes.
   - The `SocketThread` updates the `Master` about the completion of each phase.
4. **Phase Transitions:** The `Master` sends commands to transition slaves through shuffle and reduce phases based on the received progress updates.
5. **Result Compilation:** After all phases are completed, the `Master` retrieves the final result files from the slaves via FTP and compiles them into a single output file.

This coordinated effort ensures that the MapReduce process runs smoothly, distributing workload effectively and aggregating results accurately.


## `Slave.java`

The `Slave` class represents a worker node in a distributed MapReduce system.

**Key Responsibilities:**
- **MapReduce Phases**:
   - **Map Phase**: Reads a chunk of data, splits it into words, and writes the words to an output file.
   - **Shuffle1 Phase**: Distributes the intermediate words to other slaves based on a hash function.
   - **Reduce1 Phase**: Aggregates word counts from the shuffle phase.
   - **Shuffle2 Phase**: Further groups the word counts and redistributes them.
   - **Reduce2 Phase**: Final aggregation and sorting of the word counts, preparing them for output.


#### `CommunicationHandler.java`

The `CommunicationHandler` class manages both FTP and socket communications for a slave node.

**Key Responsibilities:**
- **FTP Server Management**:
   - **Initialization**: Sets up an FTP server for file exchanges between nodes.
   - **File Transfers**: Handles uploading and downloading files via FTP.

- **Socket Communication**:
   - **Initialization**: Establishes a server socket to listen for connections from the master.
   - **Message Handling**: Sends protocol messages and data objects over the socket to the master.

#### `SocketThread.java`

The `SocketThread` class handles the socket communication in a separate thread.

**Key Responsibilities:**
- **Continuous Communication**:
   - **Run Method**: Listens for and processes messages from the master while running.
   
- **Message Handling**:
   - **Command Execution**: Executes commands based on received messages, such as starting the map, shuffle, and reduce phases.
   - **State Updates**: Updates the state of the `Slave` class based on received messages.


### Detailed Implementation Points

1. **Protocols Used**:
   - **FTP**: For file transfer, ensuring that intermediate files are distributed among slave nodes.
   - **Sockets**: For real-time communication and coordination between master and slaves.

2. **Slave Communication with Master**:
   - **Sockets**: Used for command and control messages (e.g., `START_MAP`, `START_SHUFFLE1`).
   - **Initialization**: Master sends initialization commands which include the number of slaves, their IDs, and hostnames.

3. **Gathering Shuffle Files from Other Slaves**:
   - **FTP Protocol**: Used for transferring shuffle files between slaves.
   - **Shuffle Phase**: Each slave sends its shuffle files to the appropriate slave nodes using FTP. For example, `shuffle1` files are distributed based on a hash function to ensure balanced load distribution.


## Analysis of Results

As the number of nodes increases, the speedup of the MapReduce algorithm also increases, demonstrating the benefits of parallel processing. However, the speedup is not linear due to overheads such as communication, coordination, and data distribution.

**Amdahl's Law** states that the maximum speedup \( S(n) \) of a task using \( n \) parallel processors is given by:
\[ S(n) = \frac{1}{(1 - P) + \frac{P}{n}} \]
where \( P \) is the proportion of the task that can be parallelized, and \( 1 - P \) is the proportion that remains sequential.

Amdahl's Law highlights a fundamental limitation in parallel computing: as the number of processors increases, the impact of the sequential portion of the task becomes more significant, limiting the overall speedup. This means that even if a task is largely parallelizable, the non-parallelizable portion will ultimately constrain the speedup. For example, if 95% of a task can be parallelized (\( P = 0.95 \)), the theoretical maximum speedup with infinite processors is only 20x.

In practical terms, our MapReduce implementation exhibits this behavior. While adding more nodes initially results in substantial speedup, the benefits diminish as the overheads related to data distribution, communication, and coordination grow. These overheads effectively act as the sequential portion in Amdahl's Law, capping the achievable speedup regardless of the number of nodes added. Thus, optimizing both the parallel and sequential components is crucial for maximizing performance.

