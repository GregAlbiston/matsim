/* *********************************************************************** *
 * project: org.matsim.*
 * TTDecorator.java
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

import java.util.LinkedList;
import java.util.List;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.network.Link;
import org.matsim.router.util.TravelTimeI;

/**
 * @author illenberger
 *
 */
public class TTDecorator implements TravelTimeI {

	private TravelTimeI meantts;
	
	private List<BasicLinkI> accidantLinks = new LinkedList<BasicLinkI>();
	
//	public TTDecorator(TravelTimeI traveltimes) {
//		this.meantts = traveltimes;
//	}
	
	public void setMeanTravelTimes(TravelTimeI meantts) {
		this.meantts = meantts;
	}
	
	public void addAccidantLink(BasicLinkI link) {
		accidantLinks.add(link);
	}
	
	public void removeAccidantLink(BasicLinkI link) {
		accidantLinks.remove(link);
	}
	
	public double getLinkTravelTime(Link link, double time) {
		if(accidantLinks.contains(link)) {
			return Double.MAX_VALUE;
		} else
			return meantts.getLinkTravelTime(link, time);
	}

}
