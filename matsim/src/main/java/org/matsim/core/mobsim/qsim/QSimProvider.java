/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QSimProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.NamedComponentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger(QSimProvider.class);

	private Injector injector;
	private Config config;
	private Collection<AbstractQSimModule> modules;
	private QSimComponents components;

	@Inject
	QSimProvider(Injector injector, Config config, Collection<AbstractQSimModule> modules,
			QSimComponents components) {
		this.injector = injector;
		this.modules = modules;
		this.config = config;
		this.components = components;
	}

	@Override
	public QSim get() {
		modules.forEach(m -> m.setConfig(config));

		com.google.inject.AbstractModule module = new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				for (AbstractQSimModule module : modules) {
					install(module);
				}
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};

		Injector qSimLocalInjector = injector.createChildInjector(module);
		org.matsim.core.controler.Injector.printInjector(qSimLocalInjector, log);
		QSim qSim = qSimLocalInjector.getInstance(QSim.class);
//        qSim.setChildInjector( qSimLocalInjector ) ;

		ComponentRegistry<MobsimEngine> mobsimEngineRegistry = new ComponentRegistry<>("MobsimEngine");
		ComponentRegistry<ActivityHandler> activityHandlerRegister = new ComponentRegistry<>("ActivityHandler");
		ComponentRegistry<DepartureHandler> departureHandlerRegistry = new ComponentRegistry<>("DepartureHandler");
		ComponentRegistry<AgentSource> agentSourceRegistry = new ComponentRegistry<>("AgentSource");

		NamedComponentUtils.find(qSimLocalInjector, MobsimEngine.class)
				.forEach(mobsimEngineRegistry::register);
		NamedComponentUtils.find(qSimLocalInjector, ActivityHandler.class)
				.forEach(activityHandlerRegister::register);
		NamedComponentUtils.find(qSimLocalInjector, DepartureHandler.class)
				.forEach(departureHandlerRegistry::register);
		NamedComponentUtils.find(qSimLocalInjector, AgentSource.class)
				.forEach(agentSourceRegistry::register);

		mobsimEngineRegistry.getOrderedComponents(components.activeMobsimEngines).stream()
				.map(qSimLocalInjector::getInstance).forEach(qSim::addMobsimEngine);
		activityHandlerRegister.getOrderedComponents(components.activeActivityHandlers).stream()
				.map(qSimLocalInjector::getInstance).forEach(qSim::addActivityHandler);
		departureHandlerRegistry.getOrderedComponents(components.activeDepartureHandlers).stream()
				.map(qSimLocalInjector::getInstance).forEach(qSim::addDepartureHandler);
		agentSourceRegistry.getOrderedComponents(components.activeAgentSources).stream()
				.map(qSimLocalInjector::getInstance).forEach(qSim::addAgentSource);

		NamedComponentUtils.find(qSimLocalInjector, MobsimListener.class).forEach((name, component) -> {
			qSim.addQueueSimulationListeners(qSimLocalInjector.getInstance(component));
		});

		return qSim;
	}

}
