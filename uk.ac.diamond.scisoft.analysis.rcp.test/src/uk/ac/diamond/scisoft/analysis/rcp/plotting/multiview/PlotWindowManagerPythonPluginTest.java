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

package uk.ac.diamond.scisoft.analysis.rcp.plotting.multiview;

import org.eclipse.ui.IWorkbenchPage;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import uk.ac.diamond.scisoft.analysis.PythonHelper;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.IPlotWindowManager;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.multiview.MultiPlotViewTestBase.ThreadRunner.ThreadRunnable;

/**
 * Concrete class that tests RPC connection from launched Python
 */
public class PlotWindowManagerPythonPluginTest extends PlotWindowManagerPluginTestAbstract {

	private static IPlotWindowManager manager;

	private static final String IMPORT_SCISOFTPY_AS_DNP = "import os, sys;"
			+ "scisoftpath = os.getcwd() + '/../uk.ac.diamond.scisoft.python/src';" + "sys.path.append(scisoftpath);"
			+ "import scisoftpy as dnp;";

	@BeforeClass
	public static void setupRMIClient() {

		manager = new IPlotWindowManager() {

			private String runCommand(String command, String viewName) throws Exception {
				if (viewName == null)
					viewName = "None";
				else
					viewName = "'" + viewName + "'";
				String pythonStdout = PythonHelper.runPythonScript(IMPORT_SCISOFTPY_AS_DNP
						+ "print dnp.plot.window_manager." + command + "(" + viewName + ")", false);
				return pythonStdout.trim();
			}

			@Override
			public String openView(IWorkbenchPage page, String viewName) {
				try {
					return runCommand("open_view", viewName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String openDuplicateView(IWorkbenchPage page, String viewName) {
				try {
					return runCommand("open_duplicate_view", viewName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String[] getOpenViews() {
				try {
					String pythonStdout = PythonHelper.runPythonScript(IMPORT_SCISOFTPY_AS_DNP
							+ "print dnp.plot.window_manager.get_open_views()", false);
					pythonStdout = pythonStdout.trim();
					String[] split = PythonHelper.parseArray(pythonStdout);
					return split;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@AfterClass
	public static void dropReference() {
		manager = null;
	}


	@Override
	public String openDuplicateView(IWorkbenchPage page, final String viewName) {
		ThreadRunner threadRunner = new ThreadRunner(new ThreadRunnable() {

			@Override
			public Object run() throws Exception {
				return manager.openDuplicateView(null, viewName);
			}

		});
		return (String) threadRunner.run();
	}

	@Override
	public String openView(IWorkbenchPage page, final String viewName) {
		ThreadRunner threadRunner = new ThreadRunner(new ThreadRunnable() {

			@Override
			public Object run() throws Exception {
				return manager.openView(null, viewName);
			}

		});
		return (String) threadRunner.run();
	}

	@Override
	public String[] getOpenViews() {
		ThreadRunner threadRunner = new ThreadRunner(new ThreadRunnable() {

			@Override
			public Object run() throws Exception {
				return manager.getOpenViews();
			}

		});
		return (String[]) threadRunner.run();
	}

	
	
}
