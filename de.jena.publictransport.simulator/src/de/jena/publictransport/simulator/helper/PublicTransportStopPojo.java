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

import de.jena.udp.model.trafficos.publictransport.PublicTransportDoorSideType;

/**
 * 
 * @author ilenia
 * @since May 4, 2023
 */
public record PublicTransportStopPojo(String name, Double lat, Double lng, PublicTransportDoorSideType doorOpeningSide) {
	
	 public String getName() {
		 return name;
	 }

	 public Double getLatitude() {
		 return lat;
	 }
	 
	 public Double getLongitude() {
		 return lng;
	 }
	 
	 public PublicTransportDoorSideType getDoorOpeningSide() {
		 return doorOpeningSide;
	 }
}
