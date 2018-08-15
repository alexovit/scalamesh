# Scala Mesh
Example of the mesh network for signed streaming data. Each node of the network issues random transactions at random time, sign them and broadcast to it's seed nodes that are configurable. Each node listens the network for new connections checks incoming transactions signature save them to file and broadcast to all the seeds.

Out of scope: key storage, system shutdown
## Prerequisite
* SBT (https://www.scala-sbt.org/1.0/docs/Setup.html)

## Usage
* Use SBT to run network nodes. Configuration accessible via command line arguments
```
sbt "run <args>"
    -h host             hostname
    -p port             port
    -f file             path to file
    --seed host:port    multiple argument for seed nodes host port
```
## Testnet
* To make a test let's create a network of three nodes connected in a chain.
```
        ---->       ---->
   (1)         (2)          (3)
        <----       <----
    
```
* In a different terminal tabs run a process for each node from 1 to 3
```
sbt "run -p 10274 -f /home/my/node1.chain --seed 127.0.0.1:10275"
sbt "run -p 10275 -f /home/my/node2.chain --seed 127.0.0.1:10274 --seed 127.0.0.1:10276"
sbt "run -p 10276 -f /home/my/node3.chain --seed 127.0.0.1:10275"
```
* Log contains unique reference of each issued transaction so we can see how transaction is propagating the network
* For testing purposes nodes network also issue fake transactions with wrong signature. These should be discarded in a process.