/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.jena.publictransport.simulator;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gecko.osgi.messaging.MessagingService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.util.promise.PromiseFactory;

import de.dim.trafficos.apis.PublicTransportService;
import de.jena.publictransport.simulator.helper.PublicTransportSimulatorHelper;
import de.jena.udp.model.trafficos.common.IdNameElement;
import de.jena.udp.model.trafficos.common.TOSCommonFactory;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDataValue;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDataValueObject;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDataValueType;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDoorChange;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDoorChangeType;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDoorCount;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDoorCountingType;
import de.jena.udp.model.trafficos.publictransport.PublicTransportLine;
import de.jena.udp.model.trafficos.publictransport.PublicTransportPosition;
import de.jena.udp.model.trafficos.publictransport.PublicTransportStation;
import de.jena.udp.model.trafficos.publictransport.PublicTransportStopRequested;
import de.jena.udp.model.trafficos.publictransport.PublicTransportTimeTableEntry;
import de.jena.udp.model.trafficos.publictransport.TOSPublicTransportFactory;
import org.gecko.emf.json.constants.EMFJs;

@Component(name="PublicTransportSimulator", service = PublicTransportSimulator.class )
public class PublicTransportSimulator {
	
	@Reference(target = "(id=full)")
	private MessagingService messaging;
	
