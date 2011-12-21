/*
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

package uk.ac.diamond.scisoft.analysis.rcp.plotting.fitting;

import java.awt.Color;

import org.eclipse.swt.graphics.RGB;

import uk.ac.diamond.scisoft.analysis.fitting.functions.APeak;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.roi.IRowData;

public class FittedPeakData implements IRowData {

	private APeak fittedPeak;
	private Color peakColour;
	private boolean plot = true;
	
	public FittedPeakData(APeak peak, Color colour) {
		fittedPeak = peak;
		peakColour = colour;
	}
	
	@Override
	public boolean isPlot() {
		return plot;
	}

	@Override
	public void setPlot(boolean require) {
		plot = require;
	}

	public Color getPeakColour() {
		return peakColour;
	}

	public void setPeakColour(Color peakColour) {
		this.peakColour = peakColour;
	}

	public APeak getFittedPeak() {
		return fittedPeak;
	}

	@Override
	public RGB getPlotColourRGB() {
		return new RGB(peakColour.getRed(), peakColour.getGreen(), peakColour.getBlue());
	}
}
