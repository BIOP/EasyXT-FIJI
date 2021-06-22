package ch.epfl.biop.imaris;

import Ice.ObjectPrx;
import Imaris.*;
import Imaris.Error;

import java.util.function.Consumer;

public class ItemTracker {

    /**
     * Standard logger
     */
    private static Consumer<String> log = (str) -> System.out.println("ItemTracker : " + str);

    /**
     * Error logger
     */
    private static Consumer<String> errlog = (str) -> System.err.println("ItemTracker : " + str);


    // Fields from Imaris API
    // file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.6.0/html/xtinterface/structImaris_1_1IImageProcessing.html
    //
    // minimal value
    ObjectPrx aItem; // either aSpots or aSurfaces
    String aTrackFiltersString; // for all

    float aMaximalDistance; // required for AutoregressiveMotion(Expert), Brownian , Lineage
    int aGapSize; // required for AutoregressiveMotion(Expert), Brownian , Lineage

    float aIntensityWeight;   // required for AutoregressiveMotionExpert

    String aMethod;

    public static ItemTrackerBuilder Item(ObjectPrx aItem) throws Error {
        return ItemTrackerBuilder.setItem(aItem);
    }

    public ObjectPrx track() throws Error {

        IFactoryPrx factory = EasyXT.Utils.getImarisApp().GetFactory();
        ObjectPrx aItemTracked = null;

        if (aMethod != null) {
            if (factory.IsSpots(aItem)) {
                ISpotsPrx spots_totrack = (ISpotsPrx) EasyXT.Utils.castToType(aItem);

                ISpotsPrx spots_tracked = null;

                if (aMethod.equals("ConnectedComponents")) {
                    spots_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSpotsConnectedComponents(spots_totrack, aTrackFiltersString);
                } else if (aMethod.equals("BrownianMotion")) {
                    spots_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSpotsBrownianMotion(spots_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod.equals("Lineage")) {
                    spots_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSpotsLineage(spots_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod.equals("AutoregressiveMotion")) {
                    spots_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSpotsAutoregressiveMotion(spots_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod.equals("AutoregressiveMotionExpert")) {
                    spots_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSpotsAutoregressiveMotionExpert(spots_totrack, aMaximalDistance, aGapSize, aIntensityWeight, aTrackFiltersString);
                }

                aItemTracked = spots_tracked;

            } else if (factory.IsSurfaces(aItem)) {

                ISurfacesPrx surfaces_totrack = (ISurfacesPrx) EasyXT.Utils.castToType(aItem);

                ISurfacesPrx surfaces_tracked = null;

                if (aMethod == "ConnectedComponents") {
                    surfaces_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSurfacesConnectedComponents(surfaces_totrack, aTrackFiltersString);
                } else if (aMethod == "BrownianMotion") {
                    surfaces_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSurfacesBrownianMotion(surfaces_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod == "Lineage") {
                    surfaces_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSurfacesLineage(surfaces_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod == "AutoregressiveMotion") {
                    surfaces_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSurfacesAutoregressiveMotion(surfaces_totrack, aMaximalDistance, aGapSize, aTrackFiltersString);
                } else if (aMethod == "AutoregressiveMotionExpert") {
                    surfaces_tracked = EasyXT.Utils.getImarisApp().GetImageProcessing().TrackSurfacesAutoregressiveMotionExpert(surfaces_totrack, aMaximalDistance, aGapSize, aIntensityWeight, aTrackFiltersString);
                }

                aItemTracked = surfaces_tracked;
            }
        } else {
            log.accept("Please define a Tracking Method, using setMethod(...), or useConnectedComponents , use...");
        }

        return aItemTracked;
    }


    public static final class ItemTrackerBuilder {
        ObjectPrx aItem;
        String aMethod;//= "AutoregressiveMotion";
        String aTrackFiltersString; // for all
        float aMaximalDistance; // required for AutoregressiveMotion(Expert), Brownian , Lineage
        int aGapSize; // required for AutoregressiveMotion(Expert), Brownian , Lineage
        float aIntensityWeight;   // required for AutoregressiveMotionExpert

        private ItemTrackerBuilder(ObjectPrx aItem) throws Error {
            this.aItem = aItem;
        }

        public static ItemTrackerBuilder setItem(ObjectPrx aItem) throws Error {
            return new ItemTrackerBuilder(aItem);
        }

        public ItemTrackerBuilder setMethod(String method) {
            this.aMethod = method;
            return this;
        }

        public ItemTrackerBuilder useConnectedComponents() {
            this.aMethod = "ConnectedComponents";
            return this;
        }

        public ItemTrackerBuilder useBrownianMotion() {
            this.aMethod = "BrownianMotion";
            return this;
        }

        public ItemTrackerBuilder useLineage() {
            this.aMethod = "Lineage";
            return this;
        }

        public ItemTrackerBuilder useAutoregressiveMotion() {
            this.aMethod = "AutoregressiveMotion";
            return this;
        }

        public ItemTrackerBuilder useAutoregressiveMotionExpert() {
            this.aMethod = "AutoregressiveMotionExpert";
            return this;
        }

        public ItemTrackerBuilder setFilter(String filter) {
            this.aTrackFiltersString = filter;
            return this;
        }

        public ItemTrackerBuilder setMaxDistance(float maximalDistance) {
            this.aMaximalDistance = maximalDistance;
            return this;
        }

        public ItemTrackerBuilder setGapSize(int gapSize) {
            this.aGapSize = gapSize;
            return this;
        }

        public ItemTrackerBuilder setIntensityWeight(int intensityWeight) {
            this.aIntensityWeight = intensityWeight;
            return this;
        }

        public ItemTracker build() throws Error {
            /*
            System.out.println(aItem);
            System.out.println(aMethod);
            System.out.println(aTrackFiltersString);
            System.out.println(aGapSize);
            System.out.println(aMaximalDistance);
            System.out.println(aIntensityWeight);
            */
            ItemTracker itemTracker = new ItemTracker();
            itemTracker.aItem = (ObjectPrx) this.aItem;
            itemTracker.aMethod = this.aMethod;
            itemTracker.aTrackFiltersString = this.aTrackFiltersString;
            itemTracker.aGapSize = this.aGapSize;
            itemTracker.aMaximalDistance = this.aMaximalDistance;
            itemTracker.aIntensityWeight = this.aIntensityWeight;

            return itemTracker;
        }
    }
    // ISurfaces = TrackSurfacesConnectedComponents (ISurfaces *aSurfaces, string aTrackFiltersString)
    // ISurfaces = TrackSurfacesAutoregressiveMotion (ISurfaces *aSurfaces, float aMaximalDistance, int aGapSize, string aTrackFiltersString)
    // ISurfaces = TrackSurfacesAutoregressiveMotionExpert (ISurfaces *aSurfaces, float aMaximalDistance, int aGapSize, float aIntensityWeight, string aTrackFiltersString)
    // ISurfaces = TrackSurfacesBrownianMotion (ISurfaces *aSurfaces, float aMaximalDistance, int aGapSize, string aTrackFiltersString)
    // ISurfaces = TrackSurfacesLineage (ISurfaces *aSurfaces, float aMaximalDistance, int aGapSize, string aTrackFiltersString)

    // ISpots =TrackSpotsAutoregressiveMotion (ISpots *aSpots, float aMaximalDistance, int aGapSize, string aTrackFiltersString)
    // ISpots = TrackSpotsAutoregressiveMotionExpert (ISpots *aSpots, float aMaximalDistance, int aGapSize, float aIntensityWeight, string aTrackFiltersString)
    // ISpots = TrackSpotsBrownianMotion (ISpots *aSpots, float aMaximalDistance, int aGapSize, string aTrackFiltersString)
    // ISpots = TrackSpotsConnectedComponents (ISpots *aSpots, string aTrackFiltersString)
    // ISpots = TrackSpotsLineage (ISpots *aSpots, float aMaximalDistance, int aGapSize, string aTrackFiltersString)
}
