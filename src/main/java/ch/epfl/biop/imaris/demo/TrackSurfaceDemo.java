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

import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.ISurfaces;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ItemTracker;
import ch.epfl.biop.imaris.SurfacesDetector;

/**
 * EasyXT Demo
 * How to make a surface using SurfaceDetector and show it in ImageJ
 *
 * @author Nicolas Chiaruttini
 * October 2020
 * EPFL - SV - PTECH - PTBIOP
 */

public class TrackSurfaceDemo {

    // Note : you need to be in the 3D View in order to perform
    public static void main(String... args) throws Exception {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Makes a surface detector and detect the surface
            ISurfacesPrx surface = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(1)
                    .setLowerThreshold(40)
                    .setUpperThreshold(255.0)
                    .setName("My Surface")
                    .setColor(new Integer[]{255, 120, 45})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.Scene.addItem(surface);
/*
            ISurfacesPrx surfaceTracked = (ISurfacesPrx) ItemTracker.Item(surface).
                    setMethod("AutoregressiveMotion")
                    .setMaxDistance((float) 5.0) // without a Max Distance, there is great chance you can't track object in time
                    //.setGapSize(3) // Filter is optional
                    //.setFilter("\"Track Duration\" above 50 s") // Filter is optional
                    .build().track();


            ISurfacesPrx surfaceTracked = (ISurfacesPrx) ItemTracker.Item(surface)
                    .useConnectedComponents()
                    //.setFilter("\"Track Duration\" above 50 s") // Filter is optional
                    .build().track();
            */

            ISurfacesPrx surfaceTracked = (ISurfacesPrx) EasyXT.Tracks.create(surface).useConnectedComponents().build().track();

            if (surfaceTracked!=null) EasyXT.Scene.addItem(surfaceTracked);
            else System.out.println("ERROR: can't create track");

        } catch (Error error) {
            System.out.println("ERROR:" + error.mDescription);
            System.out.println("LOCATION:" + error.mLocation);
            System.out.println("String:" + error.toString());
        }
    }
}
