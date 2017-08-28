# Consistent Database

### Credits:

    The problem statement and setup of cassandra has been shamelessly copied
    from Prof. Arun V. class (590CC Cloud Computing- PA2)
    https://docs.google.com/document/d/1-iBuA-rmW9SSMUQinYRdJsi8kuh7qRwIMphJOUzo36I/edit


### Problem

1. To build server which acts as a distributed database.
2. A single server node uses a single node cassandra database as a single node
backend but ensures consistency with other server nodes.
3. The servers should communicate with each other and ensure consistency.
The servers are accessed via a client jar.

### Build Environment and Tools used

1. I used ant for building jars and then realized that 
I needed to add datastax which had its on dependencies. So I used 
maven to fill up the library and ant for build.(Lesson: Next time stick with Maven)

2. A Makefile contains a more consice way of building everything
Clears the logs, frees up ports and deploys servers.

### Forms of Consistency guaranteed

1. Server centric consistency:
   A totally ordered Multicast algorithm is implemented.
2. Client centric consistency:
   Read your writes,Writes your writes, monotonic reads and monotonic writes
   
### How to run 
1. Download Cassandra and run with ./cassandra -f. Check status with 
./nodetool status . This should have one node in 127.0.0.1
2. Copy the Cassandra file and make the following changes to run it 
as a different node.
3. Create an alias for your loopback interface as follows, sudo ifconfig lo0 alias 127.0.0.2 
4. Edit node2’s conf/cassandra.yaml file to change listen_address: localhost instead to listen_address: 127.0.0.2. Similarly, set rpc_address: 127.0.0.2.
5. Note that the cluster_name parameter in the above file tells node2 which cluster it belongs to and the seeds parameter in the above file tells node2 that 127.0.0.1 (or node1) is the seed node. Leave these unchanged.
6. Start node2’s server using bin/cassandra -f.
7. You should see node2 successfully start up except for one JMX-related exception that you can ignore (because we won’t be using JMX). If it bothers you, you can make it go away by editing conf/cassandra-env.sh to set JMX_PORT to some value other than the default value of 7199 (that node1 is also using).
8. Download all dependencies into lib/ (mkdir lib if not exists) by doing:

        mvn dependency:copy-dependencies -DoutputDirectory=lib
9. Create a keyspace in both the nodes called 'repl1'.
      
      CREATE KEYSPACE repl1
          WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

10. make server (deploys the 2 server. Its just a quick way to run a sequence of commands)
11. ant run-client: deploys the client
12. To test Server centric consistency:

        ant run-client://Run in terminal 1
        12347:localhost "insert into table1(id,col1) values (10,'seven');"
        ant run-client://Run in terminal 2
        12345:localhost "insert into table1(id,col1) values (11,'seven');"
    
    update 1 happens before update 2 in both nodes. As can be confirmed from the logs

13. To test Client-centric consistency:

        ant run-client://
        12347:localhost "insert into table1(id,col1) values (10,'seven');"
        12345:localhost "select * from table1;"
    
    This should return database stale for 10 seconds after which as the update gets
    propagated. The actual value is returned.                              
    
### Mistakes/Improvement    

1. Lamport clock should be persistant and stored in the Database

2. Used course grained single lock. Can improve performance by having 
fine grained lock.    