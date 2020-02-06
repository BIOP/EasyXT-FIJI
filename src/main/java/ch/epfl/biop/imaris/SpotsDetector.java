package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.ISpotsPrx;

/**
 * Helper functions for spots detection in Imaris
 *
 * With the use of a builder pattern, this class combines the following Imaris functions :
 * - DetectSpots2()
 * - DetectSpotsRegionGrowing() TODO: implement and explain how the builder switch or not to this function
 *
 * DetectSpotsWithRegions() not supported -> replaced by DetectSpotsRegionGrowing() T
 * DetectSpots() obsolete -> replaced by DetectSpots2()
 *
 * The Builder is tuned to allow for an invisible switch between these functions depending on the builder methods calls
 *
 * TODO : look at Elliptical spot detection
 *
 * Authors
 * Nicolas Chiaruttini, nicolas.chiaruttini@epfl.ch
 * Olivier Burri, olivier.burri@epfl.ch
 *
 * BIOP, EPFL,  Jan 2020
 *
 * Useful links:
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html#ae10348d92f0d2df6848ed44253c69391
 */

public class SpotsDetector {

    // Fields from Imaris API - DetectSpots2D (Replaces DetectSpots, implemented for bpPointsViewer.)
    // Process entire image if aRegionsOfInterest is empty.
    // Example of aSpotFiltersString: '"Position X" above 30.000 um "Intensity Center Ch=1" above automatic threshold'

    IDataSetPrx aDataSet;
    int[][] aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
    Integer aChannelIndex;
    Float aEstimateDiameter;
    Boolean aSubtractBackground;
    String aSpotFiltersString;

    // Additional fields from Imaris API - DetectSpotsRegionGrowing
    // If aRegionsFromLocalContrast is false, regions are computed from channel intensity.
    // If aRegionsThresholdAutomatic is true, aRegionsThresholdManual is ignored.
    // If aRegionsSpotsDiameterFromVolume is false, spots diameter is equal to region border distance.

    Boolean  	aRegionsFromLocalContrast;
    Boolean  	aRegionsThresholdAutomatic;
    Float  	aRegionsThresholdManual;
    Boolean  	aRegionsSpotsDiameterFromVolume;
    Boolean  	aRegionsCreateChannel;

    // Fields added to modify output

    String name;
    Integer[] color;

    public ISpotsPrx detect() throws Error {

        ISpotsPrx spots;

        if ((aRegionsFromLocalContrast!=null)||
            (aRegionsThresholdAutomatic!=null)||
            (aRegionsThresholdManual!=null)||
            (aRegionsSpotsDiameterFromVolume!=null)||
            (aRegionsCreateChannel!=null)) {
            // DetectSpotsRegionGrowing

            // ISpots* Imaris::IImageProcessing::DetectSpotsRegionGrowing 	( 	IDataSet *  	aDataSet,
            //		tInts2D  	aRegionsOfInterest,
            //		int  	aChannelIndex,
            //		float  	aEstimateDiameter,
            //		bool  	aSubtractBackground,
            //		string  	aSpotFiltersString,
            //		bool  	aRegionsFromLocalContrast,
            //		bool  	aRegionsThresholdAutomatic,
            //		float  	aRegionsThresholdManual,
            //		bool  	aRegionsSpotsDiameterFromVolume,
            //		bool  	aRegionsCreateChannel
            //	)

            spots = EasyXT.getApp().GetImageProcessing().DetectSpotsRegionGrowing(aDataSet,
                    aRegionsOfInterest,
                    aChannelIndex,
                    aEstimateDiameter,
                    aSubtractBackground,
                    aSpotFiltersString,
                    aRegionsFromLocalContrast,
                    aRegionsThresholdAutomatic,
                    aRegionsThresholdManual,
                    aRegionsSpotsDiameterFromVolume,
                    aRegionsCreateChannel);

        } else {
                // DetectSpots2()
                // ISpots* Imaris::IImageProcessing::DetectSpots2 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aEstimateDiameter,
                //		bool  	aSubtractBackground,
                //		string  	aSpotFiltersString
                //	)

                // TODO Understand what happens with the Map<String, String> with detectspots2

                spots = EasyXT.getApp().GetImageProcessing().DetectSpots2(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aEstimateDiameter,
                        aSubtractBackground,
                        aSpotFiltersString);
        }

        // EasyXT Specific

        if (name!=null) {
            spots.SetName(name);
        } // or else it will have the default Imaris name


        if (color!=null) {
            // TODO
            //spots.SetColor ( annoying stuff to convert )
        }

        return spots;
    }

