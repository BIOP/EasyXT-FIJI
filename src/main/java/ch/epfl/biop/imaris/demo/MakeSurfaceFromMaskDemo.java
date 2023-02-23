/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2023 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.WaitForUserDialog;

/**
 * EasyXT Demo
 * How to get a surface in ImageJ and send a modified version back to Imaris
 *
 * @author Romain Guiet
 * @author Nicolas Chiaruttini
 * @author Olivier Burri
 * January 2021
 * EPFL - SV - PTECH - PTBIOP
 */

public class MakeSurfaceFromMaskDemo {

    public static void main(String... args) throws Exception {

        try {
            // Surface created and shown in ImageJ
            MakeAndGetSurfaceDemo.main();

            ImagePlus surfaceImp = IJ.getImage(); // Surface Image

            if ((args.length > 0) && (args[0].equals("Test Mode"))) {
                IJ.log("The surface will be skeletonized ...");
            } else {
                new WaitForUserDialog("The surface will be skeletonized ...").show();
            }
            // because of skeletonize
            Prefs.blackBackground = true;
            IJ.run(surfaceImp, "Skeletonize", "stack");

            if ((args.length > 0) && (args[0].equals("Test Mode"))) {
                IJ.log("And sent back to Imaris ...");
            } else {
                new WaitForUserDialog("And sent back to Imaris ...").show();
            }

            ISurfacesPrx surface = EasyXT.Surfaces.create(surfaceImp);

            EasyXT.Scene.addItem(surface);
            surfaceImp.changes = false;

        } catch (Error error) {
            error.printStackTrace();
        }

    }
}
