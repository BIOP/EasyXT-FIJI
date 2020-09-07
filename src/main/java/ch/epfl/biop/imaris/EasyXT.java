package ch.epfl.biop.imaris;

import Ice.ObjectPrx;
import Imaris.*;
import Imaris.Error;
import ImarisServer.IServerPrx;
import com.bitplane.xt.IceClient;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.HyperStackConverter;
import ij.process.*;
import net.imagej.ImageJ;

import java.awt.*;
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
                            /*System.out.println("c "+c+" z "+ z+" t "+t);
                            byte[] datab = dataset.GetDataSubVolumeAs1DArrayBytes( 0, 0, z, c, t, w, h, 1 );
                            System.out.println("Length i = "+datab.length);
                            System.out.println("Length "+((byte[])ip.getPixels()).length);*/
                            //(byte[]) ip.getPixels()
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
        if ( !( dataset.GetSizeX( ) >= imp.getWidth( ) && dataset.GetSizeY( ) >= imp.getHeight( ) && dataset.GetSizeZ( ) >= imp.getNSlices( ) && dataset.GetSizeT( ) >= imp.getNFrames( ) ) ) {
            errlog.accept( "Dataset and ImagePlus do not have the same size in XYZT" );
            errlog.accept( "  Dataset\t(X,\tY,\tZ,\tT):\t ("+dataset.GetSizeX( )+",\t"+dataset.GetSizeY( )+",\t"+dataset.GetSizeZ( )+",\t"+dataset.GetSizeT( )+")");
            errlog.accept( "  Image\t(X,\tY,\tZ,\tT):\t ("+imp.getWidth( )+",\t"+imp.getHeight( )+",\t"+imp.getNSlices( )+",\t"+imp.getNFrames( )+")");
            return;
        }
        addChannels( dataset, imp, 0,0,0 );
    }

    public static void addChannels( IDataSetPrx dataset, ImagePlus imp) throws Error {
        addChannels( dataset, imp, 0,0,0 );
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
    public static void addChannels( IDataSetPrx dataset, ImagePlus imp, int xstart, int ystart, int zstart ) throws Error {

        // Allow this only if images have the same bit depth
        int bitdepth = getBitDepth( dataset );

        // TODO : Is this necessary? Perhaps Imaris does not care adding into to floats?
        if( bitdepth != imp.getBitDepth() ) {
            errlog.accept( "addChannels: Dataset and ImagePlus do not have the same bit depth ("+bitdepth+"-bit vs "+imp.getBitDepth()+"-bit respectively)" );
        }
        // Get the extents of the image
        Calibration cal = imp.getCalibration();

        int w = imp.getWidth();
        int h = imp.getHeight();

        int nz = imp.getNSlices();
        int nt = imp.getNFrames();
        int nc = imp.getNChannels( );

        int ndc = dataset.GetSizeC();

        // TODO : Check that the xyzt bounds of the imageplus do not exceed the bounds of the dataset OR enlarge the dataset as needed

        // Enlarge the dataset
        dataset.SetSizeC( ndc+nc );

        // Now loop through the dimensions of the ImagePlus to add data
        for ( int c = 0; c < nc; c++ ) {
            int idx = imp.getStackIndex( c + 1, 1, 1 );
            int color = imp.getStack( ).getProcessor( idx ).getColorModel( ).getRGB( 255 );

            dataset.SetChannelColorRGBA( c, color );

            for ( int z = 0; z < nz; z++ ) {
                for ( int t = 0; t < nt; t++ ) {
                    idx = imp.getStackIndex( c + 1, z + 1, t + 1 );
                    ImageProcessor ip  = imp.getStack().getProcessor(idx);
                    switch ( bitdepth ) {
                        case 8:
                            dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), xstart, ystart, zstart+z, c+ndc, t, w, h, 1);
                            break;
                        case 16:
                            dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), xstart, ystart, zstart+z, c+ndc, t, w, h, 1);
                            break;
                        case 32:
                            dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), xstart, ystart, zstart+z, c+ndc, t, w, h, 1);
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
    public static void addToScene( IDataContainerPrx item) throws Error {
        app.GetSurpassScene().AddChild( item, -1 );
    }

    /**
     * Adds the provided item as the last child to the provided parent object
     * @param parent The parent object
     * @param item the item to add as a child
     * @throws Error
     */
    public static void addToScene( IDataContainerPrx parent, IDataItemPrx item) throws Error {
        parent.AddChild( item, -1 );
    }

    public static IDataContainerPrx createGroup( String groupName ) throws Error {
        IDataContainerPrx group = app.GetFactory().CreateDataContainer( );
        group.SetName( groupName );
        return group;
    }


  /*  public ImagePlus getChannelImage(int channel ) throws Error {
        IDataSetPrx dataset = app.GetDataSet( );
        return getImagePlus( channel, dataset );
    }*/

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
     *
     */

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
    public enum ItemClass {
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