    // Removes a bit of the verbosity
    public static SpotsDetectorBuilder Channel(int indexChannel ) throws Error {
        return SpotsDetectorBuilder.aSpotsDetector(indexChannel);
    }

    public static final class SpotsDetectorBuilder {

        IDataSetPrx aDataSet;
        int[][] aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
        Integer aChannelIndex;
        Float aEstimateDiameter;
        Boolean aSubtractBackground;
        String aSpotFiltersString;

        // Additional fields from Imaris API - DetectSpotsRegionGrowing
        // If aRegionsFromLocalContrast is false, regions are computed from channel intensity.
        // If aRegionsThresholdAutomatic is true, aRegionsThresholdManual is ignored.
        // If aRegionsSpotsDiameterFromVolume is false, spots diameter is equal to region border distance.

        Boolean  	aRegionsFromLocalContrast;
        Boolean  	aRegionsThresholdAutomatic;
        Float  	aRegionsThresholdManual;
        Boolean  	aRegionsSpotsDiameterFromVolume;
        Boolean  	aRegionsCreateChannel;

        // Fields added to modify output

        String name;
        Integer[] color;

        private SpotsDetectorBuilder(int channelIndex) throws Error {
            // default values
            aDataSet = EasyXT.getApp().GetDataSet();
            this.aChannelIndex = channelIndex;
        }

        public static SpotsDetectorBuilder aSpotsDetector(int channelIndex) throws Error  {
            return new SpotsDetectorBuilder(channelIndex);
        }

        public SpotsDetectorBuilder setDataSet(IDataSetPrx aDataSet) {
            this.aDataSet = aDataSet;
            return this;
        }

        public SpotsDetectorBuilder setROI(int[][] aRegionsOfInterest) {
            this.aRegionsOfInterest = aRegionsOfInterest;
            return this;
        }

        public SpotsDetectorBuilder setColor(Integer[] color) {
            this.color = color;
            return this;
        }

        public SpotsDetectorBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SpotsDetectorBuilder setDiameter(double diameter) {
            this.aEstimateDiameter = new Float(diameter);
            return this;
        }

        public SpotsDetectorBuilder isSubtractBackground(Boolean aSubtractBackground) {
            this.aSubtractBackground = aSubtractBackground;
            return this;
        }

        public SpotsDetectorBuilder setFilter(String aFiltersString) {
            this.aSpotFiltersString = aFiltersString;
            return this;
        }

        public SpotsDetectorBuilder isRegionsFromLocalContrast(Boolean aRegionsFromLocalContrast) {
            this.aRegionsFromLocalContrast = aRegionsFromLocalContrast;
            return this;
        }

        public SpotsDetectorBuilder enableRegionsThresholdAutomatic() {
            this.aRegionsThresholdAutomatic = true;
            return this;
        }

        public SpotsDetectorBuilder setRegionsThresholdManual(double threshold) {
            this.aRegionsThresholdManual = new Float(threshold);
            this.aRegionsThresholdAutomatic = false;
            return this;
        }

        public SpotsDetectorBuilder createRegionsChannel() {
            this.aRegionsCreateChannel = true;
            return this;
        }

        public SpotsDetectorBuilder isRegionsSpotsDiameterFromVolume(Boolean flag) {
            this.aRegionsSpotsDiameterFromVolume = flag;
            return this;
        }

        public SpotsDetector build() {
            SpotsDetector spotsDetector = new SpotsDetector();
            spotsDetector.aDataSet = this.aDataSet;
            spotsDetector.aRegionsOfInterest = this.aRegionsOfInterest;
            spotsDetector.aChannelIndex = this.aChannelIndex;
            spotsDetector.aEstimateDiameter = this.aEstimateDiameter;
            spotsDetector.aSubtractBackground = this.aSubtractBackground;
            spotsDetector.aSpotFiltersString = this.aSpotFiltersString;

            spotsDetector.aRegionsFromLocalContrast = this.aRegionsFromLocalContrast;
            spotsDetector.aRegionsThresholdAutomatic = this.aRegionsThresholdAutomatic;
            spotsDetector.aRegionsThresholdManual = this.aRegionsThresholdManual;
            spotsDetector.aRegionsSpotsDiameterFromVolume = this.aRegionsSpotsDiameterFromVolume;
            spotsDetector.aRegionsCreateChannel = this.aRegionsCreateChannel;

            spotsDetector.color = this.color;
            spotsDetector.name = this.name;

            return spotsDetector;
        }
    }
}
