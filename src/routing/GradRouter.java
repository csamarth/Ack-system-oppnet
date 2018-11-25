package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;

public class GradRouter extends ActiveRouter {
	/** identifier for the initial number of copies setting ({@value})*/
	public static final String NROF_COPIES = "nrofCopies";
	/** GradRouter settings' name space ({@value})*/
	public static final String GRAD_NS = "GradRouter";
	/** Key for number of copies property */
	public static final String MSG_COUNT_PROPERTY = GRAD_NS + "." + NROF_COPIES;
	/** Key for last transfer type */
	public static final String TRANSFER_TYPE_PROPERTY = GRAD_NS + ".lastTransferType";
	
	private int nrofCopies;
	
	public GradRouter(Settings s) {
		super(s);
		
		Settings gradSettings = new Settings(GRAD_NS);
		nrofCopies = gradSettings.getInt(NROF_COPIES);
	}

	public GradRouter(GradRouter r) {
		super(r);
		this.nrofCopies = r.nrofCopies;
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

		MessageTuple t = tryOtherMessages();
	}

	private MessageTuple tryOtherMessages() {
		List <MessageTuple> messages = new ArrayList<MessageTuple>();
		
		Collection<Message> messageCollection = getMessageCollection();
		
		for (Connection con: getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			GradRouter otherRouter = (GradRouter) other.getRouter();
			
			if (otherRouter.isTransferring()) {
				continue;
			}
			
			for (Message m : messageCollection) {
				if (otherRouter.hasMessage(m.getId())) {
					continue;
				}
				MessageTransferScheme messageTransferScheme = 
						getMessageTransferScheme(getHost(), other, m.getTo());

				
				if (messageTransferScheme == MessageTransferScheme.NO_TRANSFER) {
					continue;
				}
				
				/*
				 * A faithful adaptation of spray and wait strategy should compare
				 * if number of messages is <=1, but in our case the results obtained
				 * are similar to SnW. However, if we choose to compare with zero, 
				 * implying no wait state, the delivery rate increases by 10%, but is
				 * accompanied by an increase in overhead ratio and latency. What shall
				 * be done?
				 */
				if ((Integer)m.getProperty(MSG_COUNT_PROPERTY) <= 0 &&
						messageTransferScheme != MessageTransferScheme.COMPLETE_TRANSFER)
				continue;
				/* There's a problem. This property is set for the message and once set,
				 * it will be valid for all connections. We have to find a way to set it
				 * per connection. Maybe we can do it by using a 3 tuple instead of two,
				 * and set this property just before startTransfer().
				 */
				//m.updateProperty(TRANSFER_TYPE_PROPERTY, messageTransferScheme);
				messages.add(new MessageTuple(m, con, messageTransferScheme));
			}
		}
		
		if (messages.size() == 0) return null;
		
		return tryMessagesForGivenConnection(messages);
	}
	
	private MessageTuple tryMessagesForGivenConnection(List<MessageTuple> messages) {
		if (messages.size()==0) return null;
		
		for (MessageTuple t : messages) {
			Message m = t.getMesssage();
			Connection con = t.getConnection();
			m.updateProperty(TRANSFER_TYPE_PROPERTY, t.getScheme());
			if (startTransfer(m, con) == RCV_OK) {
				return t;
			}
			m.updateProperty(TRANSFER_TYPE_PROPERTY, null);
		}
		return null;
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
		MessageTransferScheme messageTransferScheme = (MessageTransferScheme) msg.getProperty(TRANSFER_TYPE_PROPERTY);

		/* In case of complete transfer, the nrofCopies does not need to be
		 * changed whereas in case of no transfer, this function will never
		 * be called. Hence we have only two options, binary or naive
		 */
		if (messageTransferScheme == MessageTransferScheme.BINARY) {
			nrofCopies = (int) Math.ceil(nrofCopies/2.0);
		}
		else if (messageTransferScheme == MessageTransferScheme.NAIVE ||
				messageTransferScheme == null) {
			nrofCopies = 1;
		}
		msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		msg.updateProperty(TRANSFER_TYPE_PROPERTY, null);
		return msg;
	}
	
