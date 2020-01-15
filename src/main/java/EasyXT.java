import Imaris.*;
import Imaris.Error;
import ImarisServer.IServerPrx;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.*;


import static java.lang.System.out;

public class EasyXT {
    private static ImarisLib lib;
    private static IApplicationPrx app;

    private static  Map<tType,Integer> datatype;

    static {
        Map<tType, Integer> tmap = new HashMap<>( 4 );
        tmap.put( tType.eTypeUInt8, 8 );
        tmap.put( tType.eTypeUInt16, 16 );
        tmap.put( tType.eTypeFloat, 32 );
        tmap.put( tType.eTypeUnknown, -1 );

        datatype = Collections.unmodifiableMap( tmap );

        lib = new ImarisLib( );
        int id = lib.GetServer( ).GetObjectID( 0 );
        app = lib.GetApplication( id );
    }

    public static ImarisLib getLib() {
        return lib;
    }

    public static IApplicationPrx getApp() {
        return app;
    }

    private static Class<?> getType( IDataItemPrx object ) throws Error {
        IFactoryPrx factory = app.GetFactory( );

        if (factory.IsSpots( object )) {
            return ISpots.class;
        }
        if (factory.IsSurfaces( object )) {
            return ISurfaces.class;
        }
        return null;
    }

    public static IDataItemPrx getObject( String name, Class<? extends IDataItem> cls ) throws Error {

        IDataContainerPrx parent = app.GetSurpassScene( );
        int nChildren = parent.GetNumberOfChildren( );

        for ( int i = 0; i < nChildren; i++ ) {
            IDataItemPrx child = parent.GetChild( i );

            String aname = child.GetName();
            Class acls = getType(child);
            //out.println( acls == cls );
            if ( aname.equals( name ) )
                return parent.GetChild( i );
        }
        return null;
    }

    // TODO: Statistics Manipulation methods

    // TODO: Creating Spots and Surfaces

    // TODO: Detecting Spots and Surfaces, with tracking

    // TODO: Convert spots to channel

    // TODO: Mask Channel using spots or surfaces

    // TODO: Creating Spots And Surfaces

    // TODO: Find way to batch process OpenImage() with doc

    // TODO: Make Documentation

    // TODO: Allow easy getting and setting of images per channel

    public static ISurfacesPrx getSurfaces( String name )  throws Error {
        IDataItemPrx object = getObject( name,  ISurfaces.class);
        ISurfacesPrx surf = app.GetFactory( ).ToSurfaces( object );
        return surf;
    }

    public static ImagePlus getImagePlus( IDataSetPrx dataset ) throws Error {

        int nc = dataset.GetSizeC();;
        int nz = dataset.GetSizeZ();
        int nt = dataset.GetSizeT();

        int w = dataset.GetSizeX();
        int h = dataset.GetSizeY();
        ImarisCalibration cal = new ImarisCalibration( dataset );
        int bitdepth = getBitDepth( dataset );

        ImageStack stack = ImageStack.create( w,h,nc*nz*nt, bitdepth );
        ImagePlus imp = new ImagePlus( app.GetCurrentFileName(), stack );
        imp.setDimensions( nc, nz, nt );

            for( int c=0; c<nc; c++ ) {
                for( int z=0; z<nz; z++ ) {
                    for( int t=0; t<nt; t++ ) {
                        int idx = imp.getStackIndex( c+1, z+1, t+1 );
                        ImageProcessor ip;
                        switch (bitdepth) {
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

        imp = HyperStackConverter.toHyperStack( imp, nc,nz,nt );
        return imp;

    }

    public static int getBitDepth( IDataSetPrx dataset ) throws Error {
        tType type = dataset.GetType( );
        // Thanks NICO
        return datatype.get( type );
    }

  /*  public ImagePlus getChannelImage(int channel ) throws Error {
        IDataSetPrx dataset = app.GetDataSet( );
        return getImagePlus( channel, dataset );
    }*/

    public static ImagePlus getSurfaceMask( ISurfacesPrx surface ) throws Error {
        // Check if there are channels
        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet() );

        IDataSetPrx final_dataset = app.GetDataSet().Clone();
        final_dataset.SetSizeC( 1 );
        final_dataset.SetType( tType.eTypeUInt8 );

        // Loop through each timepoint, and get the dataset, then replace
        for ( int t=0; t<cal.tSize; t++ ) {
            IDataSetPrx one_timepoint = getSurfaceDataset( surface, 1.0, t );
            final_dataset.SetDataVolumeAs1DArrayBytes(one_timepoint.GetDataVolumeAs1DArrayBytes ( 0, 0) , 0, t );
        }

        return getImagePlus( final_dataset );

    }

    public static ImagePlus getSurfaceMask( ISurfacesPrx surface, int timepoint ) throws Error {
        return getSurfaceMask( surface, 1.0, timepoint );
    }

    public static ImagePlus getSurfaceMask( ISurfacesPrx surface, double downsample, int timepoint ) throws Error {

        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet() ).getDownsampled( downsample );

        IDataSetPrx data = surface.GetMask ( (float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                                             (float)cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                                              cal.xSize, cal.ySize, cal.zSize, timepoint );

        ImagePlus imp = getImagePlus( data );
        imp.setCalibration( cal );

        return imp;
    }

    public static IDataSetPrx getSurfaceDataset( ISurfacesPrx surface, double downsample, int timepoint ) throws Error {
        ImarisCalibration cal = new ImarisCalibration( app.GetDataSet( ) ).getDownsampled( downsample );

        IDataSetPrx data = surface.GetMask( (float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                cal.xSize, cal.ySize, cal.zSize, timepoint );
        return data;
    }

    public static void main( String[] args ) throws Error {
        try {
            //ISpotsPrx spots = e.getSpotsObject( "Spots From neutro" );

            ImageJ ij = new ImageJ();
            ij.setVisible(true);

            ISurfacesPrx surf = SurfacesDetector.Channel(1).setLowerThreshold(200).build().detect();

            //ISurfacesPrx surf = EasyXT.getSurfaces( "Surfaces 1" );
            EasyXT.getSurfaceMask( surf ).show();

        } catch ( Error error ) {
            out.println( "ERROR:"+ error.mDescription);
        }
    }

    public ISpotsPrx getSpots ( String name )  throws Error {
        IDataItemPrx object = getObject( name,  ISpots.class);
        ISpotsPrx spot = app.GetFactory().ToSpots( object );
        return spot;
    }

    public void addChannel() {
        }


    public enum ItemClass {
        SPOTS( ISpotsPrx.class),
        SURFACES(ISurfaces.class);

        Class cls;
        ItemClass(Class cls) {
            this.cls = cls;
        }

        public Class getType( ) {
            return this.cls;
        }
    }
}
