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

package uk.ac.diamond.scisoft.analysis.rcp;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.rcp.editors.DataSetPlotEditor;
import uk.ac.diamond.scisoft.analysis.rcp.editors.NexusMultiPageEditor;
import uk.ac.diamond.scisoft.analysis.rcp.editors.NexusTreeEditor;
import uk.ac.gda.common.rcp.util.EclipseUtils;

public class NexusTreeEditorPluginTest {

	@Before
	public void before() {
		while (Job.getJobManager().currentJob() != null) EclipseUtils.delay(1000);
	}
		
	@Test
	public void testSelectingLooping1DDataSets() throws Exception {

	    final NexusMultiPageEditor editor = createNexusEditor("FeKedge_1_15.nxs");
		
		final DataSetPlotEditor dataEd = (DataSetPlotEditor)editor.getActiveEditor();
		
		for (int i = 0; i < 100; i++) {
			dataEd.setDatasetSelected("counterTimer01.lnI0It", true);
			EclipseUtils.delay(200);
			dataEd.setDatasetSelected("Energy", true);
			EclipseUtils.delay(200);
		}
		
		while (Job.getJobManager().currentJob() != null) EclipseUtils.delay(1000);
	    EclipseUtils.delay(2000);
	}
	
	
	private NexusMultiPageEditor createNexusEditor(final String fileName) throws PartInitException {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		NexusMultiPageEditor   editor = (NexusMultiPageEditor)EclipseUtils.openExternalEditor("testfiles/"+fileName);
		
		window.getActivePage().activate(editor);
		window.getActivePage().setPartState(window.getActivePage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
        return editor;
	}

	/**
	 * If you expand the tree manually by setting the delay longer,
	 * you can reproduce https://bugs.eclipse.org/bugs/show_bug.cgi?id=321143
	 * 
	 * This test also used to always give an out of memory but it seems to be a little better.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testOutOfMemError() throws Exception {

	    final NexusMultiPageEditor editor = createNexusEditor("FeKedge_1_15.nxs");
	
		editor.setActivePage(1);
		
		final NexusTreeEditor treeEd = (NexusTreeEditor)editor.getActiveEditor();
		treeEd.expandAll();
		
		while (Job.getJobManager().currentJob() != null) EclipseUtils.delay(1000);
		EclipseUtils.delay(2000);
		
		editor.setActivePage(0);

	}
	
	/**
	 * Two data sets with different sizes.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDataSetsWithDifferentSizes() throws Exception {
		
	    final NexusMultiPageEditor editor = createNexusEditor("FeKedge_1_15.nxs");
		final DataSetPlotEditor dataEd = (DataSetPlotEditor)editor.getActiveEditor();
		dataEd.setDatasetSelected("Energy", true);               // 1D

	    // Now go to the data tab
		editor.setActivePage(2);
		
		EclipseUtils.delay(1000);
		editor.setActivePage(0);
	}

	
	/**
	 * Currently just checks if plotting sets with different dimensions 
	 * causes an exception, if does test fails.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDatasetsDifferentDimensions() throws Exception {

	    final NexusMultiPageEditor editor = createNexusEditor("FeKedge_1_15.nxs");
		final DataSetPlotEditor dataEd = (DataSetPlotEditor)editor.getActiveEditor();
		dataEd.setDatasetSelected("Energy", true);               // 1D
		dataEd.setDatasetSelected("xspress2System.data", false); // 3D

	    // Now go to the data tab
		editor.setActivePage(2);
		
		EclipseUtils.delay(1000);
		editor.setActivePage(0);
	}

	/**
	 * Should not show the first 1D tab as there are no 1D data sets.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDatasetsIn2D() throws Exception {

	    final NexusMultiPageEditor editor = createNexusEditor("i22-4996.nxs");
		final DataSetPlotEditor dataEd = (DataSetPlotEditor)editor.getActiveEditor();
		dataEd.setDatasetSelected("Calibration.data", true);  // 3D
		dataEd.setDatasetSelected("Hotwaxs.data",     false); // 3D

	    // Now go to the data tab
		editor.setActivePage(2);
		
		EclipseUtils.delay(1000);
		editor.setActivePage(0);
	}

}
