/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.router.util;

import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelCost;

public abstract class KnowledgeTravelCost implements TravelCost, Cloneable {

	protected PersonImpl person;
	protected QueueNetwork queueNetwork;
	
	public double getLinkTravelCost(LinkImpl link, double time, PersonImpl person)
	{
		this.person = person;
		return getLinkTravelCost(link, time);
	}
	
	public void setPerson(PersonImpl person)
	{
		this.person = person;
	}
	
	public PersonImpl getPerson()
	{
		return this.person;
	}

	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}
	
	public QueueNetwork getQueueNetwork()
	{
		return this.queueNetwork;
	}
	
	@Override
	public KnowledgeTravelCost clone()
	{
		return this;
	}
}