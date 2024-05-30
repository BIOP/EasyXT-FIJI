/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2024 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package ch.epfl.biop.imaris.command;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
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
            EasyXT.Scene.addItem(surf);
            surf.SetVisible(false);
            surf.SetVisible(true);

        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
