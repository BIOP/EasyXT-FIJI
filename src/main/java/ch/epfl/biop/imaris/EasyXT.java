package ch.epfl.biop.imaris;

import Ice.ObjectPrx;
import Imaris.*;
import Imaris.Error;
import ImarisServer.IServerPrx;
import com.bitplane.xt.IceClient;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.HyperStackConverter;
import ij.process.*;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * EasyXT static class:
 *
 * Wrap Imaris Extension API in a convenient way
 *
 */

// TODO: Detecting Spots and Surfaces, with tracking

// TODO: Convert spots to channel

// TODO: Mask Channel using spots or surfaces

// TODO: Creating Spots And Surfaces

// TODO: Find way to batch process OpenImage() with doc

// TODO: Make Documentation

// TODO: Allow easy getting and setting of images per channel


public class EasyXT {
    private static IApplicationPrx app;
    private static IceClient mIceClient;
    public static Map<tType, Integer> datatype;
    private static String mEndPoints = "default -p 4029";

    /**
     * Standard logger
     */
    private static Consumer<String> log = (str) -> System.out.println("EasyXT : "+str);

    /**
     * Error logger
     */
    private static Consumer<String> errlog = (str) -> System.err.println("EasyXT : "+str);

    /**
     * Static initialisation :
     * Gets the Imaris server
     * Ensure the connection is closed on JVM closing
     */
    static {
        log.accept( "Initializing EasyXT" );
        // Populate static map : Imaris Type -> Bit Depth
        Map<tType, Integer> tmap = new HashMap<>( 4 );
        tmap.put( tType.eTypeUInt8, 8 );    // Unsigned integer, 8  bits : 0..255
        tmap.put( tType.eTypeUInt16, 16 );  // Unsigned integer, 16 bits : 0..65535
        tmap.put( tType.eTypeFloat, 32 );   // Float 32 bits
        tmap.put( tType.eTypeUnknown, -1 ); // ? Something else

        datatype = Collections.unmodifiableMap( tmap );

        mIceClient = new IceClient( "ImarisServer", mEndPoints, 10000 );
        app = getImaris( mIceClient.GetServer( ), 0 );

        // Closing connection on jvm close
        Runtime.getRuntime( ).addShutdownHook(
                new Thread(() -> {
                    log.accept( "Closing ICE Connection from Imaris..." );
                    CloseIceClient( );
                    log.accept( "Done." );
                })

        );

        log.accept( "Initialization Done. Ready to call EasyXT" );
    }

    // TODO Refactor & Comment
    /* private static IceClient GetIceClient( ) {
        if ( mIceClient == null ) {
            mIceClient = new IceClient( "ImarisServer", mEndPoints, 10000 );
        }
        return mIceClient;
    }*/

    // TODO Refactor & Comment
    private static void CloseIceClient( ) {
        if ( mIceClient != null ) {
            mIceClient.Terminate( );
            mIceClient = null;
        }
    }

    // TODO Refactor & Comment
    private static IApplicationPrx getImaris(IServerPrx var0, int var1 ) {
        ObjectPrx var2 = var0.GetObject( var1 );
        return IApplicationPrxHelper.checkedCast( var2 );
    }

    /**
     * Returns instance of Imaris App
     * @return
     */
    public static IApplicationPrx getImaris( ) {
        return app;
    }

    /**
     * Helper method : returns the class of an Imaris Object
     * @param object
     * @return class of the contained object
     * @throws Error
     */
    private static Class<?> getType( IDataItemPrx object ) throws Error {
        IFactoryPrx factory = app.GetFactory( );
        if ( factory.IsSpots( object ) ) {
            return ISpots.class;
        }
        if ( factory.IsSurfaces( object ) ) {
            return ISurfaces.class;
        }
        if ( factory.IsVolume( object ) ) {
            return IVolume.class;
        }
        return null;
    }

    // TODO Comment
    public static IDataItemPrx getObject( String name, Class<? extends IDataItem> cls ) throws Error {

        IDataContainerPrx parent = app.GetSurpassScene( );
        int nChildren = parent.GetNumberOfChildren( );

        for ( int i = 0; i < nChildren; i++ ) {
            IDataItemPrx child = parent.GetChild( i );

            String aname = child.GetName( );
            Class acls = getType( child );
            //out.println( acls == cls );
            if ( aname.equals( name ) )
                return parent.GetChild( i );
        }
        return null;
    }

