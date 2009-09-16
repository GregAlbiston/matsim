package playground.christoph.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentWait2LinkEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.core.population.LegImpl;

import playground.christoph.network.MyLinkImpl;

public class LinkVehiclesCounter implements BasicLinkEnterEventHandler,
		BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
		BasicAgentDepartureEventHandler, BasicAgentWait2LinkEventHandler,
		BasicAgentStuckEventHandler, QueueSimulationAfterSimStepListener,
		QueueSimulationInitializedListener {

	/*
	 * Counts the number of Vehicles on the QueueLinks of a given QueueNetwork.
	 * 
	 * Additional a List of Links with changed VehicleCount per TimeStep is
	 * created.
	 */

	private QueueNetwork queueNetwork;

	private static final Logger log = Logger.getLogger(LinkVehiclesCounter.class);

	// cyclical Status information - adapted from QueueSimulation
	protected static final int INFO_PERIOD = 3600;

	Map<Id, Integer> waitingMap;
	Map<Id, Integer> vehQueueMap;
	Map<Id, Integer> parkingMap;
	// Map<Id, Integer> transitVehicleStopQueueMap; // maybe later...
	Map<Id, Integer> bufferMap;

	/*
	 *  List of Links where the number of driving Cars has changed in the 
	 *  current TimeStep (and maybe changed back again...)
	 *  Is used internally and changed during a SimStep
	 */
	Map<Id, Integer> countChangedMap; // LinkId, CarCount driving on the Link
	
	/*
	 * List of Links where the Number of driving has changed in the last 
	 * TimeStep. It can for example be used by Replanners between two SimSteps
	 * to check for which Links they have to recalculate the TravelTimes and
	 * TravelCosts.
	 */
	Map<Id, Integer> countChangedInTimeStepMap; // Counts from the previous TimeStep
	
	Map<Id, Integer> countLastTimeStepMap; // Counts from the previous TimeStep
	
	int lostVehicles;
	int initialVehicleCount;

	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;

		// Doing this will create the Maps and fill them with "0" Entries.
//		createInitialCounts();
	}

	private synchronized void createInitialCounts() {
		
		// initialize the Data Structures
		parkingMap = new HashMap<Id, Integer>();
		waitingMap = new HashMap<Id, Integer>();
		vehQueueMap = new HashMap<Id, Integer>();
		bufferMap = new HashMap<Id, Integer>();

		countChangedMap = new HashMap<Id, Integer>();
		countChangedInTimeStepMap = new HashMap<Id, Integer>();
		countLastTimeStepMap = new HashMap<Id, Integer>();
		
		initialVehicleCount = 0;
		lostVehicles = 0;
		
		// collect the Counts
		for (QueueLink queueLink : queueNetwork.getLinks().values()) {
			int vehCount = queueLink.getAllVehicles().size();

			initialVehicleCount = initialVehicleCount + vehCount;

			Id id = queueLink.getLink().getId();
			parkingMap.put(id, vehCount);
			waitingMap.put(id, 0);
			vehQueueMap.put(id, 0);
			bufferMap.put(id, 0);

			// initially every LinkCount has changed
			countChangedMap.put(id, 0);
			
			// same value in the TimeStep before
			countLastTimeStepMap.put(id, 0);
		}
	}

	public synchronized void handleEvent(BasicLinkEnterEvent event) {
//		log.info("BasicLinkEnterEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount++;
		vehQueueMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count + 1);
	}

	public synchronized void handleEvent(BasicLinkLeaveEvent event) {
//		log.info("BasicLinkLeaveEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = bufferMap.get(id);
		vehCount--;
		bufferMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count - 1);
	}

	public synchronized void handleEvent(BasicAgentArrivalEvent event) {
//		log.info("BasicAgentArrivalEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount--;
		vehQueueMap.put(id, vehCount);

		vehCount = parkingMap.get(id);
		vehCount++;
		parkingMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count - 1);
	}

	/*
	 * Structure of this method: Have a look at
	 * QueueSimulation.agentDeparts()...
	 */
	public synchronized void handleEvent(BasicAgentDepartureEvent event) {
//		log.info("BasicAgentDepartureEvent " + event.getLinkId().toString() + " " + event.getTime());

		// Handling depends on the Route of the Agent
		LegImpl leg = ((AgentDepartureEventImpl) event).getLeg();

		if (leg.getMode().equals(TransportMode.car)) {
			Id id = event.getLinkId();

			int vehCount;
			vehCount = parkingMap.get(id);
			vehCount--;
			parkingMap.put(id, vehCount);
			
			/*
			 * This is the "else" part from below. Looks like as it works 
			 * correct with that, even if i have no idea why :? 
			 */
			vehCount = waitingMap.get(id);
			vehCount++;
			waitingMap.put(id, vehCount);

			int count;
			if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
			else count = countLastTimeStepMap.get(id);
			
			countChangedMap.put(id, count + 1);
			
			/*
			 * The QueueSimulation additionally checks "&& agent.chooseNextLink() == null"
			 * The agent seems to be still null here - it is set after the event was created :(
			 */
//			NetworkRoute route = (NetworkRoute) leg.getRoute();
//			
//			LinkImpl link = queueNetwork.getNetworkLayer().getLink(event.getLinkId());
//			QueueVehicle vehicle = queueNetwork.getQueueLink(event.getLinkId()).getVehicle(route.getVehicleId());
//			DriverAgent agent = vehicle.getDriver();
//
//			if (route.getEndLink() == link && agent.chooseNextLink() == null)
//			{
//				// nothing to do here... ArrivalEvent is created and that is handled elsewere.
//			} 
//			else
//			{
//				vehCount = waitingMap.get(id);
//				vehCount++;
//				waitingMap.put(id, vehCount);
//
//				countChangedMap.put(id, vehCount);
//			}
		} else {
			log.error("Unknown Leg Mode!");
		}
	}

	public synchronized void handleEvent(BasicAgentWait2LinkEvent event) {
//		log.info("BasicAgentWait2LinkEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = waitingMap.get(id);
		vehCount--;
		waitingMap.put(id, vehCount);

		vehCount = bufferMap.get(id);
		vehCount++;
		bufferMap.put(id, vehCount);
	}

	public synchronized void handleEvent(BasicAgentStuckEvent event) {
//		log.info("BasicAgentStuckEvent " + event.getLinkId().toString() + " " + event.getTime());

		lostVehicles++;

		// Nothing else to do here - if a Vehicles is stucked, it is removed
		// and a LeaveLinkEvent is created!
		
//		Id id = event.getLinkId();
//		
//		int count;
//		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
//		else count = countLastTimeStepMap.get(id);
//		
//		// where to remove it?
//		countChangedMap.put(id, count - 1);
	}

	/*
	 * Looks like that there is a small Error in the counting of the VehQueue
	 * Map (and only in this Map what is quite strange because i have no idea
	 * how this is possible)
	 * 
	 * To use this method, some getters have to be added to the QueueLink Class!
	 */
	
	private synchronized void checkVehicleCount(QueueSimulationAfterSimStepEvent e)
	{  
//		log.info("checking Vehicle Count");
//		if (e.getSimulationTime() >= infoTime) 
//		{ 
//			infoTime += INFO_PERIOD;
//			log.info("SIMULATION AT " + Time.writeTime(e.getSimulationTime()) + " checking parking Vehicles Counts");
//		}
	 
		for (QueueLink queueLink : queueNetwork.getLinks().values()) 
		{
			Id id = queueLink.getLink().getId();
	 
			/*
			 * Vehicles can be moved from the Vehicle Queue to the Buffer
			 * without creating an Event, so we don't know the exact counts
			 * in the single lists. Due to the fact that sum must be correct
			 * we can check the sum or move the vehicles in our list as long
			 * as our count is wrong. 
			 */
//			int inBufferCount = queueLink.getVehiclesInBuffer().size();
//			int myInBufferCount = bufferMap.get(id); 
//			
//			if (inBufferCount != myInBufferCount)
//			{ 
//				int diff = inBufferCount - myInBufferCount;
//				bufferMap.put(id, inBufferCount);
//	 
//				int myVehQueueCount = vehQueueMap.get(queueLink.getLink().getId());
//				vehQueueMap.put(id, myVehQueueCount - diff);
//			}
	 
			if (Simulation.getLost() != lostVehicles)
			{ 
				log.error("Wrong LostCount");
				log.error("Expected: " + Simulation.getLost() + ", Found: " + lostVehicles);
			} 
//			
//			if (queueLink.parkingCount() != parkingMap.get(id))
//			{
//				log.error("Wrong ParkingCount on Link " + id); log.error("Expected: " + queueLink.parkingCount() + ", Found: " + parkingMap.get(id)); 
//			} 
//			
//			if (queueLink.bufferCount() != bufferMap.get(id))
//			{
//				log.error("Wrong BufferCount on Link " + id); log.error("Expected: " + queueLink.bufferCount() + ", Found: " + bufferMap.get(id)); 
//			} 
//			
//			if (queueLink.vehQueueCount() != vehQueueMap.get(id))
//			{
//				log.error("Wrong VehicleQueueCount on Link " + id);
//				log.error("Expected: " + queueLink.vehQueueCount() + ", Found: " + vehQueueMap.get(id)); 
//			} 
//			
//			if (queueLink.waitingCount() != waitingMap.get(id)) 
//			{ 
//				log.error("Wrong WaitingCount on Link " + id);
//				log.error("Expected: " + queueLink.waitingCount() + ", Found: " + waitingMap.get(id));
//			}
			
			int allVehicles = parkingMap.get(id) + bufferMap.get(id) + vehQueueMap.get(id) + waitingMap.get(id);
			if (queueLink.getAllVehicles().size() != allVehicles)
			{
				log.error("Wrong VehicleCount on Link " + id); log.error("Expected: " + queueLink.getAllVehicles().size() + ", Found: " + allVehicles); 
			} 	
		}
		
		int parking = 0;
		int waiting = 0;
		int inQueue = 0;
		int inBuffer = 0;
	 
		for (int count : parkingMap.values()) parking = parking + count; 
		for (int count : waitingMap.values()) waiting = waiting + count; 
		for (int count : vehQueueMap.values()) inQueue = inQueue + count; 
		for (int count : bufferMap.values()) inBuffer = inBuffer + count;
	 
		int countDiff = initialVehicleCount - this.lostVehicles - parking - waiting - inQueue - inBuffer;
		
		if (countDiff != 0) 
		{
			log.error(e.getSimulationTime() + " Wrong number of vehicles in the Simulation - probably missed some Events! Difference: " + countDiff);
		}
	 }
	 

	/*
	 * Remove Link from Map, if VehicleCount has not changed or has changed more
	 * often, so that in the end its the same as in the beginning.
	 * 
	 * [TODO] Check the amount of time that is needed for this Check. Maybe it
	 * would be faster to recalculate the TravelTimes for all Links in the Map
	 * without check.
	 */
	private void filterChangedLinks() 
	{
		for (Iterator<Id> iterator = countChangedMap.keySet().iterator(); iterator.hasNext();)
		{
			Id id = iterator.next();
			// Same Count as in the last SimStep? -> remove it from "has changed" List
			if (countChangedMap.get(id) == countLastTimeStepMap.get(id)) iterator.remove();
		}
		countChangedInTimeStepMap.clear();
		countChangedInTimeStepMap.putAll(countChangedMap);

		countLastTimeStepMap.putAll(countChangedMap);
		
		countChangedMap.clear();
	}

	/*
	 * We assume that the Simulation uses MyLinkImpl instead of LinkImpl, so
	 * we don't check this for every Link...
	 */
	private synchronized void updateLinkVehicleCounts()
	{
		Map<Id, Integer> links2Update = getChangedLinkVehiclesCounts();
		
		/*
		 * We also could iterate over all NetworkLinks and check whether their
		 * VehiclesCount has changed - but in most cases only a few Links will change
		 * in a single SimStep so iterating over the changed Entry and look for
		 *  the corresponding Link should be faster...
		 *  [TODO] Check, whether this Assumption is true.
		 */
        for (Entry<Id, Integer> entry : links2Update.entrySet()) 
        {
            Id id = entry.getKey();
            Integer vehiclesCount = entry.getValue();
         
            // Assumption...
            MyLinkImpl link = (MyLinkImpl)this.queueNetwork.getNetworkLayer().getLink(id);
            
            link.setVehiclesCount(vehiclesCount);
        }
        
//        for (Link link : this.queueNetwork.getNetworkLayer().getLinks().values())
//        {
//        	if (this.getLinkDrivingVehiclesCount(link.getId()) != ((MyLinkImpl)link).getVehiclesCount())
//        	{
//        		double v1 = this.getLinkDrivingVehiclesCount(link.getId());
//        		double v2 = ((MyLinkImpl)link).getVehiclesCount();
//        		log.error("Vehicles Count does not match! " + link.getId().toString() + " " + v1 + " " + v2);
//        	}
//        }
	}
	
	/*
	 * Returns a Map<LinkId, driving Vehicles on the Link>. The Map contains
	 * only those Links, where the number of driving Vehicles has changed within 
	 * in the current TimeStep of the QueueSimulation. If all Links are needed, 
	 * create Method like getLinkVehicleCounts().
	 */
	public Map<Id, Integer> getChangedLinkVehiclesCounts()
	{
		return countChangedInTimeStepMap;
	}

	/*
	 * Returns the number of Vehicles, that are driving on the Link.
	 */
	public int getLinkDrivingVehiclesCount(Id id) {
		int count = 0;

		count = count + waitingMap.get(id);
		count = count + vehQueueMap.get(id);
		count = count + bufferMap.get(id);

		return count;
	}

	public synchronized void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) {
//		log.info("SimStep done..." + e.getSimulationTime());
//		System.out.println("LinkVehiclesCounter QueueSimulationAfterSimStepEvent " + e.getSimulationTime() + "-------------------------------------------------------------------------------");
		// Check the vehicle count every Hour
		if (((int)e.getSimulationTime()) % 3600 == 0) checkVehicleCount(e);
		
		filterChangedLinks();
		updateLinkVehicleCounts();
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e)
	{	
//		System.out.println("LinkVehiclesCounter QueueSimulationInitializedEvent-------------------------------------------------------------------------------");
		createInitialCounts();
		filterChangedLinks();
		updateLinkVehicleCounts();
	}

	public void reset(int iteration)
	{
		createInitialCounts();
		updateLinkVehicleCounts();
	}
}
