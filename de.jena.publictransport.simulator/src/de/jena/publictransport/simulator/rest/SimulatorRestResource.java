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
package de.jena.publictransport.simulator.rest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.annotations.RequireJakartarsWhiteboard;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.osgi.service.servlet.whiteboard.annotations.RequireHttpWhiteboard;

import de.jena.publictransport.simulator.PublicTransportSimulator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * 
 * @author ilenia
 * @since May 24, 2023
 */
@RequireJakartarsWhiteboard
@RequireHttpWhiteboard
@JakartarsResource
@Component(immediate=true, name = "SimulatorRestResource", service = SimulatorRestResource.class)
@Path("")
public class SimulatorRestResource {
	
	@Reference
	PublicTransportSimulator simulator;

	@GET
	@Path("/start")
	public Response startSimulator() {
		simulator.startSimulation();
		return Response.ok().entity("Simulation started!").build();
	}
	
	@GET
	@Path("/stop")
	public Response stopSimulator() {
		simulator.stopSimulation();
		return Response.ok().entity("Simulation stoped!").build();
	}
	
	@GET
	@Path("/running")
	public Response checkSimulator() {
		
		if(simulator.isRunning()) {
			return Response.ok().entity("Simulation is running!").build();
		}
		return Response.ok().entity("Simulation is NOT running").build();
	}

}
