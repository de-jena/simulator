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
package de.jena.publictransport.simulator.sensinact.mmt.util;

import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.eclipse.m2m.qvt.oml.blackbox.java.Operation;
import org.eclipse.sensinact.gateway.geojson.Coordinates;
import org.eclipse.sensinact.gateway.geojson.GeoJsonObject;
import org.eclipse.sensinact.gateway.geojson.Point;
import org.gecko.qvt.osgi.api.ModelTransformationConstants;
import org.osgi.service.component.annotations.Component;

import de.jena.udp.model.trafficos.common.TOSCommonPackage;

/**
 * 
 * @author ilenia
 * @since May 8, 2023
 */
@Component(service = LocationToSensinactGeoJsonBlackbox.class, immediate=true, 
property = {ModelTransformationConstants.QVT_BLACKBOX + "=true", 
		  ModelTransformationConstants.BLACKBOX_MODULENAME + "=LocationToSensinactGeoJson", 
		  ModelTransformationConstants.BLACKBOX_QUALIFIED_UNIT_NAME + "=de.jena.publictransport.simulator.sensinact.mmt.util.LocationToSensinactGeoJsonBlackbox"})
@Module(packageURIs={TOSCommonPackage.eNS_URI, "https://eclipse.org/sensinact/core/provider/1.0"})
public class LocationToSensinactGeoJsonBlackbox {

	
	@Operation(description = "Converts from lat and lng to the Sensinact GeoJson Point object")
	public GeoJsonObject getGeoJson(Double lat, Double lng) {
		if(lat !=  null && lng != null) {
			Point point = new Point();
			point.coordinates = new Coordinates();
			point.coordinates.latitude = lat;
			point.coordinates.longitude = lng;
			return point;
		}
		return null;
	}
}
