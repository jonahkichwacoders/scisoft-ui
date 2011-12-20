/*
 * Copyright © 2011 Diamond Light Source Ltd.
 * Contact :  ScientificSoftware@diamond.ac.uk
 * 
 * This is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 * 
 * This software is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.diamond.scisoft.analysis.rcp.plotting;

import gda.observable.IObserver;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractCompoundDataset;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.plotserver.AxisMapBean;
import uk.ac.diamond.scisoft.analysis.plotserver.DataBean;
import uk.ac.diamond.scisoft.analysis.plotserver.DataSetWithAxisInformation;
import uk.ac.diamond.scisoft.analysis.rcp.AnalysisRCPActivator;
import uk.ac.diamond.scisoft.analysis.rcp.histogram.HistogramDataUpdate;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.enums.AxisMode;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.enums.ScaleType;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.enums.SurfPlotStyles;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.enums.TickFormatting;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.roi.SurfacePlotROI;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.utils.PlotExportUtil;
import uk.ac.diamond.scisoft.analysis.rcp.util.ResourceProperties;
import uk.ac.diamond.scisoft.analysis.rcp.views.DataWindowView;
import uk.ac.diamond.scisoft.analysis.rcp.views.HistogramView;

/**
 * A very general UI for 2D surface plotting using SWT / Eclipse RCP
 */
public class PlotSurf3DUI extends AbstractPlotUI implements IObserver {


	private AxisValues xAxis = null;
	private AxisValues yAxis = null;
	private AxisValues zAxis = null;
	private PlotWindow plotWindow = null;
	private DataSetPlotter mainPlotter;
	private Composite parent;
	private HistogramView histogramView;
	private DataWindowView dataWindowView;
	private List<IObserver> observers = 
		Collections.synchronizedList(new LinkedList<IObserver>());
	private Action boundingBox;
	private Action xCoordGrid;
	private Action yCoordGrid;
	private Action zCoordGrid;
	private Action saveGraph;
	private Action copyGraph;
	private Action printGraph;
	private Action displayFilled;
	private Action displayWireframe;
	private Action displayLinegraph;
	private Action displayPoint;
	private Action resetView;
	private Action zAxisScaleLinear;
	private Action zAxisScaleLog;

	private String[] listPrintScaleText = { ResourceProperties.getResourceString("PRINT_LISTSCALE_0"),
		ResourceProperties.getResourceString("PRINT_LISTSCALE_1"), ResourceProperties.getResourceString("PRINT_LISTSCALE_2"),
		ResourceProperties.getResourceString("PRINT_LISTSCALE_3"), ResourceProperties.getResourceString("PRINT_LISTSCALE_4"),
		ResourceProperties.getResourceString("PRINT_LISTSCALE_5"), ResourceProperties.getResourceString("PRINT_LISTSCALE_6") };
	private String printButtonText = ResourceProperties.getResourceString("PRINT_BUTTON");
	private String printToolTipText = ResourceProperties.getResourceString("PRINT_TOOLTIP");
	private String printImagePath = ResourceProperties.getResourceString("PRINT_IMAGE_PATH");
	private String copyButtonText = ResourceProperties.getResourceString("COPY_BUTTON");
	private String copyToolTipText = ResourceProperties.getResourceString("COPY_TOOLTIP");
	private String copyImagePath = ResourceProperties.getResourceString("COPY_IMAGE_PATH");
	private String saveButtonText = ResourceProperties.getResourceString("SAVE_BUTTON");
	private String saveToolTipText = ResourceProperties.getResourceString("SAVE_TOOLTIP");
	private String saveImagePath = ResourceProperties.getResourceString("SAVE_IMAGE_PATH");
	
	private static final Logger logger = LoggerFactory
	.getLogger(PlotSurf3DUI.class);
	
