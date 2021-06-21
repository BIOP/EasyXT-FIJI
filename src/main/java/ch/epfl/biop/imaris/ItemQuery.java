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
/*
 * Copyright (c) 2021 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
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

import Imaris.Error;
import Imaris.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ItemQuery is a class of EasyXT that handles searches in the surpass scene
 * This is used by EasyXT to locate objects and returns them.
 * @author Olivier Burri
 * @version 1.0
 */
public class ItemQuery {
    private IDataContainerPrx itemParent = null;
    private String itemName = null;
    private Class<? extends IDataItem> itemType = null;
    public static Boolean isRecursiveSearch = false;

    private List<IDataItemPrx> items;
    private static final Logger log = Logger.getLogger(ItemQuery.class.getName());


    /**
     * type of objects that are matched to the classes. That way users just need to specify the names as Strings
     * "Spots", "Frame", "Group", etc...
     */
    public enum ItemType {

        Spots(ISpots.class),
        Surfaces(ISurfaces.class),
        Volume(IVolume.class),
        Camera(ISurpassCamera.class),
        Light(ILightSource.class),
        Frame(IFrame.class),
        Group(IDataContainer.class);

        Class cls;

        ItemType(Class cls) {
            this.cls = cls;
        }

        Class<? extends IDataItem> getType() {

            return this.cls;
        }
    }

    /**
     * Helper method : kind of odd, but we cannot cast to the class directly and we need to explicitely use the
     * ImarisFactory type checkers... so this wrapper is used along with the ENUM {@link ItemQuery.ItemType}
     * @param item
     * @return class of the contained item
     * @throws Error
     */
    private static Class<? extends IDataItem> getType(IDataItemPrx item) throws Error {
        IFactoryPrx factory = EasyXT.Utils.getImarisApp().GetFactory();

        if (factory.IsSpots(item)) {
            return ItemType.Spots.getType();
        }
        if (factory.IsSurfaces(item)) {
            return ItemType.Surfaces.getType();
        }
        if (factory.IsVolume(item)) {
            return ItemType.Volume.getType();
        }
        if (factory.IsSurpassCamera(item)) {
            return ItemType.Camera.getType();
        }
        if (factory.IsLightSource(item)) {
            return ItemType.Light.getType();
        }
        if (factory.IsFrame(item)) {
            return ItemType.Frame.getType();
        }
        if (factory.IsDataContainer(item)) {
            return ItemType.Group.getType();
        }

        log.warning("Type not found for item " + item + " of class " + item.getClass().getSimpleName());
        return null;
    }

    /**
     * The class that does the item query. You do not have access to its constructor. Use {@link ItemQueryBuilder}
     * to instantiate an object of this type
     *
     * @param parent   the parent group, null for the main surpass scene
     * @param itemName the name of the item, null if not used
     * @param itemType the type of the item This is an Imaris type, but the builder uses a string for convenience.
     */
    private ItemQuery(IDataContainerPrx parent, String itemName, Class<? extends IDataItem> itemType) {
        this.itemParent = parent;
        this.itemName = itemName;
        this.itemType = itemType;
        items = new ArrayList<>();
    }

    /**
     * This starts the query for the desired Item
     * @return a list of items that match the request
     * @throws Error an Imaris Error
     */
    public List<IDataItemPrx> find() throws Error {
        if (this.itemParent != null)
            return find(this.itemParent);
        else
            return new ArrayList<IDataItemPrx>();
    }

    /**
     * Find the first object that matches the query
     * @return the first item that matches the query
     * @throws Error an Imaris Error
     */
    public IDataItemPrx findFirst() throws Error {
        items = find();
        if (items.size() > 0) return find().get(0);
        return null;
    }

