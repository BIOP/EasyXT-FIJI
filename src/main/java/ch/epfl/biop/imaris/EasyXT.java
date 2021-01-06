/**
 * Copyright (c) 2020 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.epfl.biop.imaris;


import Ice.ObjectPrx;
import Imaris.Error;
import Imaris.*;
import ImarisServer.IServerPrx;
import com.bitplane.xt.IceClient;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.Concatenator;
import ij.process.*;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;

import mcib3d.geom.*;
import net.imagej.legacy.translate.ImagePlusCreatorUtils;

// TODO: Detecting Spots and Surfaces, with tracking

// TODO: Convert spots to channel

// TODO: Mask Channel using spots or surfaces

// TODO: Find way to batch process OpenImage() with doc

// TODO: Make Documentation

/**
 * Main EasyXT Static class
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * @author Romain Guiet
 * <p>
 * This is the main static class you should access when you want to interact with Imaris.
 */
public class EasyXT {
    private static final IApplicationPrx app;
    private static IceClient mIceClient;
    public static Map<tType, Integer> datatype;
    private static final String mEndPoints = "default -p 4029";

    /**
     * Standard logger
     */
    private static final Consumer<String> log = (str) -> System.out.println("EasyXT : " + str);

    /**
     * Error logger
     */
    private static final Consumer<String> errlog = (str) -> System.err.println("EasyXT : " + str);

