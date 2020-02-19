package ch.epfl.biop.imaris.command;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "EasyXT>Surface>Get Surface from Imaris")
public class GetSurfaceCommand implements Command{

    @Parameter
    String surfaceName;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus surface;

    @Override
    public void run() {
        // Gets an existing surface
        try {
            ISurfacesPrx surfprx = EasyXT.getSurfaces( surfaceName );
            // Display surfaces
            surface = EasyXT.getSurfaceMask( surfprx );

        } catch (Error error) {
            error.printStackTrace();
        }


    }
}
