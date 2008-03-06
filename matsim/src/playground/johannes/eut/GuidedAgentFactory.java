/* *********************************************************************** *
 * project: org.matsim.*
 * GuidedAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.eut;

import java.util.Random;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.WithindayAgentLogicFactory;
import org.matsim.withinday.contentment.AgentContentmentI;
import org.matsim.withinday.routeprovider.RouteProvider;

/**
 * @author illenberger
 *
 */
public class GuidedAgentFactory extends WithindayAgentLogicFactory {

	private static double equipmentFraction = 0.05;
	
	private static final ForceReplan forceReplan = new ForceReplan();
	
	private static final PreventReplan preventReplan = new PreventReplan();
	
	public final ReactRouteGuidance router;
	
	public Random random;
	
	private EUTRouterAnalyzer analyzer;
	
	/**
	 * @param network
	 * @param scoringConfig
	 */
	public GuidedAgentFactory(NetworkLayer network,
			CharyparNagelScoringConfigGroup scoringConfig, TravelTimeI reactTTs) {
		super(network, scoringConfig);
		router = new ReactRouteGuidance(network, reactTTs);
	}

	public void setRouteAnalyzer(EUTRouterAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	@Override
	public AgentContentmentI createAgentContentment(WithindayAgent agent) {
		random.nextDouble();
		if(random.nextDouble() < equipmentFraction) {
			analyzer.addGuidedPerson(agent.getPerson());
			return forceReplan;
		} else
			return preventReplan;
	}

	@Override
	public RouteProvider createRouteProvider() {
		return router;
	}

}
