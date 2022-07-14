package routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import util.Tuple;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
/** Based on an Energy Aware Social based routing scheme
 * 
 *
 */
public class EASDMRouter extends ActiveRouter{
		/**
		 * List of nodes that will be used in relaying in this session
		 */
		private List<Tuple<Message, DTNHost>> relay;
		/**
		 * List of nodes that have low centrality
		 */
		private List<DTNHost> lowPi;
		/**
		 * List of nodes that have high centrality
		 */
		private List<DTNHost> highPi;
		
		/**
		 * Stores the alpha parameter used in our relay selection algorithm
		 */
		private double alpha = 0.25;
		/**
		 * Constructor. Creates a new EASDM Router from the given settings
		 * @param s The Settings object
		 */
		public EASDMRouter(Settings s)
		{
			super(s);
			//TODO: Edit this portion if settings for this router is required
			lowPi = null;
			highPi = null;
			relay = new ArrayList<Tuple<Message,DTNHost>>();
		}
		/**
		 * Copy constructor
		 * @param r The router object
		 */
		public EASDMRouter(EASDMRouter r)
		{
			super(r);
			//TODO: Edit this part if a router object is required to be created from another object
			lowPi = null;
			highPi = null;
			relay = new ArrayList<Tuple<Message,DTNHost>>();
		}
		
		@Override
		public EASDMRouter replicate()
		{
			return new EASDMRouter(this);
		}
		@Override
		public void update(){
			super.update();
			if(isTransferring() || !canStartTransfer())
				return;
			else if(exchangeDeliverableMessages()!=null)
				return;
			else
				tryRelays();
		}
		protected Connection tryRelays(){
			List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
			if(messages.size() == 0)
				return null;
			this.sortByQueueMode(messages);
			List<Connection> conn = getConnections();
			updaterelay(); //update the relay list
			//System.out.println(relay.size());
			boolean flag = false;
			Message temp = null;
			// check loop later
			for(int i=0;i<relay.size();){
				Tuple<Message,DTNHost> t = relay.get(i);
				boolean remFlag = false;
				Message m = t.getKey();
				if(temp == null)
					temp = t.getKey();
				else if(flag == false && temp.getId() != m.getId())
					temp = m;
				else if(temp!=null && flag == true && temp.getId() != m.getId())
					break;
				else
				{
					for(Connection con:conn)
					{
						DTNHost h = con.getOtherNode(getHost());
						DTNHost curr = t.getValue();
						if(h.getAddress() == curr.getAddress())
						{
							EASDMRouter r = (EASDMRouter)h.getRouter();
							if(!r.isTransferring())
							{
								startTransfer(m,con);
								remFlag = true;
								flag = true;
								break;
							}
						}
					}
				}
				if(!remFlag)
					i++;
				else
					relay.remove(i);
			}
			return null;
		}
		public void updaterelay()
		{
			List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
			for(int i=0;i<relay.size();){
				Tuple<Message,DTNHost> t = relay.get(i);
				Message m = t.getKey();
				/* If message is dropped from message buffer 
				 * the respective (message,relay) pair will also be dropped */
				if(!messages.contains(m))
					relay.remove(i);
				else
					i++;
		   	}
			/* Check for new messages and then set its relays
			 */
			boolean flag = false;
			for(Message m:messages)
			{
				flag = false;
				for(Tuple<Message,DTNHost> t:relay)
				{
					Message temp = t.getKey();
					if(temp.getId() == m.getId())
					{
						flag = true;
						break;
					}
				}
				if(!flag)
				{
					selectrelay(m);
					//System.out.println("Selecting relay");
				}
			}
		}
		public void selectrelay(Message m)
		{
			double thresh = 0.5;
			lowPi = sortByEnergy(this.getHost().getLowNodes(thresh)); 
			highPi = sortByEnergy(this.getHost().getHighNodes(thresh));
			int l = lowPi.size();
			int h = highPi.size();
			//System.out.println(l+" "+h);
			int reserved = (int)(l * this.alpha);
			double base = 1000;
			double P = 1, p = 0.5;
			for(int  i=0;i<l && P>1-p;i++)
			{
				if((l-i) <= reserved)
					break;
				DTNHost curr = lowPi.get(i);
				double energy = (Double)curr.getComBus().
						getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
				if(energy>base)
				{
					Tuple<Message, DTNHost> temp = new Tuple<Message,DTNHost>(m,curr);
					relay.add(temp);
					P=P*this.getHost().getP(curr);
				}
			}
			for(int i=0;i<h && P>1-p;i++)
			{
				DTNHost curr = highPi.get(i);
				double energy = (Double)curr.getComBus().
						getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
				if(energy>base)
				{
					Tuple<Message, DTNHost> temp = new Tuple<Message,DTNHost>(m,curr);
					relay.add(temp);
					P=P*this.getHost().getP(curr);
				}
			}
		}
		/*@Override
		public int receiveMessage(Message m, DTNHost from) {
			int retVal = super.receiveMessage(m, from);
			selectRelay(m);
			return retVal;
		}*/
		public List<DTNHost> sortByEnergy(List<DTNHost> list)
		{
			Collections.sort(list, new Comparator<DTNHost>() {
						public int compare(DTNHost h1,DTNHost h2)
						{
							double e1 = (Double)h1.getComBus().
									getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
							double e2 = (Double)h2.getComBus().
									getProperty(routing.util.EnergyModel.ENERGY_VALUE_ID);
							if(e1>e2)
								return -1;
							else if(e1 == e2)
								return 0;
							else
								return 1;
						}
					});
			return list;
		}
}		



