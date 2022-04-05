/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2022 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>EasyXT>Surface>Send Labels Image as Surface(s) to Imaris")
public class LabelsImageToSurfaceCommand implements Command {

    @Parameter
    ImagePlus imp;

    @Parameter
    Boolean sendImp;

    @Override
    public void run() {
        // Using the Image Name as a key to access the surface
        try {
            ISurfacesPrx surface = EasyXT.Surfaces.createFromLabels(imp);
            EasyXT.Scene.addItem(surface);

            if (sendImp) {
                if ( imp.getBitDepth() == EasyXT.Dataset.getBitDepth(EasyXT.Dataset.getCurrent()) ){
                    EasyXT.Dataset.addChannels(imp);
                } else{
                    System.out.println("Labels image and Imaris dataset have different bitdepth");
                }
            }

        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