    // TODO Comment
    public static ISurfacesPrx getSurfaces( String name ) throws Error {
        IDataItemPrx object = getObject( name, ISurfaces.class );
        ISurfacesPrx surf = app.GetFactory( ).ToSurfaces( object );
        return surf;
    }

    /**
     * Returns an ImagePlus image of a dataset
     * TODO : add a way to select only a subpart of it
     *
     * @param dataset
     * @return
     * @throws Error
     */
    public static ImagePlus getImagePlus( IDataSetPrx dataset ) throws Error {

        int nc = dataset.GetSizeC( );
        int nz = dataset.GetSizeZ( );
        int nt = dataset.GetSizeT( );

        int w = dataset.GetSizeX( );
        int h = dataset.GetSizeY( );


        ImarisCalibration cal = new ImarisCalibration( dataset );
        int bitdepth = getBitDepth( dataset );

        ImageStack stack = ImageStack.create( w, h, nc * nz * nt, bitdepth );
        ImagePlus imp = new ImagePlus( app.GetCurrentFileName( ), stack );
        imp.setDimensions( nc, nz, nt );

        for ( int c = 0; c < nc; c++ ) {
            for ( int z = 0; z < nz; z++ ) {
                for ( int t = 0; t < nt; t++ ) {
                    int idx = imp.getStackIndex( c + 1, z + 1, t + 1 );
                    ImageProcessor ip;
                    switch ( bitdepth ) {
                        case 8:
                            byte[] datab = dataset.GetDataSubVolumeAs1DArrayBytes( 0, 0, z, c, t, w, h, 1 );
                            ip = new ByteProcessor( w, h, datab, null );
                            stack.setProcessor( ip, idx );
                            break;
                        case 16:
                            short[] datas = dataset.GetDataSubVolumeAs1DArrayShorts( 0, 0, z, c, t, w, h, 1 );
                            ip = new ShortProcessor( w, h, datas, null );
                            stack.setProcessor( ip, idx );
                            break;
                        case 32:
                            float[] dataf = dataset.GetDataSubVolumeAs1DArrayFloats( 0, 0, z, c, t, w, h, 1 );
                            ip = new FloatProcessor( w, h, dataf, null );
                            stack.setProcessor( ip, idx );
                            break;
                    }
                }
            }
        }
        imp.setStack( stack );
        imp.setCalibration( cal );
        imp = HyperStackConverter.toHyperStack( imp, nc, nz, nt );

        // Set LookUpTables
        if (imp instanceof CompositeImage) {

            LUT[] luts = new LUT[nc];

            for (int c=0;c<nc;c++) {
                Color color = EasyXT.getColorFromInt(dataset.GetChannelColorRGBA(c));
                luts[c] = LUT.createLutFromColor(color);
            }
            // TODO : transfer min max
            ((CompositeImage)imp).setLuts(luts);
        } else if (nc==1) {
            // TODO : transfer min max
            imp.setLut(LUT.createLutFromColor(EasyXT.getColorFromInt(dataset.GetChannelColorRGBA(0))));
        }

        return imp;

    }

