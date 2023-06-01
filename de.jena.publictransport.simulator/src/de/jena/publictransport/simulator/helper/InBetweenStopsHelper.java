/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author ilenia
 * @since May 31, 2023
 */
public class InBetweenStopsHelper {
	public static final Map<String, List<Double[]> > betweenStopsPositions = 
			Map.ofEntries(Map.entry("Jena-Ost-Jenzigweg", 
			List.of(new Double[]{11.6190164,50.9336656}, new Double[]{11.6179006,50.933571}, new Double[]{11.6167633,50.9335169}, new Double[]{11.615154,50.9334898},
					new Double[]{11.6134803,50.933598}, new Double[]{11.6120426,50.9338009}, new Double[] {11.610562,50.9339631}, new Double[]{11.6087811,50.9341389})),
			Map.entry("Jenzigweg-Geschwister-Scholl-Straße", 
			List.of(new Double[]{11.6071825,50.9339733}, new Double[]{11.6068499,50.9337096}, new Double[]{11.6066246,50.9334865}, new Double[]{11.6063885,50.9332295},
					new Double[]{11.6061954,50.9330064}, new Double[]{11.6057877,50.932479}, new Double[] {11.6055624,50.932222}, new Double[]{11.605026,50.9317217},
					new Double[]{11.6041355,50.9312281}, new Double[]{11.603599,50.9309238}, new Double[] {11.6024618,50.9303287}, new Double[]{11.601464,50.929808})),
			Map.entry("Geschwister-Scholl-Straße-Steinweg", 
			List.of(new Double[]{11.5997581,50.9291183}, new Double[]{11.5988569,50.9287937}, new Double[]{11.5978055,50.9286044}, new Double[]{11.596754,50.9284759},
					new Double[]{11.5957348,50.9285029}, new Double[]{11.5948121,50.9285503}, new Double[] {11.5937178,50.9285909}, new Double[]{11.5925054,50.9286179})),
			Map.entry("Steinweg-Stadtzentrum Löbdergraben", 
			List.of(new Double[]{11.5908746,50.928672}, new Double[]{11.5906064,50.9285435}, new Double[]{11.5905742,50.9283542}, new Double[]{11.5905528,50.9281175},
					new Double[]{11.5904562,50.9275765}, new Double[]{11.5901773,50.9271302}, new Double[] {11.58904,50.9271167}, new Double[]{11.5882031,50.9270558})),
			Map.entry("Stadtzentrum Löbdergraben-Paradiesbahnhof West", 
			List.of(new Double[]{11.587538,50.9270287}, new Double[]{11.5872376,50.9269949}, new Double[]{11.5870122,50.9269746}, new Double[]{11.5867548,50.9268664},
					new Double[]{11.5865509,50.9267244}, new Double[]{11.5864651,50.9265689}, new Double[] {11.5864651,50.9263795}, new Double[]{11.5865831,50.9262037},
					new Double[]{11.5868084,50.9259264}, new Double[]{11.5868835,50.9255274}, new Double[] {11.586905,50.9251284}, new Double[]{11.5864973,50.924885})),
			Map.entry("Paradiesbahnhof West-Enver-Simsek-Platz", 
			List.of(new Double[]{11.5852205,50.9243947}, new Double[]{11.5845124,50.9240971}, new Double[]{11.583697,50.9236508}, new Double[]{11.5829246,50.9229203},
					new Double[]{11.5825169,50.9224469}, new Double[]{11.5820663,50.9220006}, new Double[] {11.5815513,50.9213107}, new Double[]{11.5811221,50.9207561},
					new Double[]{11.580929,50.9202827}, new Double[]{11.5805213,50.9194981}, new Double[] {11.5799634,50.9187406}, new Double[]{11.5796201,50.9181724},
					new Double[]{11.5790193,50.9170631}, new Double[]{11.5785043,50.9163326}, new Double[]{11.5771739,50.915548}, new Double[]{11.5762941,50.9149933},
					new Double[]{11.5759508,50.9144251}, new Double[]{11.5756933,50.9134105}, new Double[] {11.57535,50.9122605}, new Double[]{11.5754573,50.9114758},
					new Double[]{11.5757362,50.9107046}, new Double[]{11.5761654,50.9099469}, new Double[] {11.57638,50.909284}, new Double[]{11.5764443,50.9086345})),
			Map.entry("Enver-Simsek-Platz-Winzerla", 
			List.of(new Double[]{11.576895,50.9063478}, new Double[]{11.5770022,50.905563}, new Double[]{11.5771739,50.9051436}, new Double[]{11.5773241,50.9045211},
					new Double[]{11.5773241,50.9040069}, new Double[]{11.5776245,50.9028296}, new Double[] {11.5779464,50.9019501}, new Double[]{11.578397,50.9009487})));

	private static Map<String, AtomicInteger> counterMap = new HashMap<>();
	
	public static Double[] getInBetweenStops(String station1, String station2, int tripRef) {
		
		if(!counterMap.containsKey(station1+"-"+station2+"-"+tripRef)) {
			counterMap.put(station1+"-"+station2+"-"+tripRef, new AtomicInteger(0));
		}
//		This means we do not have enough points in between the stops for the simulation, so we keep sending back the last one, as if the tram was at a red traffic light
		if(counterMap.get(station1+"-"+station2+"-"+tripRef).get() > betweenStopsPositions.get(station1+"-"+station2).size()-1) {
			counterMap.get(station1+"-"+station2+"-"+tripRef).set(betweenStopsPositions.get(station1+"-"+station2).size()-1);
		}
		Double[] coord = betweenStopsPositions.get(station1+"-"+station2).get(counterMap.get(station1+"-"+station2+"-"+tripRef).getAndIncrement());
		return coord;		
	}
	
	public static void resetCounterForTrip(int tripRef, String station) {
		counterMap.entrySet().stream().filter(e -> e.getKey().endsWith(station+"-"+tripRef)).findFirst().get().getValue().set(0);
	}
}
