/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.util.RoutingInfo;

import util.Tuple;

import core.Coord;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Implementation of Game router as described in 
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class edMultiRouter extends ActiveRouter {
	
	/** number of encounters of every node with every other node*/
	private static int[][] encounters;

	/** sumEncounters of total encounters by every node*/
	private static Map<DTNHost, Integer> sumEncounters;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public edMultiRouter(Settings s) {
		super(s);
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected edMultiRouter(edMultiRouter r) {
		super(r);
	}
	
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateEncounters(getHost(),otherHost);
		}
	}
	

	/**
	 * Updates the value of encounters when two nodes come in contact with each other
	 * @param host1 first node
	 * @param host2 second node
	 */
	public void updateEncounters(DTNHost host1, DTNHost host2) {
		//each message has different destination and we'll need encounters of every node with the destination(which is changing with each message) in same time instance, hence we've decided to use a 2D array
		if (this.encounters == null) {
			this.encounters=new int[host1.getHosts().size()][host1.getHosts().size()]; //TODO:replace 126 by hosts.size
		}
		if(this.sumEncounters == null){
			this.sumEncounters=new HashMap<DTNHost, Integer>();
		}
		edMultiRouter othRouter = (edMultiRouter)host2.getRouter();
		edMultiRouter myRouter = (edMultiRouter)host1.getRouter();

		//if sumEncounters does not contain host1 , put host1 in sumEncounters and initialise by 0
		if(!this.sumEncounters.containsKey(host1))
		{
			sumEncounters.put(host1,0);
		}
		//if sumEncounters does not contain host1 , put host1 in sumEncounters and initialise by 0
		if(!this.sumEncounters.containsKey(host2))
		{
			sumEncounters.put(host2,0);
		}
		if(myRouter!=othRouter)
		{
			this.encounters[host1.getAddress()][host2.getAddress()]++;

			//increase the value of sumEncounters of host1 by 1 
			this.sumEncounters.put(host1,this.sumEncounters.get(host1)+1);
		}
		else
		{
			this.encounters[host1.getAddress()][host2.getAddress()]++;
			this.encounters[host2.getAddress()][host1.getAddress()]++;
			this.sumEncounters.put(host1,this.sumEncounters.get(host1)+1);
			this.sumEncounters.put(host2,this.sumEncounters.get(host2)+1);
		}
		//System.out.println(host1.getAddress()+ " " + sumEncounters.get(host1));
		//System.out.println(host2.getAddress()+ " " + sumEncounters.get(host2));
	}

	/**
	 * Returns the current encounter (E) value for a host
	 * @param host1 The host to look the E for
	 * @param host2 The node with respect to which we have to return the encounters
	 * @return the current E value
	 */
	public int getEncounter(DTNHost host1,DTNHost host2){
		return this.encounters[host1.getAddress()][host2.getAddress()];
	}

	/**
	 * Returns the current sumEncounters (S) value for a host
	 * @param host The host to look the S for
	 * @return the current S value
	 */
	public int getsumEncounters(DTNHost host){
		if(sumEncounters.containsKey(host))
		{
			//System.out.println("poop:"+host.getAddress()+"="+this.sumEncounters.get(host));
			return this.sumEncounters.get(host);
		}
		else
			return 0;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();		
	}
	
	/**
	 * Tries to send all other messages to all connected hosts
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();

		
		for(Message m : msgCollection){

			Map<DTNHost, Double> bestGammaWorld = new HashMap<DTNHost, Double>();

			Map<DTNHost, Double> bestGammaLocal = new HashMap<DTNHost, Double>();

			DTNHost dest = m.getTo();

			double threshold=1;

			/*
			for(DTNHost node : getHost().getHosts()){

				//alpha and beta of otherRouter
				double alphaNode,betaNode,gammaNode;

				//if the sumEncounters of encounters of all other nodes w.r.t destination is 0,
				//then initialise alphaOther to 0 (prevents divide by zero error)
				if(getsumEncounters(dest)==0)
				{
					//System.out.println();
					alphaNode=0;
				}
				else
				{
					alphaNode=getEncounter(dest,node)/getsumEncounters(dest);
				}
				
				//beta for otherRouter
				betaNode=getDistFor(dest,node)/getsumDist(dest);

				gammaNode=alphaNode/betaNode;

				if(gammaNode>threshold)
					bestGammaWorld.put(node,gammaNode);
			}
			*/

			for (Connection con : getConnections()){

				DTNHost me = getHost();
				DTNHost other = con.getOtherNode(getHost());
				edMultiRouter othRouter = (edMultiRouter)other.getRouter();
				
				//alpha and beta of otherRouter
				double alphaOther,betaOther,gammaOther;
				//alpha and beta of MeRouter
				double alphaMe,betaMe,gammaMe;

				//if the sumEncounters of encounters of all other nodes w.r.t destination is 0,
				//then initialise alphaOther to 0 (prevents divide by zero error)
				if(getsumEncounters(dest)==0)
				{
					alphaOther=0;
					alphaMe=0;
				}
				else
				{
					alphaOther=getEncounter(dest,other)/getsumEncounters(dest);
					alphaMe=getEncounter(dest,me)/getsumEncounters(me);
				}
				
				//beta for otherRouter
				betaOther=getDistFor(dest,other)/getsumDist(dest);
				//beta for MeRouter
				betaMe=getDistFor(dest,me)/getsumDist(me);

				gammaOther=alphaOther/betaOther;
				gammaMe=alphaMe/betaMe;

				if(gammaOther>threshold)
					bestGammaLocal.put(other,gammaOther);
			}

			//intersection 
			//Map<DTNHost, Double> intersection = new HashMap<DTNHost, Double>(bestGammaLocal);
			//intersection.keySet().retainAll(bestGammaWorld.keySet());

			//if intersection is not empty
			if(!bestGammaLocal.isEmpty()){
				/*
				Connection c= new Connection(getHost(),getHost().getInterface(),maxGamma.getKey(),maxGamma.getKey().getInterface());
				edMultiRouter othRouter = (edMultiRouter)other.getRouter();
				if(!othRouter.isTransferring())
				{
					messages.add(new Tuple<Message, Connection>(m,con));	
				}
				*/
				
				for(Connection con : getConnections())
				{
					DTNHost other=con.getOtherNode(getHost());
					edMultiRouter othRouter = (edMultiRouter)other.getRouter();
					if(othRouter.isTransferring()){
						continue;
					}
					if(othRouter.hasMessage(m.getId())){
						continue;
					}
					if(bestGammaLocal.containsKey(other))
					{
						messages.add(new Tuple<Message, Connection>(m,con));	
					}
				}
				
			}
			/*
			else{

				for(Connection con : getConnections())
				{
					DTNHost other=con.getOtherNode(getHost());
					edMultiRouter othRouter = (edMultiRouter)other.getRouter();
					if(othRouter.isTransferring()){
						continue;
					}
					if(othRouter.hasMessage(m.getId())){
						continue;
					}
				
				
						messages.add(new Tuple<Message, Connection>(m,con));	
				
				}
			}
			*/
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// sort the message-connection tuples (i dont's see the need to sort:-Kunal)
		//Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

//Returns the sum of all the nodes w.r.t dest
private double getsumDist(DTNHost dest)
{
	double sumDist=0;
	for(DTNHost n:dest.getHosts()){
		sumDist+=getDistFor(n,dest);
	}
	return sumDist;
}

/**	
* Returns the current distance between dest node and the nextHost node
* @param dest The destination node 
* @param nextHost The node from which we want to calculate the diatance from
* @return the current distance
*/
private double getDistFor(DTNHost dest,DTNHost nextHost)
{
	Coord destLoc = dest.getLocation();
	Coord nextHostLoc = nextHost.getLocation();
	double x1 = nextHostLoc.getX();
	double y1 = nextHostLoc.getY();
	double x2 = destLoc.getX();
	double y2 = destLoc.getY();
	double a = y1 - y2;
	double b = x2 - x1;
	double c = y2*x1 - y1*x2;
	double dist = Math.pow((a*a+b*b),0.5); 
	if(dist<0) dist = -dist;
	return dist;
}

	@Override
	public MessageRouter replicate() {
		edMultiRouter r = new edMultiRouter(this);
		return r;
	}

}
