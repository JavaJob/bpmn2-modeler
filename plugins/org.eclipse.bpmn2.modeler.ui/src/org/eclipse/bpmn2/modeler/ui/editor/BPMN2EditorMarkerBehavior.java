package org.eclipse.bpmn2.modeler.ui.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.utils.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.ui.MarkerHelper;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.ui.util.EditUIMarkerHelper;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.ui.editor.DefaultMarkerBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.swt.widgets.Display;

public class BPMN2EditorMarkerBehavior extends DefaultMarkerBehavior {

	/**
	 * The marker helper instance is responsible for creating workspace resource
	 * markers presented in Eclipse's Problems View.
	 */
	private MarkerHelper markerHelper = new EditUIMarkerHelper();

	/**
	 * Map to store the diagnostic associated with a resource.
	 */
	private Map<Resource, Diagnostic> resourceToDiagnosticMap = new LinkedHashMap<Resource, Diagnostic>();

	/**
	 * Controls whether the problem indication should be updated.
	 */
	private boolean updateProblemIndication = true;

	/**
	 * Creates a new instance of {@link DefaultMarkerBehavior} that is
	 * associated with the given {@link DiagramBehavior}.
	 * 
	 * @param diagramBehavior
	 *            the associated {@link DiagramBehavior}
	 * @since 0.10
	 */
	public BPMN2EditorMarkerBehavior(DiagramBehavior diagramBehavior) {
		super(diagramBehavior);
	}

	/**
	 * Initializes this marker behavior extension. The default implementation
	 * simply registers an adapter that updates the markers when EMF objects
	 * change.
	 */
	public void initialize() {
		diagramBehavior.getResourceSet().eAdapters().add(problemIndicationAdapter);
	}

	/**
	 * Returns the adapter that is installed for updating the markers.
	 * 
	 * @return the problemIndicationAdapter
	 */
	public EContentAdapter getProblemIndicationAdapter() {
		return problemIndicationAdapter;
	}

	/**
	 * Can be called to (temporarily) disable the marker update adapter, so that
	 * mass changes do not result in a bunch of notifications and cause
	 * performance penalties.
	 * 
	 * @see #enableProblemIndicationUpdate()
	 */
	public void disableProblemIndicationUpdate() {
		updateProblemIndication = false;
	}

	/**
	 * Can be called to enable the marker update adapter again after it has been
	 * disabled with {@link #disableProblemIndicationUpdate()}. The default
	 * implementation also triggers an update of the markers.
	 */
	public void enableProblemIndicationUpdate() {
		updateProblemIndication = true;
		updateProblemIndication();
	}

	/**
	 * Updates the problems indication markers in the editor. The default
	 * implementation used an EMF {@link BasicDiagnostic} to do the checks and
	 * {@link EditUIMarkerHelper} to check and set markers for {@link EObject}s.
	 */
	protected void updateProblemIndication() {
		if (diagramBehavior == null) {
			// Already disposed
			return;
		}
		TransactionalEditingDomain editingDomain = diagramBehavior.getEditingDomain();
		if (updateProblemIndication && editingDomain != null) {
			ResourceSet resourceSet = editingDomain.getResourceSet();
			final BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.OK, Activator.PLUGIN_ID, 0, null,
					new Object[] { resourceSet });
			for (final Diagnostic childDiagnostic : resourceToDiagnosticMap.values()) {
				if (childDiagnostic.getSeverity() != Diagnostic.OK) {
					diagnostic.add(childDiagnostic);
				}
			}
			if (markerHelper.hasMarkers(resourceSet)) {
				markerHelper.deleteMarkers(resourceSet);
			}
			if (diagnostic.getSeverity() != Diagnostic.OK) {
				try {
					markerHelper.createMarkers(diagnostic);
				} catch (final CoreException exception) {
				}
			}
		}
	}

	/**
	 * Returns a diagnostic describing the errors and warnings listed in the
	 * resource and the specified exception (if any).
	 * 
	 * @param resource
	 *            the resource to analyze
	 * @param exception
	 *            forwarded as data object to the {@link BasicDiagnostic}
	 * @return a new {@link Diagnostic} for the given resource
	 * 
	 */
	public Diagnostic analyzeResourceProblems(Resource resource, Exception exception) {
		if ((!resource.getErrors().isEmpty() || !resource.getWarnings().isEmpty()) && updateProblemIndication) {
			final IFile file = FileUtils.getFile(resource.getURI());
			final String fileName = file != null ? file.getFullPath().toString() : "unknown name"; //$NON-NLS-1$
			final BasicDiagnostic basicDiagnostic = new BasicDiagnostic(
					Diagnostic.ERROR,
					Activator.PLUGIN_ID,
					0,
					"Problems encountered in file " + fileName, new Object[] { exception == null ? (Object) resource : exception }); //$NON-NLS-1$
			basicDiagnostic.merge(EcoreUtil.computeDiagnostic(resource, true));
			return basicDiagnostic;
		} else if (exception != null) {
			return new BasicDiagnostic(Diagnostic.ERROR, Activator.PLUGIN_ID, 0, "Problems encountered in file", //$NON-NLS-1$ 
					new Object[] { exception });
		} else {
			return Diagnostic.OK_INSTANCE;
		}
	}

	/**
	 * Called to dispose this instance when the editor is closed. The default
	 * implementation simply disables the marker update adapter and removes it
	 * from the resource set and clears its member variables.
	 */
	public void dispose() {
		disableProblemIndicationUpdate();
		ResourceSet resourceSet = diagramBehavior.getResourceSet();
		// Check for null to prevent NPE, see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=429215
		if (resourceSet != null) {
			resourceSet.eAdapters().remove(problemIndicationAdapter);
		}

		problemIndicationAdapter = null;
		markerHelper = null;
		diagramBehavior = null;
		resourceToDiagnosticMap.clear();
		resourceToDiagnosticMap = null;
	}

	/**
	 * Adapter used to update the problem indication when resources are demanded
	 * loaded.
	 */
	private EContentAdapter problemIndicationAdapter = new EContentAdapter() {
		@Override
		public void notifyChanged(Notification notification) {
			if (notification.getNotifier() instanceof Resource) {
				switch (notification.getFeatureID(Resource.class)) {
					case Resource.RESOURCE__IS_LOADED: {
						final Resource resource = (Resource) notification.getNotifier();
						final Diagnostic diagnostic = analyzeResourceProblems(resource, null);
						if (diagnostic.getSeverity() != Diagnostic.OK) {
							resourceToDiagnosticMap.put(resource, diagnostic);
						} else {
							resourceToDiagnosticMap.remove(resource);
						}
	
						if (updateProblemIndication) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									updateProblemIndication();
								}
							});
						}
						break;
					}
				}
			} else {
				super.notifyChanged(notification);
			}
		}

		@Override
		protected void setTarget(Resource target) {
			basicSetTarget(target);
		}

		@Override
		protected void unsetTarget(Resource target) {
			basicUnsetTarget(target);
		}
	};
}
