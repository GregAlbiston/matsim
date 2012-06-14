package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the SINGLE DEPOT DISTRIBUTION/DELIVERY vrp problem.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.DistributionTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.constraints.PickORDeliveryCapacityAndTWConstraint;
import org.matsim.core.gbl.MatsimRandom;

public class DTWSolverFactory implements VRPSolverFactory{
	
	public List<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();

	private Random random = MatsimRandom.getRandom();
	
	private int iterations = 200;
	
	private int warmupIterations = 20;
	
	public DTWSolverFactory() {

	}
	
	public DTWSolverFactory(int iterations, int warmupIterations) {
		super();
		this.iterations = iterations;
		this.warmupIterations = warmupIterations;
	}



	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles, Network network, Costs costs) {
		verifyDistributionProblem(shipments,carrierVehicles);
		DTWSolver rrSolver = new DTWSolver(shipments, carrierVehicles, costs, network, new InitialSolution());
		DistributionTourWithTimeWindowsAlgoFactory ruinAndRecreateFactory = new DistributionTourWithTimeWindowsAlgoFactory();
		addListeners(ruinAndRecreateFactory);
		rrSolver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
		rrSolver.setnOfWarmupIterations(warmupIterations);
		rrSolver.setnOfIterations(iterations);
		PickORDeliveryCapacityAndTWConstraint constraints = new PickORDeliveryCapacityAndTWConstraint();
		rrSolver.setGlobalConstraints(constraints);
		
		return rrSolver;
	}

	private void verifyDistributionProblem(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles) {
		Id location = null;
		for(CarrierVehicle v : carrierVehicles){
			if(location == null){
				location = v.getLocation();
			}
			else if(!location.toString().equals(v.getLocation().toString())){
				throw new IllegalStateException("if you use this solver " + this.getClass().toString() + "), all vehicles must have the same depot-location. vehicle " + v.getVehicleId() + " has not.");
			}
		}
		for(CarrierShipment s : shipments){
			if(location == null){
				return;
			}
			if(!s.getFrom().toString().equals(location.toString())){
				throw new IllegalStateException("if you use this solver, all shipments must have the same from-location. errorShipment " + s);
			}
		}
		
	}

	private void addListeners(DistributionTourWithTimeWindowsAlgoFactory ruinAndRecreateFactory) {
		for(RuinAndRecreateListener l : listeners){
			ruinAndRecreateFactory.addRuinAndRecreateListener(l);
		}
		
	}

}
