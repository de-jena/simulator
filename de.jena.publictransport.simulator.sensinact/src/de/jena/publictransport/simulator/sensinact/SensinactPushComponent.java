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
package de.jena.publictransport.simulator.sensinact;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.sensinact.prototype.PrototypePush;
import org.gecko.core.pool.Pool;
import org.gecko.osgi.messaging.Message;
import org.gecko.osgi.messaging.MessagingService;
import org.gecko.qvt.osgi.api.ConfigurableModelTransformatorPool;
import org.gecko.qvt.osgi.api.ModelTransformator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.util.pushstream.PushStream;

import de.jena.model.sensinact.ibis.IbisAdmin;
import de.jena.model.sensinact.ibis.IbisDevice;
import de.jena.model.sensinact.ibis.IbisSensinactFactory;
import de.jena.udp.model.trafficos.publictransport.PublicTransportDataValue;

@Component(immediate=true, name="PublicTransportSensinactPushComponent")
public class SensinactPushComponent {
	
	@Reference
	private PrototypePush sensinact;
	
	@Reference(target = ("(pool.componentName=simulatorModelTransformatorService)"))
	private ConfigurableModelTransformatorPool poolComponent;

	private ResourceSet resourceSet;
	private PushStream<Message> subscription;
	
	@Activate
	public SensinactPushComponent(BundleContext bctx, @Reference(target = "(id=full)") MessagingService messaging,
			@Reference(target = "(&(emf.model.name=publictransport)(emf.resource.configurator.name=EMFJson))", scope = ReferenceScope.PROTOTYPE_REQUIRED) ResourceSet resourceSet) {
		this.resourceSet = resourceSet;
		try {
			subscription = messaging.subscribe("public/transport/data/entry/#");
			subscription.forEach(this::handlePublicTransportMessage);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void handlePublicTransportMessage(Message message) {

		Resource resource = resourceSet.createResource(URI.createURI("temp_"+UUID.randomUUID().toString()+".json"));
		Map<String, Object> saveOptions = new HashMap<String, Object>();
		saveOptions.put("OPTION_SERIALIZE_DEFAULT_VALUE", Boolean.TRUE);
		try {

			byte[] content = message.payload().array();
			System.out.println("Recieved Public Transport Message: " + message.topic());
			ByteArrayInputStream bais = new ByteArrayInputStream(content);
			resource.load(bais, saveOptions);
			PublicTransportDataValue value = (PublicTransportDataValue) resource.getContents().get(0);
			handlePublicTransportDataValue(value);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			resourceSet.getResources().remove(resource);
			resource.getContents().clear();
		}
	}
	
	private void handlePublicTransportDataValue(PublicTransportDataValue value) {

		Map<String,Pool<ModelTransformator>> poolMap = poolComponent.getPoolMap();
		Pool<ModelTransformator> pool = poolMap.get("simulatorModelTransformatorService-ibisSimulatorPool");
		if(pool != null) {
			ModelTransformator transformator = pool.poll();
			try {
				IbisDevice push = (IbisDevice) transformator.startTransformation(value);				
				
				IbisAdmin ibisAdmin = IbisSensinactFactory.eINSTANCE.createIbisAdmin();
				ibisAdmin.setDeviceType("SIMULATED-TRAM");
				push.setIbisAdmin(ibisAdmin);
				
				sensinact.pushUpdate(push);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			finally {
				pool.release(transformator);
			}
		}
	}
	
	@Deactivate
	public void deactivate() {
		subscription.close();
	}
}