	/**
	 * @param window 
	 * @param plotter
	 * @param parent
	 * @param page 
	 * @param id 
	 */
	public PlotSurf3DUI(PlotWindow window, 
			 final DataSetPlotter plotter,
			 Composite parent, 
			 IWorkbenchPage page, 
			 IActionBars bars,
			 String id) {

		this.parent = parent;
		this.plotWindow = window;
		xAxis = new AxisValues();
		yAxis = new AxisValues();
		zAxis = new AxisValues();
		this.mainPlotter = plotter;
		buildMenuActions(bars.getMenuManager(), plotter); 
		buildToolActions(bars.getToolBarManager(), 
				         plotter, parent.getShell());								 

		try {
			 histogramView = (HistogramView) page.showView("uk.ac.diamond.scisoft.analysis.rcp.views.HistogramView",
					id, IWorkbenchPage.VIEW_CREATE);
			plotWindow.addIObserver(histogramView);
			histogramView.addIObserver(plotWindow);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		try {
			dataWindowView = (DataWindowView) page.showView("uk.ac.diamond.scisoft.analysis.rcp.views.DataWindowView",
					id, IWorkbenchPage.VIEW_CREATE);
			if (histogramView != null)
				histogramView.addIObserver(dataWindowView);
			dataWindowView.addIObserver(this);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		if (dataWindowView == null) {
			logger.warn("Cannot find data window");
		}	
	}	

	
	private void buildToolActions(IToolBarManager manager, final DataSetPlotter plotter,
			  final Shell shell)
	{
		resetView = new Action() {
			@Override
			public void run()
			{
				mainPlotter.resetView();
				mainPlotter.refresh(false);
			}
		};
		resetView.setText("Reset view");
		resetView.setToolTipText("Reset panning and zooming");
		resetView.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor("icons/house_go.png"));
		
		boundingBox = new Action("",IAction.AS_CHECK_BOX) {
			@Override
			public void run()
			{
				plotter.enableBoundingBox(boundingBox.isChecked());
				plotter.refresh(false);
			}
		};
		boundingBox.setText("Bounding box on/off");
		boundingBox.setToolTipText("Bounding box on/off");
		boundingBox.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor("icons/box.png"));		
		boundingBox.setChecked(true);
		xCoordGrid = new Action("",IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				plotter.setTickGridLines(xCoordGrid.isChecked(), 
										 yCoordGrid.isChecked(),zCoordGrid.isChecked());
				plotter.refresh(false);
			}
		};
		xCoordGrid.setChecked(true);
		xCoordGrid.setText("X grid lines ON/OFF");
		xCoordGrid.setToolTipText("Toggle x axis grid lines on/off");
		xCoordGrid.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor("icons/text_align_justify_rot.png"));
		yCoordGrid = new Action("",IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				plotter.setTickGridLines(xCoordGrid.isChecked(), 
										 yCoordGrid.isChecked(),zCoordGrid.isChecked());
				plotter.refresh(false);
				
			}
		};
		yCoordGrid.setChecked(true);
		yCoordGrid.setText("Y grid lines ON/OFF");
		yCoordGrid.setToolTipText("Toggle y axis grid lines on/off");
		yCoordGrid.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor("icons/text_align_justify.png"));		

		zCoordGrid = new Action("",IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				plotter.setTickGridLines(xCoordGrid.isChecked(), 
										 yCoordGrid.isChecked(),zCoordGrid.isChecked());
				plotter.refresh(false);
				
			}
		};
		zCoordGrid.setChecked(true);
		zCoordGrid.setText("Z grid lines ON/OFF");
		zCoordGrid.setToolTipText("Toggle z axis grid lines on/off");
		zCoordGrid.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor("icons/text_align_justify.png"));
		
		saveGraph = new Action() {
			
			// Cache file name otherwise they have to keep
			// choosing the folder.
			private String filename;
			
			@Override
			public void run() {
				
				FileDialog dialog = new FileDialog (shell, SWT.SAVE);
				
				String [] filterExtensions = new String [] {"*.jpg;*.JPG;*.jpeg;*.JPEG;*.png;*.PNG", "*.ps;*.eps","*.svg;*.SVG"};
				if (filename!=null) {
					dialog.setFilterPath((new File(filename)).getParent());
				} else {
					String filterPath = "/";
					String platform = SWT.getPlatform();
					if (platform.equals("win32") || platform.equals("wpf")) {
						filterPath = "c:\\";
					}
					dialog.setFilterPath (filterPath);
				}
				dialog.setFilterNames (PlotExportUtil.FILE_TYPES);
				dialog.setFilterExtensions (filterExtensions);
				filename = dialog.open();
				if (filename == null)
					return;

				plotter.saveGraph(filename, PlotExportUtil.FILE_TYPES[dialog.getFilterIndex()]);
			}
		};
		saveGraph.setText(saveButtonText);
		saveGraph.setToolTipText(saveToolTipText);
		saveGraph.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor(saveImagePath));

		copyGraph = new Action() {
			@Override
			public void run() {
				plotter.copyGraph();
			}
		};
		copyGraph.setText(copyButtonText);
		copyGraph.setToolTipText(copyToolTipText);
		copyGraph.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor(copyImagePath));

		printGraph = new Action() {
			@Override
			public void run() {
//				PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//				PrinterData printerData = dialog.open();
				plotter.printGraph();
			}
		};
		printGraph.setText(printButtonText);
		printGraph.setToolTipText(printToolTipText);
		printGraph.setImageDescriptor(AnalysisRCPActivator.getImageDescriptor(printImagePath));
