/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2021 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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
package ch.epfl.biop.imaris.demo;

import ch.epfl.biop.imaris.EasyXT;
import net.imagej.ImageJ;

import java.io.File;

/**
 * EasyXT Demo
 * <p>
 * - Creates a new instance of FIJI
 * - Opens the demo image, a cell reaching metaphase, 3 channels, 40 timepoints, 8 bits
 *
 * @author Nicolas Chiaruttini
 * <p>
 * October 2020
 * <p>
 * EPFL - SV - PTECH - PTBIOP
 */

public class FreshStartWithIJAndBIOPImsSample {

    public static void main(String... args) throws Imaris.Error {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        File sample = EasyXT.Samples.getSampleFile();
        EasyXT.Files.openImage(sample);
    }

}
