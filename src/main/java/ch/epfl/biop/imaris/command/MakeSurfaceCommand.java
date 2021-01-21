package ch.epfl.biop.imaris.command;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SurfacesDetector;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>EasyXT>Surface>Make Surface In Imaris")
public class MakeSurfaceCommand implements Command {

    @Parameter(label = "Channel, zero index based")
    int channelIndex;

    @Parameter
    String surfaceName;

    @Parameter
    ColorRGB color;

    @Parameter
    double smoothingWidth = -1;

    @Parameter
    double lowerThreshold;

    @Override
    public void run() {
        try {
            ISurfacesPrx surf = EasyXT.Surfaces.create(channelIndex)
                    .setSmoothingWidth(smoothingWidth)
                    .setLowerThreshold(lowerThreshold)
                    .setName(surfaceName)
                    .setColor(new Integer[]{color.getRed(), color.getGreen(), color.getBlue()})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.Scene.addToScene(surf);
            surf.SetVisible(false);
            surf.SetVisible(true);

        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
