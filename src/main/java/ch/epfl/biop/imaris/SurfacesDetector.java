package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.ISurfacesPrx;

/**
 * Helper functions for surface detection in Imaris
 *
 * With the use of a builder pattern, this class combines the following Imaris functions :
 * - DetectSurfaces
 * - DetectSurfaceRegionGrowing TODO: implement and explain how the builder switch or not to this function
 * - DetectSurfacesRegionGrowingWithUpperThreshold TODO: implement and explain how the builder switch or not to this function
 *
 * The Builder is tuned to allow for an invisible switch between these functions depending on the builder methods calls
 * If an upper threshold is set {
 *     calling DetectSurfacesRegionGrowingWithUpperThreshold
 * } else if any of the seed detection parameters is set {
 *     calling DetectSurfaceRegionGrowing
 * } else {
 *     calling DetectSurfaces
 * }
 *
 * Authors
 * Nicolas Chiaruttini, nicolas.chiaruttini@epfl.ch
 * Olivier Burri, olivier.burri@epfl.ch
 *
 * BIOP, EPFL,  Jan 2020
 *
 * Useful links:
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html#a41275043bc718252958bce85f4d4561a
 */

public class SurfacesDetector {

    // Fields from Imaris API - DetectSurfaces
    // Process entire image if aRegionsOfInterest is empty.
    // Example of aSurfaceFiltersString: '"Volume" above automatic threshold'
    // If aSmoothFilterWidth is equal to zero, smoothing is disabled.
    // If aLocalContrastFilterWidth is equal to zero, local contrast is disabled.
    // If aIntensityThresholdAutomatic is true, aIntensityThresholdManual is ignored.

    IDataSetPrx aDataSet;
    int[][] aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
    Integer aChannelIndex;
    Float aSmoothFilterWidth;
    Float aLocalContrastFilterWidth;

    Boolean  	aIntensityLowerThresholdAutomatic; //Boolean aIntensityThresholdAutomatic;
    Float  	aIntensityLowerThresholdManual; //Float aIntensityThresholdManual;

    String aSurfaceFiltersString;

    // Additional fields from Imaris API - DetectSurfacesRegionGrowing
    // Example of aSeedsFiltersString: '"Quality" above 7.000'

    Float  	aSeedsEstimateDiameter;
    Boolean aSeedsSubtractBackground;
    String  aSeedsFiltersString;

    // Additional fields from Imaris API - DetectSurfacesRegionGrowingWithUpperThreshold()
    // Detect Surfaces with upper (or double) threshold and region growing.
    //  If aLowerThresholdEnabled is true, aIntensityLowerThresholdAutomatic and aIntensityLowerThresholdManual are ignored.
    //  If aIntensityLowerThresholdAutomatic is true, aIntensityLowerThresholdManual is ignored.
    //  If aUpperThresholdEnabled is true, aIntensityUpperThresholdAutomatic and aIntensityUpperThresholdManual are ignored.
    //  If aIntensityUpperThresholdAutomatic is true, aIntensityUpperThresholdManual is ignored.

    Boolean 	aLowerThresholdEnabled;
    //Boolean  	aIntensityLowerThresholdAutomatic;
    //Float  	aIntensityLowerThresholdManual;
    Boolean  	aUpperThresholdEnabled;
    Boolean  	aIntensityUpperThresholdAutomatic;
    Float  	aIntensityUpperThresholdManual;

    // Fields added to modify output

    String name;
    Integer[] color;

