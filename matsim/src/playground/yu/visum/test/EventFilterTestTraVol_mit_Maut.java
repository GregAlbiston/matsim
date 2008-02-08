/*
 * $Id: EventFilterTestTraVol_mit_Maut.java,v 1.9 2007/11/11 01:25:42 mrieser Exp $
 */

/* *********************************************************************** *
 *                    org.matsim.demandmodeling.events                     *
 *                             EventsTest.java                             *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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

package playground.yu.visum.test;

import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;

import playground.yu.visum.filter.EventFilterAlgorithm;
import playground.yu.visum.filter.finalFilters.TraVolCal;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * @author yuchen
 */
public class EventFilterTestTraVol_mit_Maut {

	public static void testRunTraVolCal_mit_Maut() throws IOException {

		World world = Gbl.getWorld();
		Config config = Gbl.getConfig();

		// network
		System.out.println("  creating network object... ");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		NetworkLayer network = (NetworkLayer) world.createLayer(NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");

		System.out.println("  reading network file... ");
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");
		// plans
		System.out.println("  creating plans object... ");
		Plans plans = new Plans(Plans.USE_STREAMING);
		System.out.println("  done.");
		// events
		System.out.println("  creating events object... ");
		Events events = new Events();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		TraVolCal tvc = new TraVolCal(plans, network);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(tvc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos
		System.out.println("  reading events file and running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		// System.out.println("we have " + attcetf.getCount()
		// + " events at last -- LinkAveCalEventTimeFilter.");
		 System.out.println("we have " + tvc.getCount()
		 + " events at last -- TraVolCal.");
		System.out.println("  done.");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam("attribut_TraVol", "outputAttNetFile"));
		psUdaNet.output(tvc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamLinkATT psLinkAtt = new PrintStreamLinkATT(config.getParam("attribut_TraVol", "outputAttFile"),
				network);
		psLinkAtt.output(tvc);
		psLinkAtt.close();
		System.out.println("\tdone.");

		System.out.println("  done.");
	}

	/**
	 * @param args - test/yu/config_hms_mit_Maut.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		Gbl.startMeasurement();
		Gbl.createConfig(args);
		testRunTraVolCal_mit_Maut();
		Gbl.printElapsedTime();
	}
}
