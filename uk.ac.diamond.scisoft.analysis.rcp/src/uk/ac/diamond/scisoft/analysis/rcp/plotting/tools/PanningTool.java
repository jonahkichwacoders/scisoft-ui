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

package uk.ac.diamond.scisoft.analysis.rcp.plotting.tools;

import java.util.Iterator;
import java.util.LinkedList;

import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Transformation;
import de.jreality.scene.data.DoubleArray;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
/**
 * A screen panning tool
 */

public class PanningTool extends AbstractTool {

	private static InputSlot activate = InputSlot.getDevice("PrimaryMenu");
	private static InputSlot trafo = InputSlot.getDevice("PointerNDC");
	private static InputSlot worldToNDC = InputSlot.getDevice("WorldToNDC");
	private SceneGraphComponent node;
    private double[] tempStorage = new double[16];
    private double[] oldTrafo = new double[16];
    private double[] newTrafo = new double[16];
	private double xSize = 1.0;
	private double ySize = 1.0;
    private LinkedList<PanActionListener> listeners = null;
    
    /**
	 * Constructor for the PaningTool
	 * @param node SceneGraphComponent node the transformation should applied to
	 */
	
	public PanningTool(SceneGraphComponent node) {
		super(activate);
		addCurrentSlot(trafo);
		this.node = node;
		listeners = new LinkedList<PanActionListener>();
	}
	
	@Override
	public void activate(ToolContext tc) {
		if (this.node.getTransformation() == null) node.setTransformation(new Transformation());
		DoubleArray tm = tc.getTransformationMatrix(trafo);
		tm.toDoubleArray(oldTrafo);
	}
	
	/**
	 * Set the image size dimension for the panning
	 * @param xObjSize x size in object space
	 * @param yObjSize  y size in object space
	 */
	
	public void setDataDimension(double xObjSize, double yObjSize) {

		xSize = xObjSize;
		ySize = yObjSize;
	}
	
	@Override
	public void perform(ToolContext tc) {
		DoubleArray tm = tc.getTransformationMatrix(trafo);
		tm.toDoubleArray(newTrafo);
		double myDeltaX = newTrafo[3] - oldTrafo[3];
		double myDeltaY = newTrafo[7] - oldTrafo[7];
		System.arraycopy(newTrafo,0,oldTrafo,0,16);
		DoubleArray worldNDCtm = tc.getTransformationMatrix(worldToNDC);
		tm.toDoubleArray(tempStorage);
		Matrix tempMat = new Matrix(worldNDCtm);
		double[] vec = {xSize - 0.5 * xSize, 
						ySize - 0.5 * ySize,0.0};
		tempMat.transformVector(vec);
		double xNDCspan = vec[0];
		double yNDCspan = vec[1];
		vec[0] = - 0.5 * xSize;
		vec[1] = -0.5 * ySize;
		tempMat.transformVector(vec);
		xNDCspan -= vec[0];
		yNDCspan -= vec[1];
		double xScaling = (xSize / xNDCspan) * myDeltaX;
		double yScaling = (ySize / yNDCspan) * myDeltaY;
		tempStorage[3] = xScaling;
		tempStorage[7] = yScaling;
		tempStorage[11] = 0.0;
		Matrix mat = MatrixBuilder.euclidean(node.getTransformation()).times(tempStorage).getMatrix();
		notifyPanActionListener(mat.getEntry(0,3),
								mat.getEntry(1,3));
		//tc.getViewer().render();
	}
	
	private void notifyPanActionListener(double xTrans, double yTrans) {
		Iterator<PanActionListener> iter = listeners.iterator();
		while (iter.hasNext()) {
			iter.next().panPerformed(xTrans, yTrans);
		}
	}
	
	public void removePanActionListener(PanActionListener listener) {
		listeners.remove(listener);
	}
	
	public void removeAllPanActionListener() {
		listeners.clear();
	}
	
	public void addPanActionListener(PanActionListener listener) {
		listeners.add(listener);
	}
	
}
