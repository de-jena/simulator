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

import java.time.Instant;
import java.util.Date;

import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.eclipse.m2m.qvt.oml.blackbox.java.Operation;
import org.gecko.qvt.osgi.api.ModelTransformationConstants;
import org.osgi.service.component.annotations.Component;

/**
 * 
 * @author ilenia
 * @since May 8, 2023
 */
@Component(service = DateToInstantBlackbox.class, immediate=true, 
property = {ModelTransformationConstants.QVT_BLACKBOX + "=true", 
		  ModelTransformationConstants.BLACKBOX_MODULENAME + "=DateToInstant", 
		  ModelTransformationConstants.BLACKBOX_QUALIFIED_UNIT_NAME + "=de.jena.publictransport.simulator.sensinact.mmt.util.DateToInstantBlackbox"})
@Module(packageURIs={"http://www.eclipse.org/emf/2002/Ecore", "https://eclipse.org/sensinact/core/provider/1.0"})
public class DateToInstantBlackbox {
	
	@Operation(description = "Converts from java.util.Date to Instant")
	public Instant getInstant(Date date) {
		if(date !=  null) {
			return date.toInstant();
		}
		return null;
	}

	@Operation(description = "Converts from milliseconds to Instant")
	public Instant getInstant(Long millis) {
		if(millis !=  null) {
			return Instant.ofEpochMilli(millis);
		}
		return null;
	}
}
