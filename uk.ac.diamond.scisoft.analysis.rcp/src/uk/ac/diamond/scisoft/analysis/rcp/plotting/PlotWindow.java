/*-
 * Copyright 2012 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.diamond.scisoft.analysis.rcp.plotting;

import gda.observable.IObserver;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.common.ui.util.DisplayUtils;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.axis.IAxis;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.tool.IToolPageSystem;
import org.dawnsci.plotting.api.trace.ColorOption;
import org.dawnsci.plotting.jreality.core.AxisMode;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.plotserver.AxisOperation;
import uk.ac.diamond.scisoft.analysis.plotserver.DataBean;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiBean;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiParameters;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiPlotMode;
import uk.ac.diamond.scisoft.analysis.rcp.AnalysisRCPActivator;
import uk.ac.diamond.scisoft.analysis.rcp.histogram.HistogramDataUpdate;
import uk.ac.diamond.scisoft.analysis.rcp.histogram.HistogramUpdate;
import uk.ac.diamond.scisoft.analysis.rcp.preference.PreferenceConstants;
import uk.ac.diamond.scisoft.analysis.rcp.views.DataWindowView;
import uk.ac.diamond.scisoft.analysis.rcp.views.HistogramView;

/**
 * Actual PlotWindow that can be used inside a View- or EditorPart
 */
@SuppressWarnings("deprecation")
public class PlotWindow extends AbstractPlotWindow {
	public static final String RPC_SERVICE_NAME = "PlotWindowManager";
	public static final String RMI_SERVICE_NAME = "RMIPlotWindowManager";

	static private Logger logger = LoggerFactory.getLogger(PlotWindow.class);

	private DataSetPlotter mainPlotter;

	private IPlottingSystem plottingSystem;

	private Composite plotSystemComposite;
	private Composite mainPlotterComposite;

	/**
	 * Obtain the IPlotWindowManager for the running Eclipse.
	 * 
	 * @return singleton instance of IPlotWindowManager
	 */
	public static IPlotWindowManager getManager() {
		// get the private manager for use only within the framework and
		// "upcast" it to IPlotWindowManager
		return PlotWindowManager.getPrivateManager();
	}

	public PlotWindow(Composite parent, GuiPlotMode plotMode, IActionBars bars, IWorkbenchPage page, String name) {
		this(parent, plotMode, null, null, bars, page, name);
	}

	public PlotWindow(final Composite parent, GuiPlotMode plotMode, IGuiInfoManager manager,
			IUpdateNotificationListener notifyListener, IActionBars bars, IWorkbenchPage page, String name) {
		super(parent, manager, notifyListener, bars, page, name);

		if (plotMode == null)
			plotMode = GuiPlotMode.ONED;

		// this needs to be started in 1D as later mode changes will not work as plot UIs are not setup
		if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM)
			createDatasetPlotter(PlottingMode.ONED);