    public ISurfacesPrx detect() throws Imaris.Error {

        ISurfacesPrx surfaces;

        if ((aUpperThresholdEnabled!=null)||
            (aIntensityUpperThresholdAutomatic!=null)||
            (aIntensityUpperThresholdManual!=null)) {
            // DetectSurfacesRegionGrowingWithUpperThreshold

            // ISurfaces* Imaris::IImageProcessing::DetectSurfacesRegionGrowingWithUpperThreshold 	( 	IDataSet *  	aDataSet,
            //		tInts2D  	aRegionsOfInterest,
            //		int  	aChannelIndex,
            //		float  	aSmoothFilterWidth,
            //		float  	aLocalContrastFilterWidth,
            //		bool  	aLowerThresholdEnabled,
            //		bool  	aIntensityLowerThresholdAutomatic,
            //		float  	aIntensityLowerThresholdManual,
            //		bool  	aUpperThresholdEnabled,
            //		bool  	aIntensityUpperThresholdAutomatic,
            //		float  	aIntensityUpperThresholdManual,
            //		float  	aSeedsEstimateDiameter,
            //		bool  	aSeedsSubtractBackground,
            //		string  	aSeedsFiltersString,
            //		string  	aSurfaceFiltersString
            //	)

            // Need to deal with a Lower Threshold Enabled TODO

            surfaces = EasyXT.getImaris().GetImageProcessing().DetectSurfacesRegionGrowingWithUpperThreshold(aDataSet,
                    aRegionsOfInterest,
                    aChannelIndex,
                    aSmoothFilterWidth,
                    aLocalContrastFilterWidth,
                    aLowerThresholdEnabled,
                    aIntensityLowerThresholdAutomatic,
                    aIntensityLowerThresholdManual,
                    aUpperThresholdEnabled,
                    aIntensityUpperThresholdAutomatic,
                    aIntensityUpperThresholdManual,
                    aSeedsEstimateDiameter,
                    aSeedsSubtractBackground,
                    aSurfaceFiltersString,
                    aSurfaceFiltersString);

        } else {
            if ((aSeedsEstimateDiameter != null) || (aSeedsSubtractBackground != null) || (aSeedsFiltersString != null)) {
                // DetectSurfaceRegionGrowing
                // ISurfaces* Imaris::IImageProcessing::DetectSurfacesRegionGrowing 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aSmoothFilterWidth,
                //		float  	aLocalContrastFilterWidth,
                //		bool  	aIntensityThresholdAutomatic,
                //		float  	aIntensityThresholdManual,
                //		float  	aSeedsEstimateDiameter,
                //		bool  	aSeedsSubtractBackground,
                //		string  	aSeedsFiltersString,
                //		string  	aSurfaceFiltersString
                //	)

                surfaces = EasyXT.getImaris().GetImageProcessing().DetectSurfacesRegionGrowing(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aSeedsEstimateDiameter,
                        aSeedsSubtractBackground,
                        aSurfaceFiltersString,
                        aSurfaceFiltersString);

            } else {
                // DetectSurfaces

                //ISurfaces* Imaris::IImageProcessing::DetectSurfaces 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aSmoothFilterWidth,
                //		float  	aLocalContrastFilterWidth,
                //		bool  	aIntensityThresholdAutomatic,
                //		float  	aIntensityThresholdManual,
                //		string  	aSurfaceFiltersString
                //	)

                surfaces = EasyXT.getImaris().GetImageProcessing().DetectSurfaces(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aSurfaceFiltersString);
            }
        }

        // EasyXT Specific

        if (name!=null) {
            surfaces.SetName(name);
        } // or else it will have the default Imaris name


        if (color!=null) {
            surfaces.SetColorRGBA( color[0] + (color[1] * 256) + (color[2] * 256 * 256 ) );
        }

        return surfaces;
    }

    // Removes a bit of the verbosity
    public static SurfacesDetectorBuilder Channel(int indexChannel ) throws Error {
        return SurfacesDetectorBuilder.aSurfacesDetector(indexChannel);
    }

    public static final class SurfacesDetectorBuilder {

        IDataSetPrx aDataSet;
        int[][] aRegionsOfInterest;
        Integer aChannelIndex;
        Float aSmoothFilterWidth = new Float(0); // Default with disables smoothing
        Float aLocalContrastFilterWidth = new Float(0); // aLocalContrastFilterWidth is equal to zero, local contrast is disabled.
        //Boolean aIntensityThresholdAutomatic = new Boolean(true);  // If aIntensityThresholdAutomatic is true, aIntensityThresholdManual is ignored.
        //Float aIntensityThresholdManual = new Float(0); // Disabled by default because aIntensityThresholdAutomatic is true by default
        String aSurfaceFiltersString;
        Float  	aSeedsEstimateDiameter;
        Boolean aSeedsSubtractBackground;
        String  aSeedsFiltersString;
        Boolean 	aLowerThresholdEnabled;
        Boolean  	aIntensityLowerThresholdAutomatic = new Boolean(true);
        Float  	aIntensityLowerThresholdManual = new Float(0);
        Boolean  	aUpperThresholdEnabled;
        Boolean  	aIntensityUpperThresholdAutomatic;
        Float  	aIntensityUpperThresholdManual;

        Integer[] color;
        String name;

        private SurfacesDetectorBuilder(int channelIndex) throws Imaris.Error {
            // default values
            aDataSet = EasyXT.getImaris().GetDataSet();
            this.aChannelIndex = channelIndex;
        }

