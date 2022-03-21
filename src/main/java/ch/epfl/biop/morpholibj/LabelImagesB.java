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
package ch.epfl.biop.morpholibj;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.label.LabelImages;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LabelImagesB
{
    /**
     * Private constructor to prevent class instantiation.
     */
    protected LabelImagesB()
    {
    }

public static final ImagePlus cropLabel(ImagePlus imagePlus, int label, int border, boolean withCalibration)
        {
        String newName = imagePlus.getShortTitle() + "-crop";
        ImagePlus croppedPlus;
        Calibration cal = (Calibration) imagePlus.getCalibration().clone();

        // Compute the cropped image
        if (imagePlus.getStackSize() == 1)
        {
        if (!withCalibration)
        {
        ImageProcessor image = imagePlus.getProcessor();
        ImageProcessor cropped = LabelImages.cropLabel(image, label, border);
        croppedPlus = new ImagePlus(newName, cropped);
        } else {
        ImageProcessor image = imagePlus.getProcessor();
        croppedPlus = LabelImagesB.cropLabel(image, label, border, cal, newName);
        }
        }
        else
        {
        if (!withCalibration)
        {
        ImageStack image = imagePlus.getStack();
        ImageStack cropped = LabelImages.cropLabel(image, label, border);
        croppedPlus = new ImagePlus(newName, cropped);
        } else {
        ImageStack image = imagePlus.getStack();
        croppedPlus = LabelImagesB.cropLabel(image, label, border, cal, newName);
        }
        }

        return croppedPlus;
        }


/**
 * Returns a binary image that contains only the selected particle or
 * region, by automatically cropping the image and eventually adding some
 * borders.
 *
 * @param image a, image containing label of particles
 * @param label the label of the particle to select
 * @param border the number of pixels to add to each side of the particle
 * @return a smaller binary image containing only the selected particle
 */
public static final ImagePlus cropLabel(ImageProcessor image, int label, int border , Calibration cal , String newName)
        {
        // image size
        int sizeX = image.getWidth();
        int sizeY = image.getHeight();

        // Initialize label bounds
        int xmin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;

        // update bounds by iterating on voxels
        for (int y = 0; y < sizeY; y++)
        {
        for (int x = 0; x < sizeX; x++)
        {
        // process only specified label
        int val = image.get(x, y);
        if (val != label)
        {
        continue;
        }

        // update bounds of current label
        xmin = min(xmin, x);
        xmax = max(xmax, x);
        ymin = min(ymin, y);
        ymax = max(ymax, y);
        }
        }

        // Compute size of result, taking into account border
        int sizeX2 = (xmax - xmin + 1 + 2 * border);
        int sizeY2 = (ymax - ymin + 1 + 2 * border);

        // allocate memory for result image
        ImageProcessor result = new ByteProcessor(sizeX2, sizeY2);

        // fill result with binary label
        for (int y = ymin, y2 = border; y <= ymax; y++, y2++)
        {
        for (int x = xmin, x2 = border; x <= xmax; x++, x2++)
        {
        if ((image.get(x, y)) == label)
        {
        result.set(x2, y2, 255);
        }
        }
        }
        ImagePlus croppedPlus = new ImagePlus(newName, result);
        cal.xOrigin += ( xmin - border ) * cal.pixelWidth;
        cal.yOrigin += ( ymin - border ) * cal.pixelHeight;
        croppedPlus.setCalibration( cal );
        return croppedPlus;
        }



/**
 * Returns a binary image that contains only the selected particle or
 * region, by automatically cropping the image and eventually adding some
 * borders.
 *
 * @param image a 3D image containing label of particles
 * @param label the label of the particle to select
 * @param border the number of voxels to add to each side of the particle
 * @return a smaller binary image containing only the selected particle
 */
public static final ImagePlus cropLabel(ImageStack image, int label, int border, Calibration cal , String newName)
        {
        // image size
        int sizeX = image.getWidth();
        int sizeY = image.getHeight();
        int sizeZ = image.getSize();

        // Initialize label bounds
        int xmin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;
        int zmin = Integer.MAX_VALUE;
        int zmax = Integer.MIN_VALUE;

        // update bounds by iterating on voxels
        for (int z = 0; z < sizeZ; z++)
        {
        for (int y = 0; y < sizeY; y++)
        {
        for (int x = 0; x < sizeX; x++)
        {
        // process only specified label
        int val = (int) image.getVoxel(x, y, z);
        if (val != label)
        {
        continue;
        }

        // update bounds of current label
        xmin = min(xmin, x);
        xmax = max(xmax, x);
        ymin = min(ymin, y);
        ymax = max(ymax, y);
        zmin = min(zmin, z);
        zmax = max(zmax, z);
        }
        }
        }

        // Compute size of result, taking into account border
        int sizeX2 = (xmax - xmin + 1 + 2 * border);
        int sizeY2 = (ymax - ymin + 1 + 2 * border);
        int sizeZ2 = (zmax - zmin + 1 + 2 * border);

        // allocate memory for result image
        ImageStack result = ImageStack.create(sizeX2, sizeY2, sizeZ2, 8);

        // fill result with binary label
        for (int z = zmin, z2 = border; z <= zmax; z++, z2++)
        {
        for (int y = ymin, y2 = border; y <= ymax; y++, y2++)
        {
        for (int x = xmin, x2 = border; x <= xmax; x++, x2++)
        {
        if (((int) image.getVoxel(x, y, z)) == label)
        {
        result.setVoxel(x2, y2, z2, 255);
        }
        }
        }
        }

        ImagePlus croppedPlus = new ImagePlus(newName, result);
        cal.xOrigin += ( xmin - border ) * cal.pixelWidth;
        cal.yOrigin += ( ymin - border ) * cal.pixelHeight;
        cal.zOrigin += ( zmin - border ) * cal.pixelDepth;
        croppedPlus.setCalibration( cal );
        return croppedPlus;

        }
}
