import Imaris.*;
import Imaris.Error;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
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

    public ISpotsPrx getSpots ( String name )  throws Error {
        IDataItemPrx object = getObject( name,  ISpots.class);
        ISpotsPrx spot = app.GetFactory().ToSpots( object );
        return spot;
    }

    public static ISurfacesPrx getSurfaces( String name )  throws Error {
        IDataItemPrx object = getObject( name,  ISurfaces.class);
        ISurfacesPrx surf = app.GetFactory( ).ToSurfaces( object );
        return surf;
    }

    public ImagePlus getChannelImage(int channel ) throws Error {
        IDataSetPrx dataset = app.GetDataSet( );
        return getChannelImage( channel, dataset );
    }

    public static ImagePlus getChannelImage( int channel, IDataSetPrx dataset ) throws Error {

        int nc = dataset.GetSizeC();;
        int nz = dataset.GetSizeZ();
        int nt = dataset.GetSizeT();

        int w = dataset.GetSizeX();
        int h = dataset.GetSizeY();

        int bitdepth = getBitDepth( dataset );

        ImageStack stack = ImageStack.create( w,h,nc*nz*nt, bitdepth );
        ImagePlus imp = new ImagePlus( app.GetCurrentFileName(), stack );
        imp.setDimensions( nc, nz, nt );

            for( int c=0; c<nc; c++ ) {
                for( int z=0; z<nz; z++ ) {
                    for( int t=0; t<nt; t++ ) {
                        int idx = imp.getStackIndex( channel, z+1, t+1 );
                        ImageProcessor ip;
                        switch (bitdepth) {
                            case 8:
                                byte[] datab = dataset.GetDataSubVolumeAs1DArrayBytes( 0, 0, z, channel - 1, t, w, h, 1 );
                                ip = new ByteProcessor( w, h, datab, null );
                                stack.setProcessor( ip, idx );
                                break;
                            case 16:
                                short[] datas = dataset.GetDataSubVolumeAs1DArrayShorts( 0, 0, z, channel - 1, t, w, h, 1 );
                                ip = new ShortProcessor( w, h, datas, null );
                                stack.setProcessor( ip, idx );
                                break;
                            case 32:
                                float[] dataf = dataset.GetDataSubVolumeAs1DArrayFloats( 0, 0, z, channel - 1, t, w, h, 1 );
                                ip = new FloatProcessor( w, h, dataf, null );
                                stack.setProcessor( ip, idx );
                                break;
                        }
                    }
                }
            }
        imp.setStack( stack );
        return imp;

    }

    public static int getBitDepth( IDataSetPrx dataset ) throws Error {
        tType type = dataset.GetType( );
        // Thanks NICO
        return datatype.get( type );
    }

    public static ImagePlus getSurfaceMask( ISurfacesPrx surface, int timepoint ) throws Error {

        DataExtents de = new DataExtents( app );

        IDataSetPrx data = surface.GetMask ( de.xstart, de.ystart, de.zstart, de.xend, de.yend, de.zend, de.sizex, de.sizey, de.sizez, timepoint );

        return getChannelImage( 1, data );

    }

    public void addChannel() {

    }


    public static void main( String[] args ) {
        try {
            //ISpotsPrx spots = e.getSpotsObject( "Spots From neutro" );

            ImageJ ij = new ImageJ();
            ij.setVisible(true);

            ISurfacesPrx surf = EasyXT.getSurfaces( "Surfaces 1" );


            EasyXT.getSurfaceMask(surf, 0).show();
        } catch ( Error error ) {
            out.println( "ERROR:"+ error.mDescription);
        }
    }
}