    /**
     * Static initialisation :
     * Gets the Imaris server
     * Ensure the connection is closed on JVM closing
     */
    static {
        log.accept("Initializing EasyXT");
        // Populate static map : Imaris Type -> Bit Depth
        Map<tType, Integer> tmap = new HashMap<>(4);
        tmap.put(tType.eTypeUInt8, 8);    // Unsigned integer, 8  bits : 0..255
        tmap.put(tType.eTypeUInt16, 16);  // Unsigned integer, 16 bits : 0..65535
        tmap.put(tType.eTypeFloat, 32);   // Float 32 bits
        tmap.put(tType.eTypeUnknown, -1); // ? Something else

        datatype = Collections.unmodifiableMap(tmap);

        mIceClient = new IceClient("ImarisServer", mEndPoints, 10000);
        app = getImaris(mIceClient.GetServer(), 0);

        // Closing connection on jvm close
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    log.accept("Closing ICE Connection from Imaris...");
                    CloseIceClient();
                    log.accept("Done.");
                })

        );
        log.accept("Initialization Done. Ready to call EasyXT");
    }

    // TODO Refactor & Comment
    private static void CloseIceClient() {
        if (mIceClient != null) {
            mIceClient.Terminate();
            mIceClient = null;
        }
    }

    /**
     * casts each Imaris Object to its right Class for easier downstream processing Not sure if this is needed but have
     * not tested without
     *
     * @param item the item to return the specific class of
     * @return the same item but cast to its appropriate subclass
     * @throws Error an Imaris Error Object
     */
    static IDataItemPrx castToType(ObjectPrx item) throws Error {
        IFactoryPrx factory = app.GetFactory();

        if (factory.IsSpots(item)) {
            return factory.ToSpots(item);
        }
        if (factory.IsSurfaces(item)) {
            return factory.ToSurfaces(item);
        }
        if (factory.IsVolume(item)) {
            return factory.ToVolume(item);
        }
        if (factory.IsLightSource(item)) {
            return factory.ToLightSource(item);
        }
        if (factory.IsFrame(item)) {
            return factory.ToFrame(item);
        }
        if (factory.IsDataContainer(item)) {
            return factory.ToDataContainer(item);
        }

        return null;
    }

    // TODO Refactor & Comment
    private static IApplicationPrx getImaris(IServerPrx server, int var1) {
        ObjectPrx var2 = server.GetObject(var1);
        return IApplicationPrxHelper.checkedCast(var2);
    }

    /**
     * Returns instance of Imaris App
     *
     * @return
     */
    public static IApplicationPrx getImaris() {
        return app;
    }

    /**
     * Return the first item with the selected name. Returns null if not found
     *
     * @param name the name of the item in the Imaris Scene
     * @return the requested item, null if not found
     * @throws Error an Imaris Error Object
     */
    public static IDataItemPrx getItem(String name) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name)
                .build();

        List<IDataItemPrx> items = query.get();

        if (items.size() > 0)
            return items.get(0);

        log.accept("No Items with name " + name + " found inside " + getName(query.getParent()));
        return null;
    }

    /**
     * Returns nth item of a given type
     *
     * @param type     its type, as defined by the enum {@link ItemQuery.ItemType}
     * @param position the 0-based position of the object
     * @return the requested item, null if not found
     * @throws Error an Imaris Error Object
     */
    public static IDataItemPrx getItem(String type, int position) throws Error {

        ItemQuery query = new ItemQuery.ItemQueryBuilder().setPosition(position).setType(type).build();
        List<IDataItemPrx> items = query.get();

        if (items.size() >= query.getPosition())
            return items.get(query.getPosition());

        log.accept("No Items of type " + type + " found at position " + position + " inside " + getName(query.getParent()));
        return null;
    }


    /**
     * Get the first spots object with the given name
     *
     * @param name the name of the spots object to get. Returns the first spots object if there are multiple spots with
     *             the same name (Don't do that)
     * @return the requested spots, null if not found
     * @throws Error an Imaris Error Object
     */
    public static ISpotsPrx getSpots(String name) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setType("Spots").build();
        List<IDataItemPrx> items = query.get();
        if (items.size() > 0) return (ISpotsPrx) items.get(0);
        return null;
    }

    /**
     * Get the n-th spots object in the scene
     *
     * @param position the 0-based position of the spots. a value of 2 would try to return the 3rd spots object
     * @return the requested spots, null if not found
     * @throws Error an Imaris Error Object
     */
    public static ISpotsPrx getSpots(int position) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("Spots").setPosition(position).build();
        List<IDataItemPrx> items = query.get();
        if (items.size() > 0) return (ISpotsPrx) items.get(0);
        return null;
    }

    /**
     * Get the first surfaces object with the given name
     *
     * @param name the name of the surfaces object to get. Returns the first surfaces object if there are multiple
     *             surfaces with the same name (Don't do that)
     * @return the requested surfaces, null if not found
     * @throws Error an Imaris Error Object
     */
    public static ISurfacesPrx getSurfaces(String name) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setName(name).setType("Surfaces").build();

        List<IDataItemPrx> items = query.get();
        if (items.size() > 0) return (ISurfacesPrx) items.get(0);
        return null;
    }

    /**
     * Get the first surfaces object with the given name
     *
     * @param position the 0-based position of the surfaces. a value of 2 would try to return the 3rd surfaces object
     * @return the requested surfaces, null if not found
     * @throws Error an Imaris Error Object
     */
    public static ISurfacesPrx getSurfaces(int position) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("Surfaces").setPosition(position).build();
        List<IDataItemPrx> items = query.get();
        if (items.size() > 0) return (ISurfacesPrx) items.get(0);
        return null;
    }

    /**
     * Get all items of the requested type in the main scene as a list (not within subfolder, groups)
     *
     * @param type the type, defined by a String. See {@link ItemQuery.ItemType}
     * @return a list containins the objects
     * @throws Error an Imaris Error Object
     */
    public static List<IDataItemPrx> getAll(String type) throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType(type).build();
        return query.get();
    }

    /**
     * Get all spots objects in the main scene as a list (not within subfolder, groups)
     *
     * @return the spots as a list
     * @throws Error an Imaris Error Object
     */
    public static List<ISpotsPrx> getAllSpots() throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("Spots").build();
        List<IDataItemPrx> items = query.get();

        // Explicitly cast
        List<ISpotsPrx> spots = items.stream().map(item -> {
            return (ISpotsPrx) item;
        }).collect(Collectors.toList());

        return spots;
    }

    /**
     * Get all surfaces objects in the main scene as a list (not within subfolder, groups)
     *
     * @return the surfaces as a list
     * @throws Error an Imaris Error Object
     */
    public static List<ISurfacesPrx> getAllSurfaces() throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("Surfaces").build();
        List<IDataItemPrx> items = query.get();

        // Explicitly cast
        List<ISurfacesPrx> surfs = items.stream().map(item -> {
            return (ISurfacesPrx) item;
        }).collect(Collectors.toList());

        return surfs;
    }

    /**
     * Get all Group objects in the main scene as a list
     *
     * @return the surfaces as a list
     * @throws Error an Imaris Error Object
     */
    public static List<IDataContainerPrx> getAllGroups() throws Error {
        ItemQuery query = new ItemQuery.ItemQueryBuilder().setType("DataContainer").build();
        List<IDataItemPrx> items = query.get();

        // Explicitly cast
        List<IDataContainerPrx> groups = items.stream().map(item -> {
            return (IDataContainerPrx) item;
        }).collect(Collectors.toList());

        return groups;
    }

    // ImagePlus Manipulations

    /**
     * Returns an ImagePlus image of a dataset TODO : add a way to select only a subpart of it
     *
     * @param dataset
     * @return
     * @throws Error
     */
    public static ImagePlus getImagePlus(IDataSetPrx dataset) throws Error {

        int nc = dataset.GetSizeC();
        int nz = dataset.GetSizeZ();
        int nt = dataset.GetSizeT();

        int w = dataset.GetSizeX();
        int h = dataset.GetSizeY();


        ImarisCalibration cal = new ImarisCalibration(dataset);
        int bitdepth = getBitDepth(dataset);

        ImageStack stack = ImageStack.create(w, h, nc * nz * nt, bitdepth);
        ImagePlus imp = new ImagePlus(app.GetCurrentFileName(), stack);
        imp.setDimensions(nc, nz, nt);

        for (int c = 0; c < nc; c++) {
            for (int z = 0; z < nz; z++) {
                for (int t = 0; t < nt; t++) {
                    int idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor ip;
                    switch (bitdepth) {
                        case 8:
                            byte[] datab = dataset.GetDataSubVolumeAs1DArrayBytes(0, 0, z, c, t, w, h, 1);
                            ip = new ByteProcessor(w, h, datab, null);
                            stack.setProcessor(ip, idx);
                            break;
                        case 16:
                            short[] datas = dataset.GetDataSubVolumeAs1DArrayShorts(0, 0, z, c, t, w, h, 1);
                            ip = new ShortProcessor(w, h, datas, null);
                            stack.setProcessor(ip, idx);
                            break;
                        case 32:
                            float[] dataf = dataset.GetDataSubVolumeAs1DArrayFloats(0, 0, z, c, t, w, h, 1);
                            ip = new FloatProcessor(w, h, dataf, null);
                            stack.setProcessor(ip, idx);
                            break;
                    }
                }
            }
        }
        imp.setStack(stack);
        imp.setCalibration(cal);
        imp = HyperStackConverter.toHyperStack(imp, nc, nz, nt);

        // Set LookUpTables
        if (imp instanceof CompositeImage) {

            LUT[] luts = new LUT[nc];

            for (int c = 0; c < nc; c++) {
                Color color = EasyXT.getColorFromInt(dataset.GetChannelColorRGBA(c));
                luts[c] = LUT.createLutFromColor(color);
            }
            // TODO : transfer min max
            ((CompositeImage) imp).setLuts(luts);
        } else if (nc == 1) {
            // TODO : transfer min max
            imp.setLut(LUT.createLutFromColor(EasyXT.getColorFromInt(dataset.GetChannelColorRGBA(0))));
        }

        return imp;

    }

    /**
     * Set data from an ImagePlus image into a dataset TODO : add a way to select only a subpart of it
     *
     * @param dataset the dataset to insert the imagePlus into
     * @throws Error an Imaris Error Object
     */
    public static void setImagePlus(IDataSetPrx dataset, ImagePlus imp) throws Error {

        int nc = dataset.GetSizeC();
        int nz = dataset.GetSizeZ();
        int nt = dataset.GetSizeT();

        int w = dataset.GetSizeX();
        int h = dataset.GetSizeY();


        ImarisCalibration cal = new ImarisCalibration(dataset);
        int bitdepth = getBitDepth(dataset);

        for (int c = 0; c < nc; c++) {
            for (int z = 0; z < nz; z++) {
                for (int t = 0; t < nt; t++) {
                    int idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor ip = imp.getStack().getProcessor(idx);
                    switch (bitdepth) {
                        case 8:
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

    /**
     * Convenience method to return the currently active dataset
     *
     * @return a IDatasetPrx object containing a reference to all the pixel data
     * @throws Error an Imaris Error Object
     */
    public static IDataSetPrx getCurrentDataset() throws Error {
        return EasyXT.getImaris().GetDataSet();
    }

    /**
     * Convenience method to set/replace the current dataset within the current Imaris scene with the one provided
     *
     * @param dataset a IDataSetPrx object containing a reference to all the pixel data
     * @throws Error an Imaris Error Object
     */
    public static void setCurrentDataset(IDataSetPrx dataset) throws Error {
        EasyXT.getImaris().SetDataSet(dataset);
    }

    /**
     * Adds the selected ImagePlus to the current Dataset by appending it as new channels If the dataset is visible in
     * the Imaris Scene, this is a lot slower
     *
     * @param imp the image to add to the current dataset
     * @throws Error an Imaris Error object
     */
    public static void addChannels(ImagePlus imp) throws Error {

        // Ensure that the image is not larger than the dataset
        IDataSetPrx dataset = EasyXT.getCurrentDataset();

        addChannels(dataset, imp, 0, 0, 0, 0);
    }

    /**
     * Adds the selected ImagePlus to the provided IDatasetPrx by appending it as new channels
     *
     * @param dataset
     * @param imp     the image to add to the current dataset
     * @throws Error an Imaris Error object
     */
    public static void addChannels(IDataSetPrx dataset, ImagePlus imp) throws Error {
        addChannels(dataset, imp, 0, 0, 0, 0);
    }

    /**
     * <p>Adds the selected ImagePlus to the provided IDatasetPrx by appending each channel of the ImagePlus as new
     * channels into the provided IDataSetPrx, at a given starting location in XYCZT </p>
     * <p>Sanity checks performed:</p>
     * <ul>
     *     <li>Ensure ImagePlus dimensions are not larger than the provided dataset dimensions (including provided starting locations). Throws an Error if larger than the dataset. </li>
     *     <li>Ensure consistent bit-depth between ImagePlus and IDataSetPrx. throws Error if otherwise</li>
     *     <li>Ensure consistent voxel size (ignore framerate) between ImagePlus and Dataset. Issues warning if otherwise</li>
     * </ul>
     * The user can define the start location XYZT in pixel coordinates
     *
     * @param imp     the image from which to extract the channels to append
     * @param dataset the receiver dataset
     * @param xstart  start X position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
     * @param ystart  start Y position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
     * @param zstart  start Z position, in pixels (Z=0 is the top slice in Image, will translate to bottom slice in
     *                Imaris)
     * @param tstart  start T position, in pixels (from top-left in ImageJ, will translate to bottom-left in Imaris)
     * @throws Error an Imaris Error Object
     */
    public static void addChannels(IDataSetPrx dataset, ImagePlus imp, int xstart, int ystart, int zstart, int tstart) throws Error {

        // Get Metadata on dataset and image
        ImarisCalibration dCal = new ImarisCalibration(dataset);
        int dc = dataset.GetSizeC();

        Calibration iCal = imp.getCalibration();
        int iw = imp.getWidth();
        int ih = imp.getHeight();
        int iz = imp.getNSlices();
        int it = imp.getNFrames();
        int ic = imp.getNChannels();

        int dBitDepth = getBitDepth(dataset);
        int iBitDepth = imp.getBitDepth();

        if (!(dCal.xSize >= (xstart + iw) &&
                dCal.ySize >= (ystart + ih) &&
                dCal.zSize >= (zstart + iz) &&
                dCal.tSize >= (tstart + it))) {

            String errorDetail = "Dataset\t(X,\tY,\tZ,\tT):\t (" + dCal.xSize + ",\t" + dCal.ySize + ",\t" + dCal.zSize + ",\t" + dCal.tSize + ")";
            errorDetail += "\nImage\t(X,\tY,\tZ,\tT):\t (" + iw + ",\t" + ih + ",\t" + iz + ",\t" + it + ")";
            errorDetail += "\nIncl. offset\t(X,\tY,\tZ,\tT):\t (" + (iw + xstart) + ",\t" + (ih + ystart) + ",\t" + (iz + zstart) + ",\t" + (it + tstart) + ")";

            throw new Error("Size Mismatch", "Dataset and ImagePlus do not have the same size in XYZT", errorDetail);
        }

        if (dBitDepth != iBitDepth) {
            String errorDetail = "   Dataset:" + dBitDepth + "-bit";
            errorDetail += "\nImage:" + iBitDepth + "-bit";

            throw new Error("Bit Depth Mismatch", "Dataset and ImagePlus do not have same bit depth", errorDetail);
        }

        // Issue warning in case voxel sizes do not match
        if (dCal.pixelDepth != iCal.pixelDepth &&
                dCal.pixelHeight != iCal.pixelHeight &&
                dCal.pixelWidth != iCal.pixelWidth) {

            log.accept("Warning: Voxel Sizes between Dataset and ImagePlus do not match:");
            log.accept("   Dataset Voxel Size\t(X,\tY,\tZ):\t" + dCal.pixelWidth + ",\t" + dCal.pixelHeight + ",\t" + dCal.pixelDepth + ")");
            log.accept("   Image Voxel Size\t(X,\tY,\tZ):\t" + iCal.pixelWidth + ",\t" + iCal.pixelHeight + ",\t" + iCal.pixelDepth + ")");


        }

        // Enlarge the dataset by setting its size to the cumulated number of channels
        dataset.SetSizeC(dc + ic);

        // Now loop through the dimensions of the ImagePlus to add data
        for (int c = 0; c < ic; c++) {
            int idx = imp.getStackIndex(c + 1, 1, 1);
            int color = imp.getStack().getProcessor(idx).getColorModel().getRGB(255);

            dataset.SetChannelColorRGBA(c, color);

            for (int z = 0; z < iz; z++) {
                for (int t = 0; t < it; t++) {
                    idx = imp.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor ip = imp.getStack().getProcessor(idx);
                    switch (dBitDepth) {
                        case 8:
                            dataset.SetDataSubVolumeAs1DArrayBytes(((byte[]) ip.getPixels()), xstart, ystart, zstart + z, c + dc, tstart + t, iw, ih, 1); // last element is sizeZ (one slice at a time)
                            break;
                        case 16:
                            dataset.SetDataSubVolumeAs1DArrayShorts((short[]) ip.getPixels(), xstart, ystart, zstart + z, c + dc, tstart + t, iw, ih, 1);
                            break;
                        case 32:
                            dataset.SetDataSubVolumeAs1DArrayFloats((float[]) ip.getPixels(), xstart, ystart, zstart + z, c + dc, tstart + t, iw, ih, 1);
                            break;
                    }
                }
            }
        }
    }

    // Scene Related methods

    /**
     * Adds the provided Item to the Main Imaris Scene (at the bottom)
     *
     * @param item the item (Spot, Surface, Folder) to add
     * @throws Error an Imaris Error Object
     */
    public static void addToScene(IDataItemPrx item) throws Error {
        addToScene(getImaris().GetSurpassScene(), item);
    }

    /**
     * Adds the provided item as the last child to the provided parent item
     *
     * @param parent The parent item
     * @param item   the item to add as a child
     * @throws Error an Imaris Error Object
     */
    public static void addToScene(IDataContainerPrx parent, IDataItemPrx item) throws Error {
        parent.AddChild(item, -1); // last element is position. -1 to append at the end.
    }

    /**
     * Removes the provided item from its parent,
     * if it's a Group removes the children spots & surfaces
     *
     * @param item the item in question
     * @throws Error an Imaris Error Object
     */
    public static void removeFromScene(IDataItemPrx item) throws Error {
        // if the item is a group
        IFactoryPrx factory = app.GetFactory();
        if (factory.IsDataContainer(item)) {
            IDataContainerPrx group = factory.ToDataContainer(item);
            // make sure to remove all elements in it
            for (int grp = 0; grp < group.GetNumberOfChildren(); grp++) {
                removeFromScene(group.GetChild(grp));
            }
        }
        // remove the item
        item.GetParent().RemoveChild(item);

    }

    /**
     * Removes the provided List of items from its parent
     *
     * @param items, the list of items in question
     * @throws Error an Imaris Error Object
     */
    public static void removeFromScene(List<? extends IDataItemPrx> items) throws Error {
        for (IDataItemPrx it : items) {
            removeFromScene(it);
        }
    }

    /**
     * Reset the Imaris Scene
     *
     * @throws Error an Imaris Error Object
     */
    public static void resetScene() throws Error {
        List<ISpotsPrx> spots = EasyXT.getAllSpots();
        List<ISurfacesPrx> surfaces = EasyXT.getAllSurfaces();
        List<IDataContainerPrx> groups = EasyXT.getAllGroups();
        EasyXT.removeFromScene(spots);
        EasyXT.removeFromScene(surfaces);
        EasyXT.removeFromScene(groups);
        //for ( ISpotsPrx sp: spots ) { EasyXT.removeFromScene( sp ); }
        //for ( ISurfacesPrx srf: surfaces ) { EasyXT.removeFromScene( srf ); }
        //for ( IDataContainerPrx grp: groups ) { EasyXT.removeFromScene( grp ); }

        // TODO selectScene
    }

    /**
     * Creates a "Group" (folder) that can contain other items
     *
     * @param groupName the name to identify the group with
     * @return an item that can be added to a scene ({@link EasyXT#addToScene(IDataItemPrx)}) or to which other items
     * can be added as children {@link EasyXT#addToScene(IDataContainerPrx, IDataItemPrx)}
     * IDataContainerPrx extends IDataItemPrx
     * @throws Error
     */
    public static IDataContainerPrx createGroup(String groupName) throws Error {
        IDataContainerPrx group = app.GetFactory().CreateDataContainer();
        group.SetName(groupName);
        return group;
    }

    //Surface Related methods

    // TODO Comment
    public static IDataSetPrx getSurfacesDataset(ISurfacesPrx surface) throws Error {
        // Check if there are channels
        ImarisCalibration cal = new ImarisCalibration(app.GetDataSet());

        IDataSetPrx final_dataset = app.GetDataSet().Clone();
        final_dataset.SetSizeC(1);
        final_dataset.SetType(tType.eTypeUInt8);

        // Loop through each timepoint, and get the dataset, then replace
        for (int t = 0; t < cal.tSize; t++) {
            IDataSetPrx one_timepoint = getSurfacesDataset(surface, 1.0, t);
            final_dataset.SetDataVolumeAs1DArrayBytes(one_timepoint.GetDataVolumeAs1DArrayBytes(0, 0), 0, t);
        }

        return final_dataset;
    }

    // TODO Comment
    public static ImagePlus getSurfacesMask(ISurfacesPrx surface) throws Error {

        // Get raw ImagePlus
        ImagePlus impSurface = getImagePlus(getSurfacesDataset(surface));

        // Multiply by 255 to allow to use ImageJ binary functions
        int nProcessor = impSurface.getStack().getSize();
        IntStream.range(0, nProcessor).parallel().forEach(index -> {
            impSurface.getStack().getProcessor(index + 1).multiply(255);
        });

        // Set LUT and display range
        impSurface.setLut(LUT.createLutFromColor(EasyXT.getColorFromInt(surface.GetColorRGBA())));
        impSurface.setDisplayRange(0, 255);
        impSurface.setTitle(surface.GetName());

        return impSurface;
    }

    // TODO Comment
    public static void setSurfacesMask(ISurfacesPrx surface, ImagePlus imp) throws Error {
        ImarisCalibration cal = new ImarisCalibration(app.GetDataSet());

        // Divide by 255 to allow to use ImageJ binary functions
        int nProcessor = imp.getStack().getSize();
        IntStream.range(0, nProcessor).parallel().forEach(index -> {
            imp.getStack().getProcessor(index + 1).multiply(1.0 / 255.0);
        });

        surface.RemoveAllSurfaces();

        for (int t = 0 ; t < cal.tSize ; t++){
            IDataSetPrx dataset = getSurfacesDataset(surface, 1, t);
            dataset.SetType(tType.eTypeUInt8);
            // temporary ImagePlus required to work!
            ImagePlus t_imp = new Duplicator().run(imp , 1,1,1,cal.zSize,t+1,t+1 );
            //t_imp.show() ;
            setImagePlus(dataset, t_imp );

            surface.AddSurface(dataset, t);
        }

    }

    // TODO Comment
    public static ImagePlus getSurfacesMask(ISurfacesPrx surface, int timepoint) throws Error {
        return getSurfacesMask(surface, 1.0, timepoint);
    }

    // TODO Comment
    public static ImagePlus getSurfacesMask(ISurfacesPrx surface, double downsample, int timepoint) throws Error {

        ImarisCalibration cal = new ImarisCalibration(app.GetDataSet()).getDownsampled(downsample);

        IDataSetPrx data = getSurfacesDataset(surface , downsample , timepoint );

        ImagePlus imp = getImagePlus(data);
        imp.setCalibration(cal);

        return imp;
    }

    // TODO Comment
    public static IDataSetPrx getSurfacesDataset(ISurfacesPrx surface, double downsample, int timepoint) throws Error {
        ImarisCalibration cal = new ImarisCalibration(app.GetDataSet()).getDownsampled(downsample);

        IDataSetPrx data = surface.GetMask((float) cal.xOrigin, (float) cal.yOrigin, (float) cal.zOrigin,
                (float) cal.xEnd, (float) cal.yEnd, (float) cal.zEnd,
                cal.xSize, cal.ySize, cal.zSize, timepoint);
        return data;
    }

    /**
     * Get an ImagePlus of the spots as a mask (255)
     *
     * @param spots , a spots object see {@link  #getSpots(String)}
     * @return      , an ImagePlus
     * @throws Error
     */
    public static ImagePlus getSpotsMask(ISpotsPrx spots ) throws Error {
        return getSpotsImage( spots,  false, false);
    }

    // TODO Comment
    public static ImagePlus getSpotsMLabel(ISpotsPrx spots ) throws Error {
        return getSpotsImage( spots,  true, false);
    }

    // TODO Comment
    public static ImagePlus getSpotsImage(ISpotsPrx spots, boolean isValueID, boolean isGauss) throws Error {
        // Prepare the imp to receive spots pixels
        //
        // get the calibration info from Imaris
        ImarisCalibration cal = new ImarisCalibration(app.GetDataSet());
        // Create a new imp with the cal and get stack ()
        //ImagePlus imp = IJ.createHyperStack( getOpenImageName(), cal.xSize , cal.ySize, cal.cSize, cal.zSize, cal.tSize , 32);
        //imp.setCalibration( cal );
        //ImageStack stack = imp.getStack();

        long[] spots_ids = spots.GetIds();
        float[][] spots_centerXYZ = spots.GetPositionsXYZ();
        float[][] spots_radiiXYZ = spots.GetRadiiXYZ();
        int[] spots_t = spots.GetIndicesT();

        double resXY = cal.pixelWidth;
        double resZ = cal.pixelDepth;
        String unit = cal.getUnit();

        // Define default vectors
        Vector3D V = new Vector3D(1, 0, 0);
        Vector3D W = new Vector3D(0, 1, 0);
        if (Math.abs(V.dotProduct(W)) > 0.001) {
            IJ.log("ERROR : vectors should be perpendicular");
        }

        int previous_t = spots_t[0];
        ObjectCreator3D obj = new ObjectCreator3D(cal.xSize, cal.ySize, cal.zSize);
        obj.setResolution(cal.pixelWidth, cal.pixelDepth, cal.getUnit());


        ArrayList<ImagePlus> imps = new ArrayList<ImagePlus>(spots_t[spots_t.length - 1]);

        for (int t = 0; t < spots_t.length; t++) {
            // if the current spot is from a different time-point
            if (spots_t[t] != previous_t) {
                // store the current status into an ImagePlus
                // N.B. duplicate is required to store the current time-point
                imps.add(new ImagePlus("t" + previous_t, obj.getStack().duplicate()));
                // and reset the obj
                obj.reset();
            }
            // by default the value is 255
            int val = 255;
            // but if isValueID is true, use the ID number for the value
            if (isValueID) val = (int) spots_ids[t];
            // add an ellipsoid to obj
            obj.createEllipsoidAxesUnit(spots_centerXYZ[t][0], spots_centerXYZ[t][1], spots_centerXYZ[t][2], spots_radiiXYZ[t][0], spots_radiiXYZ[t][1], spots_radiiXYZ[t][2], (float) val, V, W, isGauss);
            // set the previous_t
            previous_t = spots_t[t];

        }

        // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
        ImagePlus[] impsA = imps.toArray(new ImagePlus[0]);
        ImagePlus final_imp = Concatenator.run(impsA);
        final_imp.setDisplayRange(0, final_imp.getDisplayRangeMax());
        final_imp.setTitle(getOpenImageName());
        final_imp.setCalibration(cal);

        return final_imp;

    }

    // Image Management Methods

    /*
     * openImage, opens the file from filepath in a new imaris scene
     * @param filepath path  to an *.ims file
     * @param options option string cf : xtinterface/structImaris_1_1IApplication.html/FileOpen
     * @throws Error
     */
    public static void openImage(File filepath, String options) throws Error {
        if (!filepath.exists()) {
            errlog.accept(filepath + "doesn't exist");
            return;
        }

        if (!filepath.isFile()) {
            errlog.accept(filepath + "is not a file");
            return;
        }

        if (!filepath.getName().endsWith("ims")) {
            errlog.accept(filepath + "is not an imaris file, please convert your image first");
            return;
        }

        app.FileOpen(filepath.getAbsolutePath(), options);

    }

    /**
     * overloaded method , see {@link #openImage(File, String)}
     *
     * @param filepath to an *.ims file
     * @throws Error
     */
    public static void openImage(File filepath) throws Error {
        openImage(filepath, "");
    }

    /**
     * Saves the current imaris scene to an imaris file
     *
     * @param filepath path to save ims file
     * @param options  option string cf : xtinterface/structImaris_1_1IApplication.html/FileSave eg writer="BMPSeries".
     *                 List of formats available: Imaris5, Imaris3, Imaris2,SeriesAdjustable, TiffSeriesRGBA, ICS,
     *                 OlympusCellR, OmeXml, BMPSeries, MovieFromSlices.
     * @throws Error
     */
    public static void saveImage(File filepath, String options) throws Error {
        if (!filepath.getName().endsWith("ims")) {
            filepath = new File(filepath.getAbsoluteFile() + ".ims");
            System.out.println("Saved as : " + filepath.getAbsoluteFile());
        }
        app.FileSave(filepath.getAbsolutePath(), options);

    }

    /**
     * overloaded method , see {@link #saveImage(File, String)}
     *
     * @param filepath path to save ims file
     * @throws Error
     */
    public static void saveImage(File filepath) throws Error {

        saveImage(filepath, "");

    }

    // Statistics related functions

    /**
     * returns all available Imaris statistics from the selected item
     *
     * @param item the item to query
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item) throws Error {
        return new StatsQuery(item).get();
    }

    /**
     * returns the selected statistic from the selected item
     *
     * @param item the item to query
     * @param name the name of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item, String name) throws Error {
        return new StatsQuery(item)
                .selectStatistic(name)
                .get();
    }

    /**
     * returns only the selected statistic at the selected channel Careful. Imaris results are one-based for channels
     *
     * @param item    the item to query
     * @param name    the name of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
     * @param channel the channel for which we want the statistic
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item, String name, Integer channel) throws Error {
        return new StatsQuery(item)
                .selectStatistic(name)
                .selectChannel(channel)
                .get();
    }

    /**
     * returns the selected statistics
     *
     * @param item  the item to query
     * @param names the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item, List<String> names) throws Error {
        return new StatsQuery(item)
                .selectStatistics(names)
                .get();
    }

    /**
     * returns the selected statistics at the selected channel Careful. Imaris results are one-based for channels
     *
     * @param item    the item to query
     * @param names   the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
     * @param channel the channel for which we want the statistic
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item, List<String> names, Integer channel) throws Error {
        return new StatsQuery(item)
                .selectStatistics(names)
                .selectChannel(channel)
                .get();
    }

    /**
     * returns the selected statistics at the selected channels Careful. Imaris results are one-based for channels
     *
     * @param item     the item to query
     * @param names    the names of the statistic, as in the IJ GUI minus the "Ch=1 Img=1", ... text
     * @param channels the channels for which we want the statistic
     * @return a ResultsTable to be used and displayed by ImageJ
     * @throws Error an Imaris Error Object
     */
    public static ResultsTable getStatistics(IDataItemPrx item, List<String> names, List<Integer> channels) throws Error {
        return new StatsQuery(item)
                .selectStatistics(names)
                .selectChannels(channels)
                .get();
    }

    /**
     * Recover a Color for use to set ImagePlus LUTs
     *
     * @param color the Imaris Color descriptor
     * @return the Java Color
     */
    public static Color getColorFromInt(int color) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(color).array();
        int[] colorArray = new int[3];
        colorArray[0] = bytes[3] & 0xFF;
        colorArray[1] = bytes[2] & 0xFF;
        colorArray[2] = bytes[1] & 0xFF;
        return getColorIntFromIntArray(colorArray);
    }

    /**
     * recover an RGB color for use with ImageJ from a 3 element (R,G,B) array
     *
     * @param color the Java Color
     * @return
     */
    public static Color getColorIntFromIntArray(int[] color) {
        return new Color(color[0], color[1], color[2]);
    }

    /**
     * Returns bitdepth of a dataset. See {@link EasyXT#datatype}
     *
     * @param dataset
     * @return
     * @throws Error an Imaris Error Object
     */
    public static int getBitDepth(IDataSetPrx dataset) throws Error {
        tType type = dataset.GetType();
        // Thanks NICO
        return datatype.get(type);
    }

    /**
     * Get the name of the requested item, to avoid using GetName()
     *
     * @param item the item whose name we need
     * @return the name of the item
     * @throws Error an Imaris Error Object
     */
    public static String getName(IDataItemPrx item) throws Error {
        return item.GetName();
    }

    public static String getOpenImageName() throws Error {
        return new File(app.GetCurrentFileName()).getName();
    }


}
