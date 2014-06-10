/******************************************************************************* 
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.DefaultDeleteBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDeleteContext;

public class AbstractDefaultDeleteFeature extends DefaultDeleteBPMNShapeFeature {
	public AbstractDefaultDeleteFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public void delete(IDeleteContext context) {
		RootElement container = null;
		BaseElement element = BusinessObjectUtil.getFirstBaseElement(context.getPictogramElement());
		if (element!=null && element.eContainer() instanceof RootElement) {
			container = (RootElement) element.eContainer();
		}
		deletePeEnvironment(context.getPictogramElement());
		super.delete(context);

		DIUtils.deleteContainerIfPossible(container);
	}

}