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
