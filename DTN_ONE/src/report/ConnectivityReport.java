/* 
 * Copyright 2015 Suman Bhattacharjee, DiSARM
 * Released under GPLv3. See LICENSE.txt for details. 
 * This report will provide the path creation probability of a DTN.
 * This metric is used for measuring the degree of partial connectivity of network.
 */
package report;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import core.ConnectionListener;
import core.DTNHost;
import core.UpdateListener;
/**
 * Generates list of all contacts among nodes
 * Connections that happen during the warm up period are ignored.
 */
public class ConnectivityReport extends Report implements ConnectionListener,UpdateListener {
	private String HOST_DELIM = "<->"; // used in toString()
	private HashMap<String, ConnectionInfo> cons;
	private Collection<DTNHost> allHosts;
	private List<DTNHost> totalHosts;
	private Map<DTNHost, LinkedHashSet<DTNHost>> map = new HashMap<DTNHost, LinkedHashSet<DTNHost>>();
	private long pathcount;
	private DTNHost START;
	private DTNHost END;
	private long nodecount;
	private long cnt;
	private boolean flag;
	private long connectivitycount, totalconnectivitycount;
	/**
	 * Constructor.
	 */
	public ConnectivityReport() {
		this.allHosts = null;
		init();
	}
	
	protected void init() {
		super.init();
		this.cons = new HashMap<String, ConnectionInfo>();
	}
	
		
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}
		
		newEvent();
		ConnectionInfo ci = cons.get(host1+HOST_DELIM+host2);
		
		if (ci == null) {
			cons.put(host1+HOST_DELIM+host2, new ConnectionInfo(host1,host2));
		}
		else {
			ci.nrofConnections++;
		}
	}
	
	public void updated(List<DTNHost> hosts) {
		this.totalHosts=hosts;
		//write(hosts.size()+"");
	}

	// 	Nothing to do here..
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}
	
	/**
	 * Sets all hosts that should be in the graph at least once
	 * @param hosts Collection of hosts
	 */
	public void setAllHosts(Collection<DTNHost> hosts) {
		this.allHosts = hosts;
	}

	public void done() {
		write("Graph connectivity report");
		setPrefix("\t"); // indent following lines by one tab
		for (ConnectionInfo ci : cons.values()) {
			write(ci.h1 + "-->" + ci.h2);
			ci.addEdge(ci.h1,ci.h2);
		}
		// mention all hosts in the graph at least once
		if (this.allHosts != null) {
			for (DTNHost h : allHosts) {
				write(h+ ";");
			}
		}
		setPrefix(""); // don't indent anymore   
		for(DTNHost i : totalHosts){
			for(DTNHost j : totalHosts){
				if(i==j){
					continue;
				}
				flag=false;
				START=i;
				END = j;
				//write("");
				//write("List of paths from "+START+" to "+END);
				//write("");
				LinkedList<DTNHost> visited = new LinkedList<DTNHost>();
				visited.add(START);
				new ConnectionInfo().depthFirst(visited);
				if(flag){
					connectivitycount++;
				}
			}
		}
		write("Total number of paths : "+pathcount);
		nodecount=totalHosts.size();
		long limit=nodecount-2;
		for(int i=0;i<=nodecount-2;i++){
			cnt+= permutation(limit,i);
		}
		long totalpathcount=permutation(nodecount,2)*cnt;
		write("Total possible number of paths : "+totalpathcount);
		Double prob= (double)pathcount/(double)totalpathcount;
		write("Probability of path creation : "+prob);
		write("Total number of connectivity : "+connectivitycount);
		totalconnectivitycount=permutation(nodecount,2);
		write("Total possible number of connectivity : "+totalconnectivitycount);
		Double probconnectivity= (double)connectivitycount/(double)totalconnectivitycount;
		write("Probability of connectivity : "+probconnectivity);
		super.done();
	}
	public int permutation(long l, long index){
		int result;
		result= factorial(l)/factorial(l-index);
		return result;
	}
	public int factorial(long limit){
		int fact=1;
		for(int i=1;i<=limit;i++){
			fact=fact*i;
		}
		return fact;
	}

	/**
	 * Private class stores information of the connected hosts
	 * and nrof times they have connected.
	 */
	private class ConnectionInfo {
		private DTNHost h1;
		private DTNHost h2;
		private int nrofConnections;
		
		public ConnectionInfo(){
			
		}
		
		public ConnectionInfo(DTNHost h1, DTNHost h2) {
			this.h1 = h1;
			this.h2 = h2;
			this.nrofConnections = 1;
		}
				
		public boolean equals(Object o) {
			return o.toString().equals(this.toString());
		}
		public int hashCode() {
			return toString().hashCode();
		}
		public String toString() {
			return h1+HOST_DELIM+h2;
		}
		public int compareTo(Object o) {
			return nrofConnections - ((ConnectionInfo)o).nrofConnections;
		}
		public void addEdge(DTNHost h1, DTNHost h2) {
	        LinkedHashSet<DTNHost> adjacent = map.get(h1);
	        if(adjacent==null) {
	            adjacent = new LinkedHashSet<DTNHost>();
	            map.put(h1, adjacent);
	        }
	        adjacent.add(h2);
	    }
		public LinkedList<DTNHost> adjacentNodes(DTNHost last) {
	        LinkedHashSet<DTNHost> adjacent = map.get(last);
	        if(adjacent==null) {
	            return new LinkedList<DTNHost>();
	        }
	        return new LinkedList<DTNHost>(adjacent);
	    }
		private void depthFirst(LinkedList<DTNHost> visited) {
	        LinkedList<DTNHost> nodes = this.adjacentNodes(visited.getLast());
	        // examine adjacent nodes
	        for (DTNHost node : nodes) {
	            if (visited.contains(node)) {
	                continue;
	            }
	            if (node.equals(END)) {
	                visited.add(node);
	                //write(visited +"");
	                flag=true;
	                pathcount++;
	                visited.removeLast();
	                break;
	            }
	        }
	        // in depth-first, recursion needs to come after visiting adjacent nodes
	        for (DTNHost node : nodes) {
	            if (visited.contains(node) || node.equals(END)) {
	                continue;
	            }
	            visited.addLast(node);
	            depthFirst(visited);
	            visited.removeLast();
	        }
	    }

	}

}
