# SLR207

## Commands
Machines usable : https://tp.telecom-paris.fr/
Machines used : tp-1a201-17 - tp-1a201-18 - tp-1a201-21

ssh elegallic-22@tp-1a201-00.enst.fr 

scp ./target/myftpserver-1-jar-with-dependencies.jar elegallic-22@tp-1a201-17.enst.fr:/tmp/elegallic-22

# Workflow
Master send files via FTP -> Server receives the file
Master send map() signal -> Server starts the map() function
Server finishes the map() and send back to Master -> Master starts reduce()

FTP : 
- Sending split from masters to slaves
- Sending map results from slaves to slaves
- Sending second reduce phase (sorting) from slave to master

Socket : 
- Send number of nodes in the process to all slaves
- Send start map process
- Send start shuffle process
- Receive first reduce part (counting) from slaves
- Send group ids to slaves