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

import Imaris.Error;
import Imaris.IDataSetPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;

/**
 * EasyXT Demo
 * This demo shows how you can send a dataset directly to Imaris from an ImagePlus
 * @author Olivier Burri
 * January 2021
 * EPFL - SV - PTECH - PTBIOP
 */
public class CreateNewDataset {
    public static void main(String[] args) throws Error {
        EasyXT.main(args);
        ImagePlus imp = IJ.openImage("https://imagej.net/images/confocal-series.zip");

        // This creates the dataset from the given ImagePlus
        IDataSetPrx dataset = EasyXT.Dataset.create(imp);
        EasyXT.Dataset.setCurrent(dataset);
    }
}