        public static SurfacesDetectorBuilder aSurfacesDetector(int channelIndex) throws Imaris.Error  {
            return new SurfacesDetectorBuilder(channelIndex);
        }

        public SurfacesDetectorBuilder setDataSet(IDataSetPrx aDataSet) {
            this.aDataSet = aDataSet;
            return this;
        }

        public SurfacesDetectorBuilder setROI(int[][] aRegionsOfInterest) {
            this.aRegionsOfInterest = aRegionsOfInterest;
            return this;
        }

        public SurfacesDetectorBuilder setSmoothingWidth(double aSmoothFilterWidth) {
            this.aSmoothFilterWidth = new Float(aSmoothFilterWidth);
            return this;
        }

        public SurfacesDetectorBuilder setLocalContrastFilterWidth(double aLocalContrastFilterWidth) {
            this.aLocalContrastFilterWidth = new Float(aLocalContrastFilterWidth);
            return this;
        }

        public SurfacesDetectorBuilder setSurfaceFilter(String aSurfaceFiltersString) {
            this.aSurfaceFiltersString = aSurfaceFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder setSeedsDiameter(double aSeedsEstimateDiameter) {
            this.aSeedsEstimateDiameter = new Float(aSeedsEstimateDiameter);
            return this;
        }

        public SurfacesDetectorBuilder isSeedsSubtractBackground(Boolean aSeedsSubtractBackground) {
            this.aSeedsSubtractBackground = aSeedsSubtractBackground;
            return this;
        }

        public SurfacesDetectorBuilder setSeedsFilter(String aSeedsFiltersString) {
            this.aSeedsFiltersString = aSeedsFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder enableAutomaticLowerThreshold() {
            this.aIntensityLowerThresholdAutomatic = true;
            this.aLowerThresholdEnabled = true;
            return this;
        }

        public SurfacesDetectorBuilder setLowerThreshold(double aIntensityLowerThresholdManual) {
            this.aIntensityLowerThresholdManual = new Float(aIntensityLowerThresholdManual);
            this.aLowerThresholdEnabled = true;
            this.aIntensityLowerThresholdAutomatic = false;
            return this;
        }

        public SurfacesDetectorBuilder enableAutomaticUpperThreshold() {
            this.aIntensityUpperThresholdAutomatic = true;
            this.aUpperThresholdEnabled = true;
            return this;
        }

        public SurfacesDetectorBuilder setUpperThreshold(double aIntensityUpperThresholdManual) {
            this.aIntensityUpperThresholdManual = new Float(aIntensityUpperThresholdManual);
            this.aUpperThresholdEnabled = true;
            this.aIntensityUpperThresholdAutomatic = false;
            return this;
        }

        public SurfacesDetectorBuilder setColor(Integer[] color) {
            this.color = color;
            return this;
        }

        public SurfacesDetectorBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SurfacesDetector build() {
            SurfacesDetector surfacesDetector = new SurfacesDetector();
            surfacesDetector.aIntensityLowerThresholdManual = this.aIntensityLowerThresholdManual;
            surfacesDetector.aUpperThresholdEnabled = this.aUpperThresholdEnabled;
            surfacesDetector.aDataSet = this.aDataSet;
            surfacesDetector.aLocalContrastFilterWidth = this.aLocalContrastFilterWidth;
            surfacesDetector.aRegionsOfInterest = this.aRegionsOfInterest;
            surfacesDetector.aLowerThresholdEnabled = this.aLowerThresholdEnabled;
            surfacesDetector.aIntensityUpperThresholdManual = this.aIntensityUpperThresholdManual;
            surfacesDetector.color = this.color;
            surfacesDetector.name = this.name;
            surfacesDetector.aSeedsEstimateDiameter = this.aSeedsEstimateDiameter;
            surfacesDetector.aSeedsFiltersString = this.aSeedsFiltersString;
            surfacesDetector.aSurfaceFiltersString = this.aSurfaceFiltersString;
            surfacesDetector.aIntensityLowerThresholdAutomatic = this.aIntensityLowerThresholdAutomatic;
            surfacesDetector.aSeedsSubtractBackground = this.aSeedsSubtractBackground;
            surfacesDetector.aSmoothFilterWidth = this.aSmoothFilterWidth;
            surfacesDetector.aIntensityUpperThresholdAutomatic = this.aIntensityUpperThresholdAutomatic;
            surfacesDetector.aChannelIndex = this.aChannelIndex;

            return surfacesDetector;
        }
    }
}