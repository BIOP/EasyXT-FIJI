package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.*;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ItemQuery;
import ch.epfl.biop.imaris.SpotsDetector;
import ch.epfl.biop.imaris.SurfacesDetector;
import ij.IJ;

import java.util.List;
import java.util.logging.Level;

/**
 * EasyXT Demo
 * <p>
 * After creations of Spots and a Surface, demo of how accessing objects in the imaris scene
 * <p>
 * This showcases how to create groups, and how to recover objects from the surpass scene
 * <p>
 * The simplest way is to use EasyXT directly, but a more fine-tuning is possible using
 * the {@link ItemQuery} class directly, which is the last example in this demo.
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * <p>
 * October 2020
 * <p>
 * EPFL - SV - PTECH - PTBIOP
 */
public class AllGettersDemo {

    public static void main(String... args) throws Exception {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            //Get Extents of currently open dataset to create he same thing, but with two channels
            IDataContainerPrx itemGroup = EasyXT.Scene.createGroup("Spots And Surface");
            EasyXT.Scene.addToScene(itemGroup);

            // Make a spot detector and detect the spots
            ISpotsPrx spots = SpotsDetector.Channel(2)
                    .setName("My Spots")
                    .setDiameter(3.0)
                    .setRegionsThresholdManual(100)
                    .isSubtractBackground(true)
                    .isRegionsFromLocalContrast(true)
                    .isRegionsSpotsDiameterFromVolume(false)
                    .isCreateRegionsChannel(false)
                    .build().detect();

            EasyXT.Scene.addToScene(spots);

            // Make a spot detector and detect the spots
            ISpotsPrx spots2 = SpotsDetector.Channel(1)
                    .setName("Sub Spots")
                    .setDiameter(2.0)
                    .setRegionsThresholdManual(100)
                    .isSubtractBackground(true)
                    .isRegionsFromLocalContrast(true)
                    .isRegionsSpotsDiameterFromVolume(false)
                    .isCreateRegionsChannel(false)
                    .build().detect();

            // Add it again
            EasyXT.Scene.addToScene( spots2, itemGroup );

            // Makes a surface detector and detect the surface
            ISurfacesPrx surface = EasyXT.Surfaces.create(0)
                    .setSmoothingWidth(1)
                    .setLowerThreshold(40)
                    .setName("My Surface")
                    .setColor(new Integer[]{255, 120, 45})
                    .build()
                    .detect();

            EasyXT.Scene.addToScene(surface);

            //Highest level getters for spots and surfaces

            // Single spot
            ISpotsPrx spotByName = EasyXT.Spots.getSpots("My Spots");

            // A Subspot
            ISpotsPrx subSpotByName = EasyXT.Spots.getSpots("Sub Spots", itemGroup);

            // All Spots in Scene
            List<ISpotsPrx> spotsList = EasyXT.Spots.getAllSpots();
            for (ISpotsPrx iSpotsPrx : spotsList) {
                IJ.log(EasyXT.Scene.getName(iSpotsPrx));
            }

            // All spots recursively
            ItemQuery.isRecursiveSearch = true;

            List<ISpotsPrx> recursiveSpotsList = EasyXT.Spots.getAllSpots();
            for (ISpotsPrx iSpotsPrx : recursiveSpotsList) {
                IJ.log("Recursively: "+EasyXT.Scene.getName(iSpotsPrx));
            }
            ItemQuery.isRecursiveSearch = false;
            // Single surface
            ISurfacesPrx surfaceByName = EasyXT.Surfaces.getSurfaces("My Surface");
            //ISurfacesPrx surfaceByPosition = EasyXT.getSurfaces( 0 ); // 0 based

            // All Surfaces in Scene
            List<ISurfacesPrx> surfacesList = EasyXT.Surfaces.getAllSurfaces();

            // Generic Getter if you need other things
            IDataItemPrx rawFrame = EasyXT.Scene.getItem("Frame 1");

            //it returns the right type
            IJ.log("Is " + EasyXT.Scene.getName(rawFrame) + " an IFramePrx?  - " + (rawFrame instanceof IFramePrx));

            // And you can cast it directly if you feel like it
            IFramePrx frame = (IFramePrx) rawFrame;

            // Lowest level getter, uses an ItemQuery
            IDataContainerPrx parent = EasyXT.Scene.createGroup("Some Parent");

            ItemQuery query = new ItemQuery.ItemQueryBuilder().setName("Spots 1").setParent(parent).build();
            List<IDataItemPrx> items = query.get();

            List<IDataItemPrx> allRawSpots = EasyXT.Scene.getAll("Spots");
            List<IDataItemPrx> allRawSurfaces = EasyXT.Scene.getAll("Surfaces");
            List<IDataItemPrx> allVolumes = EasyXT.Scene.getAll("Volume");

            IJ.log("Spot: " + spotByName.GetName());
        } catch (Error error) {
            EasyXT.log.log(Level.SEVERE, "Error during Demo:", error);
        }
    }
}
