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
    Boolean aIntensityThresholdAutomatic;
    Float aIntensityThresholdManual;
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
    Boolean  	aIntensityLowerThresholdAutomatic;
    Float  	aIntensityLowerThresholdManual;
    Boolean  	aUpperThresholdEnabled;
    Boolean  	aIntensityUpperThresholdAutomatic;
    Float  	aIntensityUpperThresholdManual;

    // Fields added to modify output

    String name;
    Integer[] color;

    public void detect() throws Imaris.Error {

        ISurfacesPrx surfaces;

        if ((aLowerThresholdEnabled!=null)||
            (aIntensityLowerThresholdAutomatic!=null)||
            (aIntensityLowerThresholdManual!=null)||
            (aUpperThresholdEnabled!=null)||
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

            surfaces = EasyXT.getApp().GetImageProcessing().DetectSurfacesRegionGrowingWithUpperThreshold(aDataSet,
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

                surfaces = EasyXT.getApp().GetImageProcessing().DetectSurfacesRegionGrowing(aDataSet,
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

                surfaces = EasyXT.getApp().GetImageProcessing().DetectSurfaces(aDataSet,
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
            // TODO
            //surfaces.SetColor ( annoying stuff to convert )
        }

    }


    public static final class SurfacesDetectorBuilder {
        IDataSetPrx aDataSet;
        int[][] aRegionsOfInterest;
        Integer aChannelIndex;
        Float aSmoothFilterWidth;
        Float aLocalContrastFilterWidth;
        Boolean aIntensityThresholdAutomatic;
        Float aIntensityThresholdManual;
        String aSurfaceFiltersString;
        Float  	aSeedsEstimateDiameter;
        Boolean aSeedsSubtractBackground;
        String  aSeedsFiltersString;
        Boolean 	aLowerThresholdEnabled;
        Boolean  	aIntensityLowerThresholdAutomatic;
        Float  	aIntensityLowerThresholdManual;
        Boolean  	aUpperThresholdEnabled;
        Boolean  	aIntensityUpperThresholdAutomatic;
        Float  	aIntensityUpperThresholdManual;

        Integer[] color;
        String name;

        private SurfacesDetectorBuilder() throws Imaris.Error {
            // default values
            aDataSet = EasyXT.getApp().GetDataSet();
        }

        public static SurfacesDetectorBuilder aSurfacesDetector() throws Imaris.Error  {
            return new SurfacesDetectorBuilder();
        }

        public SurfacesDetectorBuilder aDataSet(IDataSetPrx aDataSet) {
            this.aDataSet = aDataSet;
            return this;
        }

        public SurfacesDetectorBuilder aRegionsOfInterest(int[][] aRegionsOfInterest) {
            this.aRegionsOfInterest = aRegionsOfInterest;
            return this;
        }

        public SurfacesDetectorBuilder aChannelIndex(Integer aChannelIndex) {
            this.aChannelIndex = aChannelIndex;
            return this;
        }

        public SurfacesDetectorBuilder aSmoothFilterWidth(Float aSmoothFilterWidth) {
            this.aSmoothFilterWidth = aSmoothFilterWidth;
            return this;
        }

        public SurfacesDetectorBuilder aLocalContrastFilterWidth(Float aLocalContrastFilterWidth) {
            this.aLocalContrastFilterWidth = aLocalContrastFilterWidth;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityThresholdAutomatic(Boolean aIntensityThresholdAutomatic) {
            this.aIntensityThresholdAutomatic = aIntensityThresholdAutomatic;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityThresholdManual(Float aIntensityThresholdManual) {
            this.aIntensityThresholdManual = aIntensityThresholdManual;
            return this;
        }

        public SurfacesDetectorBuilder aSurfaceFiltersString(String aSurfaceFiltersString) {
            this.aSurfaceFiltersString = aSurfaceFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder aSeedsEstimateDiameter(Float aSeedsEstimateDiameter) {
            this.aSeedsEstimateDiameter = aSeedsEstimateDiameter;
            return this;
        }

        public SurfacesDetectorBuilder aSeedsSubtractBackground(Boolean aSeedsSubtractBackground) {
            this.aSeedsSubtractBackground = aSeedsSubtractBackground;
            return this;
        }

        public SurfacesDetectorBuilder aSeedsFiltersString(String aSeedsFiltersString) {
            this.aSeedsFiltersString = aSeedsFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder aLowerThresholdEnabled(Boolean aLowerThresholdEnabled) {
            this.aLowerThresholdEnabled = aLowerThresholdEnabled;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityLowerThresholdAutomatic(Boolean aIntensityLowerThresholdAutomatic) {
            this.aIntensityLowerThresholdAutomatic = aIntensityLowerThresholdAutomatic;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityLowerThresholdManual(Float aIntensityLowerThresholdManual) {
            this.aIntensityLowerThresholdManual = aIntensityLowerThresholdManual;
            return this;
        }

        public SurfacesDetectorBuilder aUpperThresholdEnabled(Boolean aUpperThresholdEnabled) {
            this.aUpperThresholdEnabled = aUpperThresholdEnabled;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityUpperThresholdAutomatic(Boolean aIntensityUpperThresholdAutomatic) {
            this.aIntensityUpperThresholdAutomatic = aIntensityUpperThresholdAutomatic;
            return this;
        }

        public SurfacesDetectorBuilder aIntensityUpperThresholdManual(Float aIntensityUpperThresholdManual) {
            this.aIntensityUpperThresholdManual = aIntensityUpperThresholdManual;
            return this;
        }

        public SurfacesDetectorBuilder color(Integer[] color) {
            this.color = color;
            return this;
        }

        public SurfacesDetectorBuilder name(String name) {
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
            surfacesDetector.aIntensityThresholdAutomatic = this.aIntensityThresholdAutomatic;
            surfacesDetector.aSeedsEstimateDiameter = this.aSeedsEstimateDiameter;
            surfacesDetector.aIntensityThresholdManual = this.aIntensityThresholdManual;
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
