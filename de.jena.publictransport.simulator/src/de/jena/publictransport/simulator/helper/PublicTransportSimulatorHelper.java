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
package de.jena.publictransport.simulator.helper;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import de.dim.trafficos.common.model.common.Position;
import de.dim.trafficos.common.model.common.ScheduleModeType;
import de.dim.trafficos.common.model.common.TOSCommonFactory;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportDoorSideType;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportLine;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportStation;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportStop;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportTimeTable;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportTimeTableEntry;
import de.jena.upd.trafficos.publictransport.model.publictransport.PublicTransportType;
import de.jena.upd.trafficos.publictransport.model.publictransport.TOSPublicTransportFactory;

public class PublicTransportSimulatorHelper {	
	
	private static final String EXAMPLE_TRAM_LINE = "Tram Line 2";
	private static final List<PublicTransportStopPojo> TRAM_STOPS_LIST = List.of(
			new PublicTransportStopPojo("Jena-Ost", 50.93367568412728, 11.619972723749285, PublicTransportDoorSideType.LEFT),
			new PublicTransportStopPojo("Jenzigweg", 50.93413878689906, 11.607677182160305, PublicTransportDoorSideType.RIGHT),
			new PublicTransportStopPojo("Geschwister-Scholl-Straße", 50.92930817895859, 11.600225314835498, PublicTransportDoorSideType.LEFT),
			new PublicTransportStopPojo("Steinweg", 50.92872721484323, 11.591505235105968, PublicTransportDoorSideType.RIGHT),
			new PublicTransportStopPojo("Stadtzentrum Löbdergraben", 50.92711808389636, 11.587767425103504, PublicTransportDoorSideType.LEFT),
			new PublicTransportStopPojo("Paradiesbahnhof West", 50.92480529943012, 11.586045394978049, PublicTransportDoorSideType.RIGHT),
			new PublicTransportStopPojo("Enver-Simsek-Platz", 50.90015457967361, 11.578732263456944, PublicTransportDoorSideType.LEFT),
			new PublicTransportStopPojo("Winzerla", 50.90696133195654, 11.576601698347202, PublicTransportDoorSideType.RIGHT)
			);
	private static final List<LocalTime> TRAM_STOPS_TIMES_LIST = List.of(
			LocalTime.of(7, 3),
			LocalTime.of(7, 5),
			LocalTime.of(7, 8),
			LocalTime.of(7, 10),
			LocalTime.of(7, 12),
			LocalTime.of(7, 15),
			LocalTime.of(7, 21),
			LocalTime.of(7, 23),
			LocalTime.of(7, 24));
	

	public static PublicTransportLine createSimulatedData() {
		PublicTransportLine line = TOSPublicTransportFactory.eINSTANCE.createPublicTransportLine();
		line.setName(EXAMPLE_TRAM_LINE);
		line.setId(UUID.randomUUID().toString());
		line.setLineNumber("2");
		line.setType(PublicTransportType.TRAM);
		List<PublicTransportTimeTable> timeTables = createTimeTables(); 
		line.getTimeTable().addAll(timeTables);
		return line;
	}
	
	private static List<PublicTransportTimeTable> createTimeTables() {
//		for now we create only one
		PublicTransportTimeTable timeTable = TOSPublicTransportFactory.eINSTANCE.createPublicTransportTimeTable();
		timeTable.setId(UUID.randomUUID().toString());
		timeTable.setName("Working Day: Jena-Ost - Stadtzentrum - Winzerla");
		timeTable.setType(ScheduleModeType.WORKING_DAY);
		
//		Here we should repeat the timeTable for every hour and every 10 minutes from 7 to 20
//		looking at https://www.stadtwerke-jena.de/dam/jcr:e5abf00a-7717-4724-94f6-0a68cf7fbdfd/02.pdf
//		This loops between 7 and 20 hours, so 14 rounds
		int index = 0;
		for(int h = 0; h < 14; h++) {
			int hoursToAdd = h;
//			this loops over minutes
			for(int m = 0; m < 4; m++) {
				long minutesToAdd = m*10;				
				List<LocalTime> times = TRAM_STOPS_TIMES_LIST.stream().map(t -> t.plusHours(hoursToAdd))
						.map(t -> t.plusMinutes(minutesToAdd)).toList();
				PublicTransportTimeTableEntry timeTableEntry = TOSPublicTransportFactory.eINSTANCE.createPublicTransportTimeTableEntry();
				timeTableEntry.setIndex(index++);
				timeTableEntry.setFirstStation(createPublicTransportStation(TRAM_STOPS_LIST.get(0)));
				timeTableEntry.setLastStation(createPublicTransportStation(TRAM_STOPS_LIST.get(TRAM_STOPS_LIST.size()-1)));
				timeTableEntry.setBegin(getMinuteOfTheDay(times.get(0)));
				timeTableEntry.setEnd(getMinuteOfTheDay(times.get(times.size()-1)));
				for(int i = 1; i < TRAM_STOPS_LIST.size()-1; i++) {
					PublicTransportStop stop = createPublicTransportStop(TRAM_STOPS_LIST.get(i), times.get(i));
					timeTableEntry.getStops().add(stop);
				}
				timeTable.getEntry().add(timeTableEntry);
			}
		}		
		return List.of(timeTable);
	}

	private static long getMinuteOfTheDay(LocalTime localTime) {
		return localTime.toSecondOfDay()/60;
	}
	
	private static PublicTransportStop createPublicTransportStop(PublicTransportStopPojo publicTransportStopPojo, LocalTime expectedArrivalTime) {
		PublicTransportStop stop = TOSPublicTransportFactory.eINSTANCE.createPublicTransportStop();
		stop.setStation(createPublicTransportStation(publicTransportStopPojo));
		stop.setExpectedArrivalTime(getMinuteOfTheDay(expectedArrivalTime));
		return stop;
	}
	
	private static PublicTransportStation createPublicTransportStation(PublicTransportStopPojo publicTransportStopPojo) {
		PublicTransportStation station = TOSPublicTransportFactory.eINSTANCE.createPublicTransportStation();
		station.setId(UUID.randomUUID().toString());
		station.setName(publicTransportStopPojo.getName());
		station.setDoorSide(publicTransportStopPojo.doorOpeningSide());
		Position position = TOSCommonFactory.eINSTANCE.createPosition();
		position.setLatitude(publicTransportStopPojo.getLatitude());
		position.setLongitude(publicTransportStopPojo.getLongitude());
		station.setPosition(position);
		return station;
	}
}