//		printGraph.setMenuCreator(new IMenuCreator() {
//			@Override
//			public Menu getMenu(final Control parent) {
//				Menu menu = new Menu(parent);
//				MenuItem item10 = new MenuItem(menu, SWT.None);
//				item10.setText(listPrintScaleText[0]);
//				item10.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.1f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item25 = new MenuItem(menu, SWT.None);
//				item25.setText(listPrintScaleText[1]);
//				item25.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.25f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item33 = new MenuItem(menu, SWT.None);
//				item33.setText(listPrintScaleText[2]);
//				item33.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.33f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item50 = new MenuItem(menu, SWT.None);
//				item50.setText(listPrintScaleText[3]);
//				item50.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.5f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item66 = new MenuItem(menu, SWT.None);
//				item66.setText(listPrintScaleText[4]);
//				item66.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.66f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item75 = new MenuItem(menu, SWT.None);
//				item75.setText(listPrintScaleText[5]);
//				item75.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 0.75f);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				MenuItem item100 = new MenuItem(menu, SWT.None);
//				item100.setText(listPrintScaleText[6]);
//				item100.addSelectionListener(new SelectionListener() {
//					@Override
//					public void widgetSelected(SelectionEvent e) {
//						PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
//						PrinterData printerData = dialog.open();
//						plotter.printGraph(printerData, 1);
//					}
//					@Override
//					public void widgetDefaultSelected(SelectionEvent e) {}
//				});
//				return menu;
//			}
//			@Override
//			public Menu getMenu(Menu parent) {
//				return null;
//			}
//			@Override
//			public void dispose() {}
//		});
		manager.add(resetView);
		manager.add(boundingBox);
		manager.add(xCoordGrid);
		manager.add(yCoordGrid);
		manager.add(zCoordGrid);
		manager.add(new Separator(getClass().getName()+printButtonText));
		manager.add(saveGraph);
		manager.add(copyGraph);
		manager.add(printGraph);
		
		// Needed when toolbar is attached to an editor
		// or else the bar looks empty.
		manager.update(true);

	}
	
	private void buildMenuActions(IMenuManager manager, final DataSetPlotter plotter)
	{
		displayFilled = new Action() {
			@Override
			public void run() {
				plotter.setPlot2DSurfStyle(SurfPlotStyles.FILLED);
				plotter.refresh(false);
			}
		};
		displayFilled.setText("Filled mode");
		displayFilled.setDescription("Render the graph in filled mode");
		displayWireframe = new Action() {
			@Override
			public void run() {
				plotter.setPlot2DSurfStyle(SurfPlotStyles.WIREFRAME);
				plotter.refresh(false);
			}
		};
		displayWireframe.setText("Wireframe mode");
		displayWireframe.setDescription("Render the graph in wireframe mode");
		displayLinegraph = new Action() {
			@Override
			public void run() {
				plotter.setPlot2DSurfStyle(SurfPlotStyles.LINEGRAPH);
				plotter.refresh(true);
			}
		};
		displayLinegraph.setText("Linegraph mode");
		displayLinegraph.setDescription("Render the graph in linegraph mode");
		displayPoint = new Action() {
			@Override
			public void run() {
				plotter.setPlot2DSurfStyle(SurfPlotStyles.POINTS);
				plotter.refresh(false);	
			}
		};
		displayPoint.setText("Point mode");
		displayPoint.setDescription("Render the graph in dot mode");
		zAxisScaleLinear = new Action()
		{
			@Override
			public void run()
			{
				plotter.setYAxisScaling(ScaleType.LINEAR);
			}
		};
		zAxisScaleLinear.setText("Z-Axis scale linear");
		zAxisScaleLinear.setToolTipText("Change the Z-Axis scaling to be linear");
		
		zAxisScaleLog = new Action()
		{
			@Override
			public void run()
			{
				plotter.setYAxisScaling(ScaleType.LN);
			}
		};
		zAxisScaleLog.setText("Z-Axis scale logarithmic");
		zAxisScaleLog.setToolTipText("Change the Z-Axis scaling to be logarithmic (natural)");
		MenuManager zAxis = new MenuManager("Z-Axis");
		zAxis.add(zAxisScaleLinear);
		zAxis.add(zAxisScaleLog);			
		manager.add(zAxis);
		manager.add(displayFilled);
		manager.add(displayWireframe);
		manager.add(displayLinegraph);
		manager.add(displayPoint);
	}

	@Override
	public void processPlotUpdate(DataBean dbPlot, boolean isUpdate) {
		Collection<DataSetWithAxisInformation> plotData = dbPlot.getData();
		if (plotData != null) {
			Iterator<DataSetWithAxisInformation> iter = plotData.iterator();
			final List<AbstractDataset> datasets = Collections.synchronizedList(new LinkedList<AbstractDataset>());
	
			AbstractDataset xAxisValues = dbPlot.getAxis(AxisMapBean.XAXIS);
			AbstractDataset yAxisValues = dbPlot.getAxis(AxisMapBean.YAXIS);
			AbstractDataset zAxisValues = dbPlot.getAxis(AxisMapBean.ZAXIS);
			xAxis.clear();
			yAxis.clear();
			zAxis.clear();
			mainPlotter.setAxisModes((xAxisValues == null ? AxisMode.LINEAR : AxisMode.CUSTOM),
					                 (yAxisValues == null ? AxisMode.LINEAR : AxisMode.CUSTOM),
					                 (zAxisValues == null ? AxisMode.LINEAR : AxisMode.CUSTOM));
			if (xAxisValues != null) {
				if (xAxisValues.getName() != null && xAxisValues.getName().length() > 0)
					mainPlotter.setXAxisLabel(xAxisValues.getName());
				else
					mainPlotter.setXAxisLabel("X-Axis");
				xAxis.setValues(xAxisValues);
				mainPlotter.setXAxisValues(xAxis, 1);
			} else
				mainPlotter.setXAxisLabel("X-Axis");
			if (yAxisValues != null) {
				if (yAxisValues.getName() != null && yAxisValues.getName().length() > 0)
					mainPlotter.setYAxisLabel(yAxisValues.getName());
				else
					mainPlotter.setYAxisLabel("Y-Axis");
				yAxis.setValues(yAxisValues);
				mainPlotter.setYAxisValues(yAxis);
			} else
				mainPlotter.setYAxisLabel("Y-Axis");
			if (zAxisValues != null) {
				if (zAxisValues.getName() != null && zAxisValues.getName().length() > 0)
					mainPlotter.setZAxisLabel(zAxisValues.getName());
				else
					mainPlotter.setZAxisLabel("Z-Axis");
				zAxis.setValues(zAxisValues);
				mainPlotter.setZAxisValues(zAxis);
			} else
				mainPlotter.setZAxisLabel("Z-Axis");
			mainPlotter.setYTickLabelFormat(TickFormatting.roundAndChopMode);
			mainPlotter.setXTickLabelFormat(TickFormatting.roundAndChopMode);
			while (iter.hasNext()) {
				DataSetWithAxisInformation dataSetAxis = iter.next();
				AbstractDataset data = dataSetAxis.getData();
				datasets.add(data);
			}
			if (datasets.get(0) instanceof AbstractCompoundDataset) {
				logger.warn("Surface plotting of CompoundDatasets is currently not supported!");
				plotWindow.notifyUpdateFinished();				
			} else {
				final HistogramDataUpdate histoUpdate = new
				  HistogramDataUpdate(datasets.get(0));
				try {
					mainPlotter.replaceAllPlots(datasets);
				} catch (PlotException e) {
					e.printStackTrace();
				}
				dataWindowView.setData(datasets.get(0),xAxis,yAxis);
				parent.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						mainPlotter.refresh(true);
						plotWindow.notifyHistogramChange(histoUpdate);
						plotWindow.notifyUpdateFinished();
					}
				});
			}
		}

	}

	/**
	 * 
	 */
	@Override
	public void deactivate(boolean leaveSidePlotOpen) {
		histogramView.deleteIObserver(dataWindowView);
		IWorkbenchPage aPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (aPage != null)
			aPage.hideView(dataWindowView);
		plotWindow.deleteIObserver(histogramView);
	}
	


	@Override
	public void addIObserver(IObserver anIObserver) {
		observers.add(anIObserver);
	}


	@Override
	public void deleteIObserver(IObserver anIObserver) {
		observers.remove(anIObserver);
	}


	@Override
	public void deleteIObservers() {
		observers.clear();
	}

	@Override
	public void update(Object theObserved, Object changeCode) {
		if (changeCode instanceof SurfacePlotROI) {
			SurfacePlotROI roi = (SurfacePlotROI)changeCode;
			mainPlotter.setDataWindowPosition(roi);
			mainPlotter.refresh(false);			
		}
	}

}
