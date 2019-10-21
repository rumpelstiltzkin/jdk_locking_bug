To reproduce the problem:
=========================

I] Run the reproducer
---------------------
$ terminal1> git clone <this repo>
$ terminal1> cd jdk_locking_bug
$ terminal1> mvn clean install
$ terminal1> ./runtest.sh

You'll see the following messages (not these exact numbers):

$ terminal1> ./runtest.sh
seed is 1571798887404
Main spawning 100 readers
Main spawned 100 readers
Main spawning a writer
Main spawned a writer
Main waiting for threads ...
cache size is 219073
cache size is 419863
cache size is 623676
cache size is 837314
cache size is 940081
cache size is 1147448
evicting 147448 entries
evicted
cache size is 1113827
evicting 113827 entries
evicted                   <======== Hangs at this line.

II] Get the jstack output of the hung process
---------------------------------------------
Then from another terminal...
$ terminal2> ps -ef |grep java
user   37209  37208 88 02:48 pts/1    00:00:37 java -ea -cp target/*:target/lib/* com.hammerspace.jdk.locking.Main
user   37373  37330  0 02:48 pts/3    00:00:00 grep --color=auto java

$ terminal2> jstack -l 37209 > jstack.1
$ terminal2>

Wait 2 minutes, and then get another jstack
$ terminal2> jstack -l 37209 > jstack.2

III] Validate that the threads are indeed hung
-----------------------------------------------
$ terminal2> diff jstack.1 jstack.2
(shows no difference, threads are all hung)

IV] Analyze where/why the threads are hung
------------------------------------------
$ terminal2> vim -R jstack.1


