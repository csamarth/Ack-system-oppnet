# Acknowledgment System in Opportunistic Networks

Through this project, we ([Subham Kumaram](https://github.com/shubhamkrm) and I) proposed and developed an acknowledgment system in Opportunistic Networks to improve their current efficiency in terms of message throughput through relief in overhead costs. This work was accepted at the [2018 Fifth International Conference on Parallel, Distributed and Grid Computing (PGDC)](http://www.juit.ac.in/pdgc-2018/index1.php) and shall be available online from August 2019. _(link to the research paper will be updated at that time)_


# Index

* [Description of Work](#description-of-work)
* [Results](#results)
* [About the Simulator](#about-the-simulator)


# Description of Work

Opportunistic Environments comprise of nodes which usually have a very limited memory resource. As a result, many routing algorithms designed in the past or being proposed in the future suffer from congestion in the network as well as the limited availability of buffer space. To tackle this problem, we sought to remove unnecessary messages identified with the help of acknowledgments inspired from the MaxProp routing protocol. Acknowledgments contain meta value about the message and its size is almost negligible as compared to the size of a message packet. Hence, acknowledgments can be used with little load on the network.

Each node in the network contains a list of acknowledgment (ACK) packets. Whenever a relay node successfully delivers a message to the intended receiver, both the destination node and the final relay node which sends the message to the destination generate ACK packets. These messages are stored in their respective ACK packet lists. All throughout the life of the network, whenever any two nodes come in contact with each other, both of them exchange their ACK packet lists. Each node then stores a union of both the lists. If there exists a message for which an ACK has been received, it is promptly dropped.


# Results

To gauge the results of our acknowledgment system, we compared 5 established routing protocols (Epidemic, PROPHET, Spray and Wait, ProWait and GRAD) using the [ONE simulator](#about-the-simulator). The metrics used to compare were the **number of messages delivered** and the **overhead ratio** (a direct indicator of congestion in the network) while varying the buffer size of the messages and the number of hosts in the network individually. 

## Buffer Size

On varying the buffer size, protocols that relied heavily on transmitting a large number of messages (Epidemic and PROPHET) had their number of messages delivered increase by a great amount (\~83%) as compared to the increase (\~15%) in protocols that sent only a limited number of messages (Spray and Wait, ProWait and GRAD). Since overhead ratio is directly proportional to the number of messages floating in the network, it is observed to be higher in protocols such as Epidemic and PROPHET, whereas it is on the lower end for protocols such as Spray and Wait and ProWait. The overhead ratio for GRAD lies somewhere in the middle. The observations of altering the buffer size on overhead ratio is in line with the expectations. The Acknowledgment system reduces overhead ratio by a large margin in the case of Epidemic and PROPHET (\~60%), a smaller margin in GRAD (\~42%) and the smallest reduction in Spray and Wait and ProWait (\~15%), but an improvement nonetheless.

## Number of Nodes

The number of nodes that participate in the relay of a message also have an effect on the congestion of the network. With an increase in the number of nodes and the total number of messages kept constant, the availability of buffer pool increases. However, a larger number of message relay contributes to greater congestion resulting mostly from the same message having multiple copies throughout the network. Epidemic and PROPHET, again, benefited the most (\~80% and \~10% respectively) through the acknowledgment system in terms of number of messages delivered. The other protocols already had some measures for congestion control through a limitation on the number of message copies generated. Hence, there is only a marginal improvement in their performance (\~11%). The improvement in the overhead ratio is almost an exponential graph for Epidemic, PROPHET and GRAD protocols (\~50%) while its about constant for Spray and Wait and ProWait (\~15%).


# About the Simulator

All the simulations were performed in the Opportunistic Network Environment (ONE) Simulator. 

For introduction and releases, see [the ONE homepage at GitHub](http://akeranen.github.io/the-one/).

For instructions on how to get started, see [the README](https://github.com/akeranen/the-one/wiki/README).

The [wiki page](https://github.com/akeranen/the-one/wiki) has the latest information.