	@Reference(target="(&(emf.model.name=publictransport)(emf.resource.configurator.name=EMFJson))", scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private ResourceSet resourceSet;

	@Reference
	PublicTransportService publicTransportService;
	
//	This is the simulation interval in minutes
	private static final int SIMULATION_INTERVAL_MN = 1;
	
	private static final Logger LOGGER = Logger.getLogger(PublicTransportSimulator.class.getName());
	
	ExecutorService executors = Executors.newCachedThreadPool();
	PromiseFactory promiseFactory = new PromiseFactory(executors);
	
	PublicTransportLine line2;
	AtomicInteger timeInMins;
	
	private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> simulationFuture;
	private ReentrantLock simLock = new ReentrantLock(true);
	private boolean isRunning;
	
	IdNameElement lineRef = TOSCommonFactory.eINSTANCE.createIdNameElement();
	
	Predicate<PublicTransportTimeTableEntry> predicate = tte -> {
		if(tte.getBegin() == timeInMins.get()) return true;
		else if(tte.getEnd() == timeInMins.get()) return true;
		else if(tte.getStops().stream()
				.filter(st -> st.getExpectedArrivalTime() == timeInMins.get())
				.findFirst().isPresent()) return true;	
		return false;
	};
	
	Function<PublicTransportTimeTableEntry, PublicTransportStation> findStation = tte -> {
		if(tte.getBegin() == timeInMins.get()) return tte.getFirstStation();
		if(tte.getEnd() == timeInMins.get()) return tte.getLastStation();
		Optional<PublicTransportStation> stationOpt = tte.getStops().stream()
				.filter(st -> st.getExpectedArrivalTime() == timeInMins.get())
				.map(st -> st.getStation())
				.findFirst();
		if(stationOpt.isPresent()) return stationOpt.get();
		return null;
	};
	
	@Activate
	public void activate() {
		startSimulation();
	}
	
	@Deactivate
	public void deactivate() {
		if(simulationFuture == null) {
			stopSimulation();
			ses.shutdown(); 
			try {
				if (!ses.awaitTermination(60, TimeUnit.SECONDS)) {
					ses.shutdownNow(); 
				}
			} catch (InterruptedException ie) {
				ses.shutdownNow();
				Thread.currentThread().interrupt();
			}	finally {
				simulationFuture = null;
			}
		}		
	}

	public void startSimulation() {
		line2 = publicTransportService.getPublicTransportLineByName("Tram Line 2");		
		if(line2 == null) {
			LOGGER.warning("No sample Tram found in db. Cannot start simulation. We generate it first.");
			line2 = PublicTransportSimulatorHelper.createSimulatedData();
			line2 = publicTransportService.savePublicTransportLine(line2);
		}
		lineRef.setId(line2.getId());
		lineRef.setName(line2.getName());
		promiseFactory.submit(() -> {
			LOGGER.info("Scheduling Simluation");
			simulationFuture = ses.scheduleAtFixedRate(this::simulate, 0, SIMULATION_INTERVAL_MN, TimeUnit.MINUTES);
			isRunning = true;
			return true;
		});
	}
	
	public void stopSimulation() {
		simulationFuture.cancel(true);
		while(!simulationFuture.isDone()) {
			try {
				Thread.sleep(50l);
			} catch (InterruptedException e) {
				LOGGER.severe(String.format("Simulation stopping was interrupted"));
			}
		}
		isRunning = false;
		LOGGER.info("Simulation stopped");
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	private void simulate() {
		if (simLock.tryLock()) {
			try {
				doSimulate();
			} finally {
				simLock.unlock();
			}
		} else {
			LOGGER.warning(String.format("Simulation step is currently in progress, waiting"));
		}		
	}
	
	private void doSimulate() {
		timeInMins = new AtomicInteger(Calendar.getInstance(Locale.GERMANY).get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE));
		LOGGER.info("Simulate for minute of day " + timeInMins);
		List<PublicTransportTimeTableEntry> entries = line2.getTimeTable().stream()
				.map(tt -> tt.getEntry()).flatMap(tte -> tte.stream())
				.filter(tte -> tte.getEnd() >= timeInMins.get() && tte.getBegin() <= timeInMins.get()).toList();
		
		if(entries.isEmpty()) {
			LOGGER.info("No simulated trams are running at this time. Please, wait a couple of minutes.");
			return;
		}
		generateAndSendData(entries);
	}


	private void generateAndSendData(List<PublicTransportTimeTableEntry> entries) {
		entries.forEach(e -> executors.submit(() -> doGenerateAndSendData(e)));
	}


	private void doGenerateAndSendData(PublicTransportTimeTableEntry e) {
		PublicTransportStation station = findStation.apply(e);
		if(station == null) {
			LOGGER.info("We are not at a stop. Generate random stop request msg...");
			int random = getRandomInt(0, 1);
			if(random == 0) return;
			PublicTransportStopRequested stopRequested = TOSPublicTransportFactory.eINSTANCE.createPublicTransportStopRequested();
			stopRequested.setId(UUID.randomUUID().toString());
			stopRequested.setName("Stop Requested for Line 2 - trip " + e.getIndex());
			stopRequested.setStopRequested(true);
			doSendData(e.getIndex(), PublicTransportDataValueType.STOP_REQUESTED, stopRequested);
			return;
		}
		PublicTransportPosition position = TOSPublicTransportFactory.eINSTANCE.createPublicTransportPosition();
		position.setPosition(station.getPosition());
		position.setName("Position Update for Line 2 - trip " + e.getIndex());
		position.setId(UUID.randomUUID().toString());
		position.setAtStop(true);
		position.setStationName(station.getName());
		doSendData(e.getIndex(), PublicTransportDataValueType.GEO_INFO, position);
		
//		Door Open (assuming every tram has 4 doors per side)
		for(int i = 0; i < 4; i++) {
			PublicTransportDoorChange doorOpen = TOSPublicTransportFactory.eINSTANCE.createPublicTransportDoorChange();
			doorOpen.setDoorSide(station.getDoorSide());
			doorOpen.setType(PublicTransportDoorChangeType.DOOR_OPEN);
			doorOpen.setDoorId(String.valueOf(i));
			doorOpen.setName("Door Change Update for Line 2 - trip " + e.getIndex() + " - door " + doorOpen.getDoorId());
			doorOpen.setId(UUID.randomUUID().toString());
			doSendData(e.getIndex(), PublicTransportDataValueType.DOOR_CHANGE, doorOpen);
			
//			For every door we need to generate some count of passengers that goes in and out
			int num = getRandomInt(0,6), in = getRandomInt(0,7), out = getRandomInt(0,7);
			if(in == 0 && out == 0) continue;
			PublicTransportDoorCount doorCount = TOSPublicTransportFactory.eINSTANCE.createPublicTransportDoorCount();
			doorCount.setDoorId(String.valueOf(i));
			doorCount.setDoorSide(station.getDoorSide());
			doorCount.setType(getDoorCountingType(num));
			doorCount.setIn(in);
			doorCount.setOut(out);
			doorCount.setName("Door Count Update for Line 2 - trip " + e.getIndex() + " - door " + doorOpen.getDoorId());
			doorCount.setId(UUID.randomUUID().toString());
			doSendData(e.getIndex(), PublicTransportDataValueType.DOOR_COUNT, doorCount);
		}
		
//		We simulate the time the doors stay open
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
//		Door Close (assuming every tram has 4 doors per side)
		for(int i = 0; i < 4; i++) {
			PublicTransportDoorChange doorClose = TOSPublicTransportFactory.eINSTANCE.createPublicTransportDoorChange();
			doorClose.setDoorSide(station.getDoorSide());
			doorClose.setType(PublicTransportDoorChangeType.DOOR_CLOSE);
			doorClose.setDoorId(String.valueOf(i));
			doorClose.setName("Door Change Update for Line 2 - trip " + e.getIndex() + " - door " + doorClose.getDoorId());
			doorClose.setId(UUID.randomUUID().toString());
			doSendData(e.getIndex(), PublicTransportDataValueType.DOOR_CHANGE, doorClose);

		}
	}
	
	private void doSendData(int timeTableRef, PublicTransportDataValueType dataValueType, PublicTransportDataValueObject dataValueObject) {
		PublicTransportDataValue dataValue = TOSPublicTransportFactory.eINSTANCE.createPublicTransportDataValue();
		dataValue.setTimestamp(new Date());
		dataValue.setLineRef(EcoreUtil.copy(lineRef));
		dataValue.setTimeTableEntryRef(timeTableRef);
		dataValue.setType(dataValueType);
		dataValue.setValue(dataValueObject);
		Resource resource = resourceSet.createResource(URI.createURI("temp_"+UUID.randomUUID().toString()+".json"));
		resource.getContents().add(dataValue);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Map<String, Object> saveOptions = new HashMap<String, Object>();
		saveOptions.put(EMFJs.OPTION_SERIALIZE_DEFAULT_VALUE, Boolean.TRUE);
		saveOptions.put("PROXY_ATTRIBUTES", Boolean.TRUE);
		try {
			resource.save(baos, saveOptions);
			resource.getContents().clear();			
			byte[] content = baos.toByteArray();
			ByteBuffer buffer = ByteBuffer.wrap(content);
			LOGGER.info("Sending mqtt to " + "5g/public/transport/data/entry/"+timeTableRef+"/"+dataValueType.getLiteral());
			messaging.publish("5g/public/transport/data/entry/"+timeTableRef+"/"+dataValueType.getLiteral(), buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error publishing position request via MQTT", e);
		}
		
	}
	
	private int getRandomInt(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	private PublicTransportDoorCountingType getDoorCountingType(int num) {
		switch(num) {
		case 0: return PublicTransportDoorCountingType.ADULT;
		case 1: return PublicTransportDoorCountingType.CHILD;
		case 2: return PublicTransportDoorCountingType.BIKE;
		case 3: return PublicTransportDoorCountingType.WHEEL_CHAIR;
		case 4: return PublicTransportDoorCountingType.PARAM;
		case 5: return PublicTransportDoorCountingType.UNIDENTIFIED;
		case 6: default: return PublicTransportDoorCountingType.OTHER;
		}
	}
}
