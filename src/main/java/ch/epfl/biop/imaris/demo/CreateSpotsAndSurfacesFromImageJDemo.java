/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2021 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISpotsPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ij.IJ;
import ij.ImagePlus;
import mcib3d.geom.Point3D;
import org.apache.commons.math3.FieldElement;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.DoubleStream;

public class CreateSpotsAndSurfacesFromImageJDemo {
    public static void main(String[] args) throws Error {
        FreshStartWithIJAndBIOPImsSample.main();

        // Create some coordinates
        ImarisCalibration cal = EasyXT.Dataset.getCalibration();

        // Create Spots at random from coordinates

        Random rnd = new Random(300484);
        int nSpots = 10;

        int nT = cal.tSize;

        Point3D radius = new Point3D(2, 2, 3);
        List<Point3D> coordinates = new ArrayList<>(nSpots*nT);
        List<Integer> timepoints = new ArrayList<>(nSpots*nT);
        List<Point3D> radii = new ArrayList<>(nSpots*nT);

        // Prepare random numbers for the spots
        PrimitiveIterator.OfDouble rx = rnd.doubles(cal.xOrigin, cal.xEnd).iterator();
        PrimitiveIterator.OfDouble ry = rnd.doubles(cal.yOrigin, cal.yEnd).iterator();
        PrimitiveIterator.OfDouble rz = rnd.doubles(cal.zOrigin, cal.zEnd).iterator();

        for (int t = 0; t < nT; t++) {
            for (int i = 0; i < nSpots; i++) {
                coordinates.add(new Point3D(rx.next(), ry.next(), rz.next()));
                timepoints.add(t);
                radii.add(radius);
            }
        }
        ISpotsPrx spots = EasyXT.Spots.create(coordinates, radii, timepoints);
        EasyXT.Scene.setName(spots, "Random Spots");
        EasyXT.Scene.addItem(spots);

        // Create surfaces from segmented image
        ImagePlus imp = EasyXT.Dataset.getImagePlus(EasyXT.Dataset.getCurrent());
        IJ.run(imp, "Convert to Mask", "method=Default background=Dark black");

        // Send surface back
        ISurfacesPrx surface = EasyXT.Surfaces.create(imp);

        EasyXT.Scene.setName(surface, "Surface From Mask");
        EasyXT.Scene.addItem(surface);
    }
}