		if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_ABSTRACT_PLOTTING_SYSTEM) {
			createPlottingSystem();
			cleanUpDatasetPlotter();
		}
		// Setting up
		if (plotMode.equals(GuiPlotMode.ONED)) {
			if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM)
				setup1D();
			else
				setupPlotting1D();
		} else if (plotMode.equals(GuiPlotMode.ONED_THREED)) {
			setupMulti1DPlot();
		} else if (plotMode.equals(GuiPlotMode.TWOD)) {
			if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM)
				setup2D();
			else
				setupPlotting2D();
		} else if (plotMode.equals(GuiPlotMode.SURF2D)) {
			if(getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM)
				setup2DSurfaceOldPlotting();
			else
				setup2DSurfaceNewPlotting();
		} else if (plotMode.equals(GuiPlotMode.SCATTER2D)) {
			if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM)
				setupScatter2DPlot();
			else
				setupScatterPlotting2D();
		} else if (plotMode.equals(GuiPlotMode.SCATTER3D)) {
			setupScatter3DPlot();
		} else if (plotMode.equals(GuiPlotMode.MULTI2D)) {
			setupMulti2D();
		}

		parentAddControlListener();

		PlotWindowManager.getPrivateManager().registerPlotWindow(this);
	}

	private void parentAddControlListener() {
		// for some reason, this window does not get repainted
		// when a perspective is switched and the view is resized
		parentComp.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				if (e.widget.equals(parentComp)) {
					parentComp.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (mainPlotter != null && !mainPlotter.isDisposed())
								mainPlotter.refresh(false);
						}
					});
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}

	private void createDatasetPlotter(PlottingMode mode) {
		mainPlotterComposite = new Composite(parentComp, SWT.NONE);
		mainPlotterComposite.setLayout(new FillLayout());
		mainPlotter = new DataSetPlotter(mode, mainPlotterComposite, true);
		mainPlotter.setAxisModes(AxisMode.LINEAR, AxisMode.LINEAR, AxisMode.LINEAR);
		mainPlotter.setXAxisLabel("X-Axis");
		mainPlotter.setYAxisLabel("Y-Axis");
		mainPlotter.setZAxisLabel("Z-Axis");

	}

	private void createPlottingSystem() {
		plotSystemComposite = new Composite(parentComp, SWT.NONE);
		plotSystemComposite.setLayout(new FillLayout());

		try {
			plottingSystem = PlottingFactory.createPlottingSystem();
			plottingSystem.setColorOption(ColorOption.NONE);

			plottingSystem.createPlotPart(plotSystemComposite, getName(), bars, PlotType.XY, (IViewPart) getGuiManager());
			plottingSystem.repaint();

			plottingSystem.addRegionListener(getRoiManager());

		} catch (Exception e) {
			logger.error("Cannot locate any Abstract plotting System!", e);
		}
	}

	/**
	 * @return plot UI
	 */
	public IPlotUI getPlotUI() {
		return plotUI;
	}

	/**
	 * Process a plot with data packed in bean - remember to update plot mode first if you do not know the current mode
	 * or if it is to change
	 * 
	 * @param dbPlot
	 */
	@Override
	public void processPlotUpdate(DataBean dbPlot) {
		// check to see what type of plot this is and set the plotMode to the correct one
		if (dbPlot.getGuiPlotMode() != null) {
			if (parentComp.isDisposed()) {
				// this can be caused by the same plot view shown on 2 difference perspectives.
				throw new IllegalStateException("parentComp is already disposed");
			}

			updatePlotMode(dbPlot.getGuiPlotMode(), true);
		}
		// there may be some gui information in the databean, if so this also needs to be updated
		if (dbPlot.getGuiParameters() != null) {
			processGUIUpdate(dbPlot.getGuiParameters());
		}
		try {
			doBlock();
			// Now plot the data as standard
			plotUI.processPlotUpdate(dbPlot, isUpdatePlot());
			setDataBean(dbPlot);
		} finally {
			undoBlock();
		}
	}

	private void cleanUpFromOldMode(final boolean leaveSidePlotOpen) {
		setUpdatePlot(false);
		mainPlotter.unregisterUI(plotUI);
		if (plotUI != null) {
			plotUI.deleteIObservers();
			plotUI.deactivate(leaveSidePlotOpen);
			removePreviousActions();
		}
	}

	/**
	 * Cleaning up the plot view according to the current plot mode
	 * 
	 * @param mode
	 */
	private void cleanUp(GuiPlotMode mode) {
		if (mode.equals(GuiPlotMode.ONED) || mode.equals(GuiPlotMode.TWOD) || mode.equals(GuiPlotMode.SCATTER2D)
				|| mode.equals(GuiPlotMode.SURF2D)) {
			cleanUpDatasetPlotter();
			if (plottingSystem == null || plottingSystem.isDisposed())
				createPlottingSystem();
		} else if (mode.equals(GuiPlotMode.ONED_THREED)) {
			cleanUpPlottingSystem();
			if (mainPlotter == null || mainPlotter.isDisposed())
				createDatasetPlotter(PlottingMode.ONED_THREED);
			cleanUpFromOldMode(true);
		} else if (mode.equals(GuiPlotMode.SCATTER3D)) {
			cleanUpPlottingSystem();
			if (mainPlotter == null || mainPlotter.isDisposed())
				createDatasetPlotter(PlottingMode.SCATTER3D);
			cleanUpFromOldMode(true);
		} else if (mode.equals(GuiPlotMode.MULTI2D)) {
			cleanUpPlottingSystem();
			if (mainPlotter == null || mainPlotter.isDisposed())
				createDatasetPlotter(PlottingMode.MULTI2D);
			cleanUpFromOldMode(true);
		}
		parentComp.layout();
	}

	/**
	 * Cleaning of the DatasetPlotter and its composite before the setting up of a Plotting System
	 */
	private void cleanUpDatasetPlotter() {
		if (mainPlotter != null && !mainPlotter.isDisposed()) {
			bars.getToolBarManager().removeAll();
			bars.getMenuManager().removeAll();
			mainPlotter.cleanUp();
			mainPlotterComposite.dispose();

			if(getPreviousMode()==GuiPlotMode.SURF2D){
				EclipseUtils.closeView(DataWindowView.ID);
			}
		}
	}

	/**
	 * Cleaning of the plotting system and its composite before the setting up of a datasetPlotter
	 */
	private void cleanUpPlottingSystem() {
		if (!plottingSystem.isDisposed()) {
			bars.getToolBarManager().removeAll();
			bars.getMenuManager().removeAll();
			for (Iterator<IRegion> iterator = plottingSystem.getRegions().iterator(); iterator.hasNext();) {
				IRegion region = iterator.next();
				plottingSystem.removeRegion(region);
			}
			plottingSystem.removeRegionListener(getRoiManager());
			plottingSystem.dispose();
			plotSystemComposite.dispose();
		}
	}

	// Datasetplotter
	private void setup1D() {
		mainPlotter.setMode(PlottingMode.ONED);
		plotUI = new Plot1DUIComplete(this, getGuiManager(), bars, parentComp, getPage(), getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.ONED);
	}

	// Abstract plotting System
	private void setupPlotting1D() {
		plottingSystem.setPlotType(PlotType.XY);
		plotUI = new Plotting1DUI(plottingSystem);
		addScriptingAction();
		addDuplicateAction();
		addClearAction();
		updateGuiBeanPlotMode(GuiPlotMode.ONED);
	}

	// Dataset plotter
	private void setup2D() {
		mainPlotter.setMode(PlottingMode.TWOD);
		plotUI = new Plot2DUI(this, mainPlotter, getGuiManager(), parentComp, getPage(), bars, getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.TWOD);
	}

	// Abstract plotting System
	private void setupPlotting2D() {
		plottingSystem.setPlotType(PlotType.IMAGE);
		plotUI = new Plotting2DUI(getRoiManager(), plottingSystem);
		addScriptingAction();
		addDuplicateAction();
		addClearAction();
		updateGuiBeanPlotMode(GuiPlotMode.TWOD);
	}

	private void setupMulti2D() {
		mainPlotter.setMode(PlottingMode.MULTI2D);
		plotUI = new Plot2DMultiUI(this, mainPlotter, getGuiManager(), parentComp, getPage(), bars, getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.MULTI2D);
	}

	private void setup2DSurfaceNewPlotting() {
		plottingSystem.setPlotType(PlotType.SURFACE);
		plotUI = new Plotting2DUI(getRoiManager(), plottingSystem);
		addScriptingAction();
		addDuplicateAction();
		addClearAction();
		updateGuiBeanPlotMode(GuiPlotMode.SURF2D);
	}

	private void setup2DSurfaceOldPlotting() {
		mainPlotter.setMode(PlottingMode.SURF2D);
		plotUI = new PlotSurf3DUI(this, mainPlotter, parentComp, getPage(), bars, getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.SURF2D);
	}

	private void setupMulti1DPlot() {
		mainPlotter.setMode(PlottingMode.ONED_THREED);
		plotUI = new Plot1DStackUI(this, bars, mainPlotter, parentComp, getPage());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.ONED_THREED);
	}

	private void setupScatter2DPlot() {
		mainPlotter.setMode(PlottingMode.SCATTER2D);
		plotUI = new PlotScatter2DUI(this, bars, mainPlotter, parentComp, getPage(), getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.SCATTER2D);
	}

	// Abstract plotting System
	private void setupScatterPlotting2D() {
		plotUI = new PlottingScatter2DUI(plottingSystem);
		addScriptingAction();
		addDuplicateAction();
		addClearAction();
		updateGuiBeanPlotMode(GuiPlotMode.SCATTER2D);
	}

	private void setupScatter3DPlot() {
		mainPlotter.setMode(PlottingMode.SCATTER3D);
		plotUI = new PlotScatter3DUI(this, mainPlotter, parentComp, getPage(), bars, getName());
		addCommonActions(mainPlotter);
		bars.updateActionBars();
		updateGuiBeanPlotMode(GuiPlotMode.SCATTER3D);
	}

	/**
	 * @param plotMode
	 */
	@Override
	public void updatePlotMode(final GuiPlotMode plotMode, boolean async) {
		doBlock();
		DisplayUtils.runInDisplayThread(async, parentComp, new Runnable() {
			@Override
			public void run() {
				try {
					int choice = getDefaultPlottingSystemChoice();
					GuiPlotMode oldMode = getPreviousMode();
					if (oldMode == null || !plotMode.equals(oldMode)) {
						switch (choice) {
						case PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM:
							cleanUpFromOldMode(true);
							if (plotMode.equals(GuiPlotMode.ONED)) {
								setup1D();
							} else if (plotMode.equals(GuiPlotMode.ONED_THREED)) {
								setupMulti1DPlot();
							} else if (plotMode.equals(GuiPlotMode.TWOD)) {
								setup2D();
							} else if (plotMode.equals(GuiPlotMode.SURF2D)) {
								setup2DSurfaceOldPlotting();
							} else if (plotMode.equals(GuiPlotMode.SCATTER2D)) {
								setupScatter2DPlot();
							} else if (plotMode.equals(GuiPlotMode.SCATTER3D)) {
								setupScatter3DPlot();
							} else if (plotMode.equals(GuiPlotMode.MULTI2D)) {
								setupMulti2D();
							} else if (plotMode.equals(GuiPlotMode.EMPTY)) {
								clearPlot();
							}
							break;
						case PreferenceConstants.PLOT_VIEW_ABSTRACT_PLOTTING_SYSTEM:
							cleanUp(plotMode);
							if (plotMode.equals(GuiPlotMode.ONED)) {
								setupPlotting1D();
							} else if (plotMode.equals(GuiPlotMode.TWOD)) {
								setupPlotting2D();
							} else if (plotMode.equals(GuiPlotMode.SCATTER2D)) {
								setupScatterPlotting2D();
							} else if (plotMode.equals(GuiPlotMode.ONED_THREED)) {
								setupMulti1DPlot();
							} else if (plotMode.equals(GuiPlotMode.SURF2D)) {
								setup2DSurfaceNewPlotting();
							} else if (plotMode.equals(GuiPlotMode.SCATTER3D)) {
								setupScatter3DPlot();
							} else if (plotMode.equals(GuiPlotMode.MULTI2D)) {
								setupMulti2D();
							} else if (plotMode.equals(GuiPlotMode.EMPTY)) {
								clearPlot();
							}
							break;
						}
						setPreviousMode(plotMode);
					}
				} finally {
					undoBlock();
				}
			}
		});
	}

	@Override
	public void clearPlot() {
		if (mainPlotter != null && !mainPlotter.isDisposed()) {
			mainPlotter.emptyPlot();
			mainPlotter.refresh(true);
		}
		if (plottingSystem != null) {
			plottingSystem.clearRegions();
			plottingSystem.reset();
			plottingSystem.repaint();
		}
	}

	@Override
	public void processGUIUpdate(GuiBean bean) {
		setUpdatePlot(false);
		if (bean.containsKey(GuiParameters.PLOTMODE)) {
			updatePlotMode(bean, true);
		}

		if (bean.containsKey(GuiParameters.AXIS_OPERATION)) {
			AxisOperation operation = (AxisOperation) bean.get(GuiParameters.AXIS_OPERATION);
            processAxisOperation(operation);
		}

		if (bean.containsKey(GuiParameters.TITLE) && mainPlotter != null 
				&& mainPlotterComposite != null && !mainPlotterComposite.isDisposed()) {
			final String titleStr = (String) bean.get(GuiParameters.TITLE);
			parentComp.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					doBlock();
					try {
						mainPlotter.setTitle(titleStr);
					} finally {
						undoBlock();
					}
					mainPlotter.refresh(true);
				}
			});
		}

		if (bean.containsKey(GuiParameters.PLOTOPERATION)) {
			String opStr = (String) bean.get(GuiParameters.PLOTOPERATION);
			if (opStr.equals(GuiParameters.PLOTOP_UPDATE)) {
				setUpdatePlot(true);
			}
		}

		if (bean.containsKey(GuiParameters.ROIDATA) || bean.containsKey(GuiParameters.ROIDATALIST)) {
			plotUI.processGUIUpdate(bean);
		}
	}

	// this map is needed as axes from the plotting system get their titles changed
	private Map<String, IAxis> axes = new LinkedHashMap<String, IAxis>();
	private void processAxisOperation(final AxisOperation operation) {
		doBlock();
		parentComp.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					final List<IAxis> pAxes = plottingSystem.getAxes();
					if (axes.size() != 0 && axes.size() != pAxes.size()) {
						logger.warn("Axes are out of synch!");
						axes.clear();
					}
					if (axes.size() == 0) {
						for (IAxis i : pAxes) {
							String t = i.getTitle();
							if (i.isPrimaryAxis()) {
								if (t == null || t.length() == 0) {
									t = i.isYAxis() ? "Y-Axis" : "X-Axis";
								}
							}
							axes.put(t, i);
						}
					}

					String title = operation.getTitle();
					String type = operation.getOperationType();
					IAxis a = axes.get(title);
					if (type.equals(AxisOperation.CREATE)) {
						boolean isYAxis = operation.isYAxis();
						if (a != null) {
							if (isYAxis == a.isYAxis()) {
								logger.warn("Axis already exists");
								return;
							}
							logger.debug("Axis is opposite orientation already exists");
						}
						a = plottingSystem.createAxis(title, isYAxis, operation.getSide());
						axes.put(title, a);
						return;
					} else if (type.equals(AxisOperation.DELETE)) {
						if (a != null) {
							plottingSystem.removeAxis(a);
							axes.remove(title);
							return;
						}
						logger.warn("Could not find axis of given name");
					} else if (type.equals(AxisOperation.ACTIVEX)) {
						if (a != null && !a.isYAxis()) {
							plottingSystem.setSelectedXAxis(a);
							return;
						}
						// default to first x axis
						for (IAxis i: axes.values()) {
							if (!i.isYAxis()) {
								plottingSystem.setSelectedXAxis(i);
								return;
							}
						}
						logger.warn("Could not select axis of given name");
					} else if (type.equals(AxisOperation.ACTIVEY)) {
						if (a != null && a.isYAxis()) {
							plottingSystem.setSelectedYAxis(a);
							return;
						}
						// default to first y axis
						for (IAxis i: axes.values()) {
							if (i.isYAxis()) {
								plottingSystem.setSelectedYAxis(i);
								return;
							}
						}
						logger.warn("Could not select axis of given name");
					}
				} finally {
					undoBlock();
				}
			}
		});
	}

	public void notifyHistogramChange(HistogramDataUpdate histoUpdate) {
		if (getDefaultPlottingSystemChoice() == PreferenceConstants.PLOT_VIEW_DATASETPLOTTER_PLOTTING_SYSTEM) {
			Iterator<IObserver> iter = getObservers().iterator();
			while (iter.hasNext()) {
				IObserver listener = iter.next();
				listener.update(this, histoUpdate);
			}
		}
	}

	@Override
	public void update(Object theObserved, Object changeCode) {
		if (theObserved instanceof HistogramView) {
			HistogramUpdate update = (HistogramUpdate) changeCode;
			mainPlotter.applyColourCast(update);

			if (!mainPlotter.isDisposed())
				mainPlotter.refresh(false);
			if (plotUI instanceof Plot2DUI) {
				Plot2DUI plot2Dui = (Plot2DUI) plotUI;
				plot2Dui.getSidePlotView().sendHistogramUpdate(update);
			}
		}
	}

	public DataSetPlotter getMainPlotter() {
		return mainPlotter;
	}

	public IPlottingSystem getPlottingSystem() {
		return plottingSystem;
	}

	/**
	 * Required if you want to make tools work with Abstract Plotting System.
	 */
	@Override
	public Object getAdapter(final Class<?> clazz) {
		if (clazz == IToolPageSystem.class) {
			return plottingSystem;
		}
		return null;
	}

	public void dispose() {
		PlotWindowManager.getPrivateManager().unregisterPlotWindow(this);
		if (plotUI != null) {
			plotUI.deactivate(false);
			plotUI.dispose();
		}
		try {
			if (mainPlotter != null) {
				mainPlotter.cleanUp();
			}
			if (plottingSystem != null){//&& !plottingSystem.isDisposed()) {
				plottingSystem.removeRegionListener(getRoiManager());
				plottingSystem.dispose();
			}
		} catch (Exception ne) {
			logger.debug("Cannot clean up plotter!", ne);
		}
		deleteIObservers();
		mainPlotter = null;
		plotUI = null;
		System.gc();
	}

	private int getDefaultPlottingSystemChoice() {
		IPreferenceStore preferenceStore = AnalysisRCPActivator.getDefault().getPreferenceStore();
		return preferenceStore.isDefault(PreferenceConstants.PLOT_VIEW_PLOTTING_SYSTEM) ? preferenceStore
				.getDefaultInt(PreferenceConstants.PLOT_VIEW_PLOTTING_SYSTEM) : preferenceStore
				.getInt(PreferenceConstants.PLOT_VIEW_PLOTTING_SYSTEM);
	}
}