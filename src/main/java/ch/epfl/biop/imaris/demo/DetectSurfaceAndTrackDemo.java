/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2022 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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
import Imaris.IDataItemPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;

import java.io.File;

public class DetectSurfaceAndTrackDemo {

    public static void main(String... args) throws Error, Exception {
        //IJ.run("Close All", "");

        // Imaris comes with a couple of images, stored into a user folder...
        // "celldemo.ims" , ...
        File image_path = EasyXT.Samples.getImarisDemoFile("CellDevelopment.ims");
        //println image_path

        EasyXT.Files.openImage(image_path);

        // Detect surfaces
        ISurfacesPrx nuclei_surf = EasyXT.Surfaces.create(2).build().detect();
        //uncomment below to have the "non-tracked" surface
        //EasyXT.Scene.setName(nuclei_surf, "3D Nuclei Segmentation")
        //EasyXT.Scene.addItem(nuclei_surf)

        // And finally track Surfaces and add to the scene
        IDataItemPrx tracked_surf = EasyXT.Tracks.create(nuclei_surf)
                .useBrownianMotion()
                .setMaxDistance(10.0f)
                .setGapSize(3)
                .build()
                .track();

        EasyXT.Scene.setName(tracked_surf, "Tracked_Nuclei");
        EasyXT.Scene.addItem(tracked_surf);
    }
}