	@Override
	protected void transferDone(Connection con) {
		Integer newNrofCopies;
		MessageTransferScheme msgTransferScheme;
		String msgId = con.getMessage().getId();
		
		//The router's copy of message
		Message m = getMessage(msgId);
		if (m == null) return;
		
		newNrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
		msgTransferScheme = (MessageTransferScheme) m.getProperty(TRANSFER_TYPE_PROPERTY);

		if (msgTransferScheme == MessageTransferScheme.BINARY) {
			newNrofCopies /= 2;
		}
		else if (msgTransferScheme == MessageTransferScheme.COMPLETE_TRANSFER) {
			newNrofCopies = 0;
		}
		else if (msgTransferScheme == MessageTransferScheme.NAIVE) {
			newNrofCopies--;
		}
		m.updateProperty(MSG_COUNT_PROPERTY, newNrofCopies);
		m.updateProperty(TRANSFER_TYPE_PROPERTY, null);
	}
	
	@Override
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());
		
		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(nrofCopies));
		msg.addProperty(TRANSFER_TYPE_PROPERTY, null);
		addToMessages(msg, true);
		return true;
	}
	
	private MessageTransferScheme getMessageTransferScheme(DTNHost ni, DTNHost nx, DTNHost to) {
		if (nx.getLocation().distance(to.getLocation()) < getRadioRange(to))
			return MessageTransferScheme.COMPLETE_TRANSFER;

		double pi = getProbability(ni, to);
		double px = getProbability(nx, to);

		//System.out.println(pi+"\t"+px);

		if (px > pi) return MessageTransferScheme.BINARY;
		
		double[] anglesx = getAngles(nx, to);
		double alphax = anglesx[0];
		double thetax = anglesx[1];
		
		if (Double.isNaN(alphax) || alphax > (3 * thetax))
			return MessageTransferScheme.NO_TRANSFER;
		if (alphax < (3*thetax))
			return MessageTransferScheme.NAIVE;
		
		return MessageTransferScheme.NO_TRANSFER;
	}
	
	private double[] getAngles(DTNHost from, DTNHost to) {
		Coord fromCoord = from.getLocation();
		Coord toCoord = to.getLocation();
		double toRadioRange = getRadioRange(to);
		double alpha = Double.NaN;
		if (from.getPath()!=null) {
			alpha = getAlpha(fromCoord, toCoord, from.getPath().getCurrentWaypoint());
		}
		double theta = getTheta(fromCoord, toCoord, toRadioRange);
		return new double[]{alpha, theta};
	}
	
	private double getProbability(DTNHost from, DTNHost to){
		double probability;
		double[] angles = getAngles(from, to);
		double alpha = angles[0];
		double theta = angles[1];
		if (Double.isNaN(alpha)) {
			probability = Double.NEGATIVE_INFINITY;
		}
		probability = (theta - alpha)/theta;
		//System.out.println(alpha+"\t"+theta+"\t"+probability);
		
		return probability;
	}

	private double getTheta(Coord fromCoord, Coord toCoord, double toRadioRange) {
		double distance = fromCoord.distance(toCoord) ;
		double theta = Math.atan(toRadioRange/distance);
		return theta;
	}

	private double getAlpha(Coord fromCoord, Coord toCoord, Coord nextWaypoint) {
		double slopePath = getSlope(fromCoord, nextWaypoint);
		double slopeDest = getSlope(fromCoord, toCoord);
		double alpha = Math.abs(Math.atan(slopePath) - Math.atan(slopeDest));
		return alpha;
	}

	private double getSlope(Coord from, Coord to) {
		double slope = (to.getY() - from.getY())/(to.getX() - from.getX());
		return slope;
	}

	private double getRadioRange(DTNHost to) {
		return (double)getHost().getComBus().getProperty(NetworkInterface.RANGE_ID);
	}

	@Override
	public MessageRouter replicate() {
		GradRouter r = new GradRouter(this);	
		return r;
	}
	
	private class MessageTuple {
		private Message msg;
		private Connection conn;
		private MessageTransferScheme messageTransferScheme;
		
		MessageTuple(Message msg, Connection conn, MessageTransferScheme scheme) {
			this.msg = msg;
			this.conn = conn;
			this.messageTransferScheme = scheme;
		}
		
		public Message getMesssage() {
			return msg;
		}
		
		public Connection getConnection() {
			return conn;
		}
		
		public MessageTransferScheme getScheme() {
			return messageTransferScheme;
		}

		@Override
		public String toString() {
			return String.format("%s\t%s\t%s",
				msg,conn.getOtherNode(getHost()),messageTransferScheme);
		}
	}
	
	private enum MessageTransferScheme {
		COMPLETE_TRANSFER,
		NO_TRANSFER,
		BINARY,
		NAIVE
	}
}