    /**
     * This method calls the actual search and is not called by the user. it is only used by the class
     * @param parent the parent to start the search from
     * @return a list of items that match the request
     * @throws Error an Imaris Error
     */
    private List<IDataItemPrx> find(IDataContainerPrx parent) throws Error {

        int nChildren = parent.GetNumberOfChildren();

        for (int i = 0; i < nChildren; i++) {
            IDataItemPrx child = EasyXT.Utils.castToType(parent.GetChild(i));

            String aName = child.GetName();
            Class aCls = getType(child);
            // If it's a group, recurse before continuing
            if (aCls.equals(ItemType.Group.getType()) && isRecursiveSearch) {
                find((IDataContainerPrx) child);
            }

            if (this.itemName != null) { // Name set
                if (aName.equals(this.itemName)) {  // Name matches
                    if (this.itemType != null) { // Name and Type set
                        if (aCls.equals(this.itemType)) { // Name an Type match
                            items.add(child); // We found the right child with the right name, right class
                        }
                    } else { // Name set but Type Unset: Keep
                        items.add(child); // We found the right child with the right name, right class at the right position
                    }
                }
            } else { // Name not is set, use only type and number
                if (this.itemType != null) { // Type set
                    if (aCls.equals(this.itemType)) { // Type matches
                        items.add(child); // We found the right child at the right position
                    }
                } else { // Name not set, Type not set, only Position
                    items.add(child); // We found the right child at the right position
                }
            }
        }

        // Cleanup all items for them to match their class
        items.replaceAll(item -> {
            try {
                return EasyXT.Utils.castToType(item);
            } catch (Error error) {
                error.printStackTrace();
            }
            return item;
        });

        return items;
    }

    /**
     * to get only the nth item from this list, usually the first one, you can use this method as a shortcut
     * @param position the position (0-based) of the item to recover
     * @return the item in question or null if there is no item.
     * @throws Error an Imaris Error
     */
    public IDataItemPrx find(int position) throws Error {
        List<IDataItemPrx> items = find();
        if (position < items.size()) return items.get(position);

        log.warning("You requested item number " + position + ". There are only " + items.size() + " items");
        return null;
    }

    /**
     * Convenience method to see the contents of the ItemQuery class
     * @return a description of the ItemQuery
     */
    @Override
    public String toString() {
        try {
            return "ItemQuery with the following elements: \n" +
                    "  parent:\t" + EasyXT.Scene.getName(itemParent) + "\n" +
                    "  name:\t '" + itemName + "\n" +
                    "  type:\t" + itemType;
        } catch (Error error) {
            error.printStackTrace();
        }
        return "ItemQuery with the following elements: \n" +
                "  name:\t '" + itemName + "\n" +
                "  type:\t" + itemType;
    }

    /**
     * Inner builder class to access ItemQueryBuilder.
     */
    public static class ItemQueryBuilder {
        private IDataContainerPrx itemParent = null;
        String itemName = null;
        Class<? extends IDataItem> itemType = null;

        /**
         * Set the name of the item to find
         * You can use slashes to start the search in the given group
         * Eg. My Folder/My Spots.
         * @param itemName the name of the items to find
         * @return this builder
         */
        public ItemQueryBuilder setName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        /**
         * provide the parent object from which to start the search. If not provided it will
         * start at the root of the Surpass Scene.
         * @param itemParent the parent object to start the search from
         * @return this builder
         */
        public ItemQueryBuilder setParent(IDataContainerPrx itemParent) {
            this.itemParent = itemParent;
            return this;
        }

        /**
         * set the type as defined by {@link ItemType}
         * @param itemType the type, as a String: ("Spots", "Surfaces", "Group", ...)
         * @return this builder
         */
        public ItemQueryBuilder setType(String itemType) {
            this.itemType = ItemType.valueOf(itemType).getType();
            return this;
        }

        /**
         * Generates the desired ItemQuery
         * @return the ItemQuery that will execute the search
         * @throws Error an Imaris Error
         */
        public ItemQuery build() throws Error {
            if (this.itemParent == null) this.itemParent = EasyXT.Scene.getScene();

            // Special case where there are slashes denoting a shortcut to finding the object
            if (this.itemName != null) {
                String[] parts = this.itemName.split("/");
                if (parts.length > 1) {
                    Boolean isRecursiveTmp = ItemQuery.isRecursiveSearch;
                    ItemQuery.isRecursiveSearch = false;

                    // Find all child objects from the left onwards, to find the right parent to start the search
                    IDataContainerPrx scene = EasyXT.Scene.getScene();
                    for (int i = 0; i < parts.length - 1; i++) {
                        String child = parts[i];
                        IDataItemPrx tempItem = new ItemQuery(scene, child, ItemType.Group.getType()).find(0); // Find the next child
                        // if it exists, then it is a container and we change the Scene
                        if (tempItem != null) scene = (IDataContainerPrx) tempItem;
                    }
                    //At the end of this, we should have the last parent and we can build the query
                    // Reset recursive search flag back to whatever it was
                    ItemQuery.isRecursiveSearch = isRecursiveTmp;

                    return new ItemQuery(scene, parts[parts.length - 1], this.itemType);
                }
            }
            // Normal query
            return new ItemQuery(this.itemParent, this.itemName, this.itemType);
        }
    }
}
