/* 
 * Copyright 2017 IIEST, DiSARM
 * Developed by Suman Bhattacharjee
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.Settings;
import core.SettingsError;

/**
 * Message creation -external events generator. Creates one message from
 * source node/nodes (defined with {@link MessageEventGenerator#HOST_RANGE_S})
 * to all destination nodes (defined with
 * {@link MessageEventGenerator#TO_HOST_RANGE_S}). 
 * The message size, first messages time and the intervals between creating 
 * messages can be configured like with {@link MessageEventGenerator}. End
 * time is not respected, but messages are created until there's a message for
 * every destination node.
 * @see MessageEventGenerator
 */
public class MulticastEventGenerator extends MessageEventGenerator {
	/**  Range of multicast/broadcast destinations a message will contain*/
	public static final String MULTICAST_DEST_RANGE = "destinationRange";
	protected int[] destinationRange = {0, 0};
	private List<Integer> toIds;
	private int[] destination_list = null;
	public MulticastEventGenerator(Settings s) {
		super(s);
		this.destinationRange = s.getCsvInts(MULTICAST_DEST_RANGE, 2);
		this.toIds = new ArrayList<Integer>();
		
		if (toHostRange == null) {
			throw new SettingsError("Destination host (" + TO_HOST_RANGE_S + ") must be defined");
		}
		
		for (int i = toHostRange[0]; i < toHostRange[1]; i++) 
			toIds.add(i);
		if ((destinationRange[1]-destinationRange[0]) != destinations){
			throw new SettingsError("Destination address range (" + MULTICAST_DEST_RANGE + ") and number of destination (" + MULTICAST_DEST_S + ") must be identical");
		}
			destination_list = new int[]{destinationRange[0], destinationRange[1]};
		Collections.shuffle(toIds, rng);
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		int from;
		int to;
		
		from = drawHostAddress(hostRange);	
		to = this.toIds.remove(0);
		
		if (to == from) { /* skip self */
			if (this.toIds.size() == 0) { /* oops, no more from addresses */
				this.nextEventsTime = Double.MAX_VALUE;
				return new ExternalEvent(Double.MAX_VALUE);
			} else {
				to = this.toIds.remove(0);
			}
		}

		if (this.toIds.size() == 0) {
			this.nextEventsTime = Double.MAX_VALUE; /* no messages left */
		} else {
			this.nextEventsTime += drawNextEventTimeDiff();
		}
		
		MessageCreateEvent mce;		
		if(!this.is_multicast){
			mce = new MessageCreateEvent(from, to, this.getID(), drawMessageSize(), responseSize, this.nextEventsTime);
	    }
	    else
	    {             
	    	mce = new MessageCreateEvent(from, to, this.getID(), drawMessageSize(), responseSize, this.nextEventsTime,this.is_multicast,multicast_destination_list(destination_list, from));
	    }
		
		return mce;
	}

	protected int [] multicast_destination_list(int destination_list[],int from)
    {
    
		int to[] = new int[this.destinations];
		int i =0,j=0,k=0;
		boolean destination_checker = false;
		do {
                destination_checker = false;
                to[i++] = drawDestAddress(this.destination_list,k);
                k++;
                 for(j=0;j<i-1;j++)
                 {
                     if(to[j] == to[i-1]&&to[j] == from)
                     {    
                         destination_checker = true;i--;
                     }
                 }     
		} while (i<this.destinations||destination_checker);
		/*if(to.length<5)
            {
            int x =10;
            }*/
		return to;
    }
	/**
	 * Draws a multicast destination address from the configured address range
	 * @param hostRange The range of hosts
	 * @return A random host address
	 */
	protected int drawDestAddress(int destination_list[], int k) {
		if (destination_list[1] == destination_list[0]) {
			return destination_list[0];
		}
		return destination_list[0] + k;
	}
}