    /**
     * Set data from an ImagePlus image into a dataset
     * TODO : add a way to select only a subpart of it
     * TODO : Return a new dataset or the existing dataset?
     *
     * @param dataset
     * @return
     * @throws Error
     */
    public static void setImagePlus( IDataSetPrx dataset, ImagePlus imp ) throws Error {

        int nc = dataset.GetSizeC( );
        int nz = dataset.GetSizeZ( );
        int nt = dataset.GetSizeT( );

        int w = dataset.GetSizeX( );
        int h = dataset.GetSizeY( );


        ImarisCalibration cal = new ImarisCalibration( dataset );
        int bitdepth = getBitDepth( dataset );

        for ( int c = 0; c < nc; c++ ) {
            for ( int z = 0; z < nz; z++ ) {
                for ( int t = 0; t < nt; t++ ) {
                    int idx = imp.getStackIndex( c + 1, z + 1, t + 1 );
                    ImageProcessor ip  = imp.getStack().getProcessor(idx);
                    switch ( bitdepth ) {
                        case 8:
                            dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), 0, 0, z, c, t, w, h, 1);
                            break;
                        case 16:
                            dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), 0, 0, z, c, t, w, h, 1);
                            break;
                        case 32:
                            dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), 0, 0, z, c, t, w, h, 1);
                            break;
                    }
                }
            }
        }
    }

    public static IDataSetPrx getCurrentDataset() throws Error { return EasyXT.getImaris().GetDataSet(); }
    public static void setCurrentDataset( IDataSetPrx dataset ) throws Error { EasyXT.getImaris().SetDataSet( dataset ); }

    /**
     * Adds the selected ImagePlus to the provided IDatasetPrx by appending it as new channels
     * This will work only if the ImagePlus is not larger than the IDatasetPrx
     * @param imp the image to add to the current dataset
     * @throws Error an imaris error object
     */
    public static void addChannels( ImagePlus imp ) throws Error {

        // Ensure that the image is not larger than the dataset
        IDataSetPrx dataset = EasyXT.getCurrentDataset( );

        addChannels( dataset, imp, 0,0,0,0 );
    }

    public static void addChannels( IDataSetPrx dataset, ImagePlus imp) throws Error {
        addChannels( dataset, imp, 0,0,0,0 );
    }

    /**
     * Adds the selected ImagePlus to the provided IDatasetPrx by appending it as new channels
     * The user can define the start location XYZ in pixels
     * @param imp
     * @param dataset
     * @param xstart
     * @param ystart
     * @param zstart
     * @throws Error
     */
    public static void addChannels( IDataSetPrx dataset, ImagePlus imp, int xstart, int ystart, int zstart, int tstart ) throws Error {

        // Get Metadata on dataset and image
        ImarisCalibration dCal = new ImarisCalibration( dataset );
        int dc = dataset.GetSizeC();

        Calibration iCal = imp.getCalibration( );
        int iw = imp.getWidth();
        int ih = imp.getHeight();
        int iz = imp.getNSlices();
        int it = imp.getNFrames();
        int ic = imp.getNChannels( );

        int dBitDepth = getBitDepth( dataset );
        int iBitDepth = imp.getBitDepth();

        if ( !( dCal.xSize >= (xstart + iw ) &&
                dCal.ySize >= (ystart + ih ) &&
                dCal.zSize >= (zstart + iz ) &&
                dCal.tSize >= (tstart + it ) ) ) {

            String errorDetail = "Dataset\t(X,\tY,\tZ,\tT):\t (" + dCal.xSize + ",\t" + dCal.ySize + ",\t" + dCal.zSize + ",\t" + dCal.tSize + ")";
            errorDetail += "\nImage\t(X,\tY,\tZ,\tT):\t (" + iw + ",\t" + ih + ",\t" + iz + ",\t" + it + ")";
            errorDetail += "\nIncl. offset\t(X,\tY,\tZ,\tT):\t (" + ( iw + xstart ) + ",\t" + ( ih + ystart ) + ",\t" + ( iz + zstart ) + ",\t" + ( it + tstart ) + ")";

            throw new Error( "Size Mismatch", "Dataset and ImagePlus do not have the same size in XYZT", errorDetail );
        }

        if (dBitDepth != iBitDepth) {
            String errorDetail = "   Dataset:" + dBitDepth + "-bit";
            errorDetail += "\nImage:" + iBitDepth + "-bit";

            throw new Error( "Bit Depth Mismatch", "Dataset and ImagePlus do not have same bit depth", errorDetail );
        }

        // Issue warning in case voxel sizes do not match
        if ( dCal.pixelDepth != iCal.pixelDepth &&
                dCal.pixelHeight != iCal.pixelHeight &&
                dCal.pixelWidth != iCal.pixelWidth ) {

            log.accept( "Warning: Voxel Sizes between Dataset and ImagePlus do not match:" );
            log.accept( "   Dataset Voxel Size\t(X,\tY,\tZ):\t"+dCal.pixelWidth+",\t"+dCal.pixelHeight+",\t"+dCal.pixelDepth+")");
            log.accept( "   Image Voxel Size\t(X,\tY,\tZ):\t"+iCal.pixelWidth+",\t"+iCal.pixelHeight+",\t"+iCal.pixelDepth+")");


        }

        // Enlarge the dataset by setting its size to the cumulated number of channels
        dataset.SetSizeC( dc+ic );

        // Now loop through the dimensions of the ImagePlus to add data
        for ( int c = 0; c < ic; c++ ) {
            int idx = imp.getStackIndex( c + 1, 1, 1 );
            int color = imp.getStack( ).getProcessor( idx ).getColorModel( ).getRGB( 255 );

            dataset.SetChannelColorRGBA( c, color );

            for ( int z = 0; z < iz; z++ ) {
                for ( int t = 0; t < it; t++ ) {
                    idx = imp.getStackIndex( c + 1, z + 1, t + 1 );
                    ImageProcessor ip  = imp.getStack().getProcessor(idx);
                    switch ( dBitDepth ) {
                        case 8:
                            dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), xstart, ystart, zstart+z, c+dc, tstart+t, iw, ih, 1); // last element is sizeZ (one slice at a time)
                            break;
                        case 16:
                            dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), xstart, ystart, zstart+z, c+dc, tstart+t, iw, ih, 1);
                            break;
                        case 32:
                            dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), xstart, ystart, zstart+z, c+dc, tstart+t, iw, ih, 1);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Adds the provided Object to the Main Imaris Scene
     * @param item the item (Spot, Surface, Folder to add
     * @throws Error
     */
    public static void addToScene( IDataContainerPrx object) throws Error {
        app.GetSurpassScene().AddChild( object, -1 );
    }

    /**
     * Adds the provided item as the last child to the provided parent object
     * @param parent The parent object
     * @param item the item to add as a child
     * @throws Error
     */
    public static void addToScene( IDataContainerPrx parent, IDataItemPrx object) throws Error {
        parent.AddChild( object, -1 ); // last element is position. -1 to append at the end.
    }

    public static void removeFromScene( IDataItemPrx item) throws Error {
        item.GetParent().RemoveChild( item );
    }

    public static IDataContainerPrx createGroup( String groupName ) throws Error {
        IDataContainerPrx group = app.GetFactory().CreateDataContainer( );
        group.SetName( groupName );
        return group;
    }

    /*
     * Surface Related functions
     */

    // TODO Comment
    public static IDataSetPrx getSurfaceDataset(ISurfacesPrx surface ) throws Error {
        // Check if there are channels
        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet( ) );

        IDataSetPrx final_dataset = app.GetDataSet( ).Clone( );
        final_dataset.SetSizeC( 1 );
        final_dataset.SetType( tType.eTypeUInt8 );

        // Loop through each timepoint, and get the dataset, then replace
        for ( int t = 0; t < cal.tSize; t++ ) {
            IDataSetPrx one_timepoint = getSurfaceDataset( surface, 1.0, t );
            final_dataset.SetDataVolumeAs1DArrayBytes( one_timepoint.GetDataVolumeAs1DArrayBytes( 0, 0 ), 0, t );
        }

        return final_dataset;
    }

    // TODO Comment
    public static ImagePlus getSurfaceMask( ISurfacesPrx surface ) throws Error {

        // Get raw ImagePlus
        ImagePlus impSurface = getImagePlus( getSurfaceDataset(surface) );

        // Multiply by 255 to allow to use ImageJ binary functions
        int nProcessor = impSurface.getStack().getSize();
        IntStream.range(0, nProcessor).parallel().forEach(index -> {
            impSurface.getStack().getProcessor(index+1).multiply(255);
        });

        // Set LUT and display range
        impSurface.setLut(LUT.createLutFromColor(EasyXT.getColorFromInt(surface.GetColorRGBA())));
        impSurface.setDisplayRange(0,255);
        impSurface.setTitle(surface.GetName());

        return impSurface;
    }

    // TODO Comment
    public static void setSurfaceMask( ISurfacesPrx surface, ImagePlus imp ) throws Error {

        // Divide by 255 to allow to use ImageJ binary functions
        int nProcessor = imp.getStack().getSize();
        IntStream.range(0, nProcessor).parallel().forEach(index -> {
            imp.getStack().getProcessor(index+1).multiply(1.0/255.0);
        });

        IDataSetPrx dataset = EasyXT.getSurfaceDataset(surface);
        surface.RemoveAllSurfaces();
        EasyXT.setImagePlus(dataset, imp);
        surface.AddSurface(dataset,0);

        IntStream.range(0, nProcessor).parallel().forEach(index -> {
            imp.getStack().getProcessor(index+1).multiply(255);
        });

    }

    // TODO Comment
    public static ImagePlus getSurfaceMask( ISurfacesPrx surface, int timepoint ) throws Error {
        return getSurfaceMask( surface, 1.0, timepoint );
    }

    // TODO Comment
    public static ImagePlus getSurfaceMask( ISurfacesPrx surface, double downsample, int timepoint ) throws Error {

        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet( ) ).getDownsampled( downsample );

        IDataSetPrx data = surface.GetMask( (float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                cal.xSize, cal.ySize, cal.zSize, timepoint );

        ImagePlus imp = getImagePlus( data );
        imp.setCalibration( cal );

        return imp;
    }

    // TODO Comment
    public static IDataSetPrx getSurfaceDataset( ISurfacesPrx surface, double downsample, int timepoint ) throws Error {
        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet( ) ).getDownsampled( downsample );

        IDataSetPrx data = surface.GetMask( (float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                cal.xSize, cal.ySize, cal.zSize, timepoint );
        return data;
    }

    /*
     * Spot Related Functions
     * openImage, opens the file from filepath in a new imaris scene
     *
     * @param filepath path  to an *.ims file
     * @param options option string cf : xtinterface/structImaris_1_1IApplication.html/FileOpen
     * @throws Error
     */
    public static void openImage(File filepath, String options) throws Error {
        if (!filepath.exists()) {
            errlog.accept(filepath + "doesn't exist");
            return;
        }

        if (!filepath.isFile() ) {
            errlog.accept(filepath + "is not a file");
            return;
        }

        if (!filepath.getName().endsWith("ims") ) {
            errlog.accept(filepath + "is not an imaris file, please convert your image first");
            return;
        }

        app.FileOpen( filepath.getAbsolutePath(), options);

    }

    /**
     * overloaded method , see {@link #openImage(File, String)}
     *
     * @param filepath to an *.ims file
     * @throws Error
     */
    public static void openImage(File filepath) throws Error {

        openImage(filepath, "");

    }

    /**
     * saveImage, saves the current imaris scene to an imaris file
     *
     * @param filepath path to save ims file
     * @param options option string cf : xtinterface/structImaris_1_1IApplication.html/FileSave
     *                eg writer="BMPSeries". List of formats available: Imaris5, Imaris3, Imaris2,SeriesAdjustable,
     *                TiffSeriesRGBA, ICS, OlympusCellR, OmeXml, BMPSeries, MovieFromSlices.
     * @throws Error
     */
    public static void saveImage(File filepath, String options ) throws Error {
        if (!filepath.getName().endsWith("ims") ) {
            filepath = new File ( filepath.getAbsoluteFile()+".ims");
            System.out.println("Saved as : "+filepath.getAbsoluteFile());
        }
        app.FileSave( filepath.getAbsolutePath(), options);

    }

    /**
     * overloaded method , see {@link #saveImage(File, String)}
     *
     * @param filepath path to save ims file
     * @throws Error
     */
    public static void saveImage(File filepath ) throws Error {

        saveImage( filepath, "");

    }

    // TODO Comment
    public ISpotsPrx getSpots( String name ) throws Error {
        IDataItemPrx object = getObject( name, ISpots.class );
        ISpotsPrx spot = app.GetFactory( ).ToSpots( object );
        return spot;
    }


    /**
     * Helpers
     */
    // TODO Comment
    private enum ItemClass {
        SPOTS( ISpotsPrx.class ),
        SURFACES( ISurfaces.class );

        Class cls;

        ItemClass( Class cls ) {
            this.cls = cls;
        }

        public Class getType( ) {
            return this.cls;
        }
    }

    // TODO Comment
    public static Color getColorFromInt(int color) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(color).array();
        int[] colorArray = new int[3];
        colorArray[0] = bytes[3] & 0xFF;
        colorArray[1] = bytes[2] & 0xFF;
        colorArray[2] = bytes[1] & 0xFF;
        return getColorIntFromIntArray(colorArray);
    }

    // TODO Comment
    public static Color getColorIntFromIntArray(int[] color) {
        return new Color(color[0], color[1], color[2]);
    }

    /**
     * Returns bitdepth of a dataset.
     * See {@link EasyXT#datatype}
     * @param dataset
     * @return
     * @throws Error
     */
    public static int getBitDepth( IDataSetPrx dataset ) throws Error {
        tType type = dataset.GetType( );
        // Thanks NICO
        return datatype.get( type );
    }

}
