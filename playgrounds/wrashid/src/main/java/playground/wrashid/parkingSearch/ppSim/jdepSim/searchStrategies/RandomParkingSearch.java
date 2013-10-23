/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentEventMessage;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadDuringSim;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis.ParkingEventDetails;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class RandomParkingSearch implements ParkingSearchStrategy {

	protected DoubleValueHashMap<Id> startSearchTime ;
	private HashMap<Id, ParkingActivityAttributes> parkingActAttributes;
	protected HashMap<Id, String> useSpecifiedParkingType;
	
	
	
	
	protected String parkingType;
	private double maxDistance;
	private Network network;
	private Random random;
	private final double parkingDuration = 60 * 2;
	private final double walkSpeed = 3.0 / 3.6; // [m/s]

	// go to final link if no parking there, then try parking at other places.
	// accept only parking within 300m, choose random links, but if leave 300m
	// area, try
	// to take direction leading back to destination
	public RandomParkingSearch(double maxDistance, Network network) {
		this.maxDistance = maxDistance;
		this.network = network;
		this.random = MatsimRandom.getLocalInstance();
		resetForNewIteration();
	}

	public HashSet<Id> extraSearchPathNeeded = new HashSet<Id>();

	@Override
	public void handleAgentLeg(AgentWithParking aem) {

		Event event = null;

		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		ActivityImpl prevAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() - 1);
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
				.get(aem.getPlanElementIndex() + 1);

		if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
			Id personId = aem.getPerson().getId();
			
			List<Id> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();
			LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

			boolean endOfLegReached = aem.getCurrentLinkIndex() == linkIds.size() - 1;

			if (endOfLegReached) {
				DebugLib.traceAgent(personId);
				
				String filterParkingType=parkingType;
				
				if (useSpecifiedParkingType.containsKey(personId)){
					filterParkingType=useSpecifiedParkingType.get(personId);
				}
				
				Id parkingId = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(route.getEndLinkId(), filterParkingType);
				ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
						.get(aem.getPlanElementIndex() + 3);

				boolean isInvalidLink = aem.isInvalidLinkForParking();

				// TODO: include max distance here (maxDistance variable)
				
				if (!startSearchTime.containsKey(personId)) {
					startSearchTime.put(personId, aem.getMessageArrivalTime());
				}

				if (parkingId == null || isInvalidLink) {
					DebugLib.traceAgent(personId, 1);
				//	extraSearchPathNeeded.add(personId);

					Random r = new Random();
					Link link = network.getLinks().get(route.getEndLinkId());

					Link nextLink = randomNextLink(link);
					ArrayList<Id> newRoute = new ArrayList<Id>();
					newRoute.addAll(route.getLinkIds());
					newRoute.add(link.getId());
					route.setLinkIds(route.getStartLinkId(), newRoute, nextLink.getId());
					route.setEndLinkId(nextLink.getId());

					// this will just continue the search of the agent
					aem.processLegInDefaultWay();

				} else {
					DebugLib.traceAgent(personId, 2);

				//	if (extraSearchPathNeeded.contains(personId)) {
				//		extraSearchPathNeeded.remove(personId);

						aem.processEndOfLegCarMode_processEvents(leg, nextAct);

						setDurationOfParkingActivity(aem, nextAct);

						ActivityImpl currentParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
								.get(aem.getPlanElementIndex() + 1);
						Leg nextWalkLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements()
								.get(aem.getPlanElementIndex() + 2);
						Link parkingLink = network.getLinks().get(route.getEndLinkId());

						currentParkingAct.setLinkId(parkingLink.getId());

						double walkDistance = GeneralLib.getDistance(parkingLink.getCoord(), nextNonParkingAct.getCoord());
						// TODO: improve this later (no straight line)
						double walkDuration = walkDistance / walkSpeed;
						nextWalkLeg.setTravelTime(walkDuration);
						getParkingAttributesForScoring(aem).setToActWalkDuration(walkDuration);
						getParkingAttributesForScoring(aem).setToParkWalkDuration(walkDuration);
						
						// TODO: update this later, this is an approximation
						getParkingAttributesForScoring(aem).setParkingArrivalTime(aem.getMessageArrivalTime());

						// check, if more car legs, only in that case adapt that
						// leg (TODO:)

						// if car departs again during day, adapt the departure
						// walking and routes

						int indexOfNextCarLeg = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();

						
						// TODO: avoid routing if parking link Id and activity linkId remained the same
						if (indexOfNextCarLeg != -1) {
							ActivityImpl lastActBeforeNextCarLeg = (ActivityImpl) aem.getPerson().getSelectedPlan()
									.getPlanElements().get(indexOfNextCarLeg - 3);
							Leg nextwalkLegToParking = (Leg) aem.getPerson().getSelectedPlan().getPlanElements()
									.get(indexOfNextCarLeg - 2);
							ActivityImpl nextParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements()
									.get(indexOfNextCarLeg - 1);
							Leg nextCarLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg);

							walkDistance = GeneralLib.getDistance(parkingLink.getCoord(), lastActBeforeNextCarLeg.getCoord());
							walkDuration = walkDistance / walkSpeed;
							nextwalkLegToParking.setTravelTime(walkDuration);

							nextParkingAct.setLinkId(parkingLink.getId());

							//EditRoute.globalEditRoute.addInitialPartToRoute(nextNonParkingAct.getEndTime(), parkingLink.getId(),
							//		nextCarLeg);
							
							aem.rerouteTask = new RerouteTaskDuringSim(nextNonParkingAct.getEndTime(), parkingLink.getId(),nextCarLeg);
							RerouteThreadDuringSim.addTask(aem.rerouteTask);
						}

						

				//	} else {
				//		aem.processLegInDefaultWay();
				//	}
					
					logParkingSearchDuration(aem);	
					
					AgentWithParking.parkingManager.parkVehicle(personId, parkingId);
					
					if (aem.getPlanElementIndex()==aem.getPlanElementIndexOfLastCarLeg()){
						handleLastParkingScore(aem);
					}
					
					aem.processEndOfLegCarMode_scheduleNextActivityEndEventIfNeeded(nextAct);
				}
			} else {
				aem.processLegInDefaultWay();
			}
		} else {

			setDurationOfParkingActivity(aem, nextAct);

			aem.processLegInDefaultWay();

		}

		// log search time and path! TODO:

		// TODO: add score only at end of search (store it locally during
		// search)!

		// if (aem.getPlanElementIndex() >1 && aem.getPlanElementIndex() % 2 ==
		// 0){
		// AgentWithParking.parkingStrategyManager.updateScore(person.getId(),
		// aem.getPlanElementIndex()-1, 1*rand.nextDouble());
		// }

		// only consider arrival distance at the moment for scoring (both in
		// future - but for this plans have to be pre-processed and cleaned
		// first).
	}

	private void setDurationOfParkingActivity(AgentWithParking aem, ActivityImpl nextAct) {
		if (nextAct.getType().equalsIgnoreCase("parking")) {
			nextAct.setEndTime(aem.getMessageArrivalTime() + parkingDuration);
		}
	}

	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		int i = random.nextInt(links.size());
		return links.get(i);
	}

	@Override
	public String getName() {
		return "RandomParkingSearch";
	}
	
	private void logParkingSearchDuration(AgentWithParking aem){
		Id personId = aem.getPerson().getId();
		double searchDuration=GeneralLib.getIntervalDuration(startSearchTime.get(personId),aem.getMessageArrivalTime());
		
		if (searchDuration==86400){
			searchDuration=0;
		}else {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		getParkingAttributesForScoring(aem).setParkingSearchDuration(searchDuration);
		startSearchTime.remove(personId);
	}

	/*
	public void handleParkingScoring(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		double searchDuration=GeneralLib.getIntervalDuration(startSearchTime.get(personId),aem.getMessageArrivalTime());
		if (aem.getPlanElementIndex() > 1 && aem.getPlanElementIndex() % 2 == 1) {
			if (startSearchTime.containsKey(personId)){
				AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), searchDuration);
			} else {
				AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), searchDuration);
			}
		}
		/*
		if (this instanceof RandomGarageParkingSearch){
			AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), 1000);
		} else {
			AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), 0);
		}
		*/
	/*
		startSearchTime.remove(personId);
	}
*/
	
	public void handleLastParkingScore(AgentWithParking aem){
		Id personId = aem.getPerson().getId();
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
		Id currentParkingId = AgentWithParking.parkingManager.getCurrentParkingId(aem.getPerson().getId());
		
		Activity act = (Activity) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getIndexOfFirstCarLegOfDay()-3);
		
		parkingAttributesForScoring.setFacilityId(currentParkingId);
		parkingAttributesForScoring.setParkingDuration(GeneralLib.getIntervalDuration(aem.getMessageArrivalTime(), act.getEndTime()));
		
		// just an approximation - TODO: update later
		parkingAttributesForScoring.setActivityDuration(parkingAttributesForScoring.getParkingDuration());
		
		double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(parkingAttributesForScoring);
		
		AgentWithParking.parkingStrategyManager.updateScore(personId, aem.getPlanElementIndex(), parkingScore);
		ZHScenarioGlobal.parkingEventDetails.add(new ParkingEventDetails(aem.getPlanElementIndex(), parkingScore, AgentWithParking.parkingStrategyManager.getParkingStrategyForCurrentLeg(aem.getPerson(), aem.getPlanElementIndex()) , parkingAttributesForScoring));
		parkingActAttributes.remove(personId);

	}
	
	
	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		Id personId = aem.getPerson().getId();
		ParkingActivityAttributes parkingAttributesForScoring = getParkingAttributesForScoring(aem);
		//handleParkingScoring(aem);	
		
		ActivityImpl currentActivity = aem.getCurrentActivity();
		
		//int nextLegIndex = aem.getPlanElementIndex() + 1;
		//Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(nextLegIndex);

		Id currentParkingId = AgentWithParking.parkingManager.getCurrentParkingId(aem.getPerson().getId());
		
		parkingAttributesForScoring.setFacilityId(currentParkingId);
		parkingAttributesForScoring.setParkingDuration(GeneralLib.getIntervalDuration(parkingAttributesForScoring.getParkingArrivalTime(), aem.getMessageArrivalTime()));
		
		// just an approximation - TODO: update later
		parkingAttributesForScoring.setActivityDuration(parkingAttributesForScoring.getParkingDuration());
		
		double parkingScore = ZHScenarioGlobal.parkingScoreEvaluator.getParkingScore(parkingAttributesForScoring);
		
		int legIndex = aem.duringAct_getPlanElementIndexOfPreviousCarLeg();
		AgentWithParking.parkingStrategyManager.updateScore(personId, legIndex, parkingScore);
		ZHScenarioGlobal.parkingEventDetails.add(new ParkingEventDetails(legIndex, parkingScore, AgentWithParking.parkingStrategyManager.getParkingStrategyForCurrentLeg(aem.getPerson(), legIndex) , parkingAttributesForScoring));
		parkingActAttributes.remove(personId);
		
	}
	
	// TODO: log parkingAttributesForScoring + score + leg index
	
	
	private ParkingActivityAttributes getParkingAttributesForScoring(AgentWithParking aem){
		Id personId = aem.getPerson().getId();
		if (!parkingActAttributes.containsKey(personId)){
			parkingActAttributes.put(personId, new ParkingActivityAttributes(personId));
		}
		return parkingActAttributes.get(personId);
	}

	@Override
	public void resetForNewIteration() {
		startSearchTime = new DoubleValueHashMap<Id>();
		parkingActAttributes=new HashMap<Id, ParkingActivityAttributes>();
		useSpecifiedParkingType=new HashMap<Id, String>();
	}

	/*
	 * public void processEndOfLegCarMode(Leg leg, ActivityImpl
	 * nextAct,AgentWithParking aem) { Event event;
	 * 
	 * List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
	 * Id currentLinkId=null; if (aem.getCurrentLinkIndex()==-1){
	 * currentLinkId=((LinkNetworkRouteImpl)leg.getRoute()).getStartLinkId(); }
	 * else { currentLinkId = linkIds.get(aem.getCurrentLinkIndex()); }
	 * 
	 * event=new
	 * LinkLeaveEvent(aem.getMessageArrivalTime(),aem.getPerson().getId
	 * (),currentLinkId,aem.getPerson().getId());
	 * Message.eventsManager.processEvent(event);
	 * 
	 * Id endLinkId = leg.getRoute().getEndLinkId(); event=new
	 * LinkEnterEvent(aem
	 * .getMessageArrivalTime(),aem.getPerson().getId(),endLinkId
	 * ,aem.getPerson().getId()); Message.eventsManager.processEvent(event);
	 * 
	 * event = new
	 * PersonArrivalEvent(aem.getMessageArrivalTime(),aem.getPerson()
	 * .getId(),endLinkId , leg.getMode());
	 * Message.eventsManager.processEvent(event);
	 * 
	 * aem.setPlanElementIndex(aem.getPlanElementIndex() + 1); boolean
	 * isLastActivity =
	 * aem.getPlanElementIndex()==aem.getPerson().getSelectedPlan
	 * ().getPlanElements().size()-1;
	 * 
	 * event = new
	 * ActivityStartEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(),
	 * endLinkId, nextAct.getFacilityId(), nextAct.getType());
	 * aem.eventsManager.processEvent(event);
	 * 
	 * 
	 * if (!isLastActivity){ double endTimeOfActivity =
	 * getEndTimeOfActivity(nextAct,getMessageArrivalTime());
	 * 
	 * setMessageArrivalTime(endTimeOfActivity); messageQueue.schedule(this); }
	 * }
	 */

}
