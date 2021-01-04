/**
 * Copyright (c) 2020 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
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

import Imaris.*;
import Imaris.Error;

import java.util.ArrayList;
import java.util.List;

public class ItemQuery {
    private IDataContainerPrx itemParent = null;
    private String itemName = null;
    private Class<? extends IDataItem> itemType = null;
    private int itemPosition = 0;

    // Enum containing the classes of the different ImarisObjects
    public enum ItemType {

        Spots( ISpots.class ),
        Surfaces( ISurfaces.class ),
        Volume( IVolume.class ),
        Camera( ISurpassCamera.class ),
        Light( ILightSource.class ),
        Frame( IFrame.class ),
        Datacontainer( IDataContainer.class );

        Class cls;

        ItemType( Class cls ) {
            this.cls = cls;
        }

        Class<? extends IDataItem> getType( ) {
            return this.cls;
        }
    }

    /**
     * Helper method : kind of odd, but we cannot cast to the class directly and we need to explicitely use the
     * ImarisFactory type checkers... so this wrapper is used along with the ENUM {@link ItemQuery.ItemType}
     *
     * @param item
     * @return class of the contained item
     * @throws Error
     */
    private static Class<? extends IDataItem> getType( IDataItemPrx item ) throws Error {
        IFactoryPrx factory = EasyXT.getImaris( ).GetFactory( );

        if ( factory.IsSpots( item ) ) {
            return ItemType.Spots.getType( );
        }
        if ( factory.IsSurfaces( item ) ) {
            return ItemType.Surfaces.getType( );
        }
        if ( factory.IsVolume( item ) ) {
            return ItemType.Volume.getType( );
        }
        if ( factory.IsSurpassCamera( item ) ) {
            return ItemType.Camera.getType( );
        }
        if ( factory.IsLightSource( item ) ) {
            return ItemType.Light.getType( );
        }
        if ( factory.IsFrame( item ) ) {
            return ItemType.Frame.getType( );
        }
        if ( factory.IsDataContainer( item ) ) {
            return ItemType.Datacontainer.getType( );
        }

        System.err.println("Type not found for item "+item+" of class "+item.getClass().getSimpleName());

        return null;
    }


    private ItemQuery( IDataContainerPrx parent, String itemName, Class<? extends IDataItem> itemType, int itemPosition ) {
        this.itemParent = parent;
        this.itemName = itemName;
        this.itemType = itemType;
        this.itemPosition = itemPosition;
    }

    public IDataContainerPrx getParent( ) {
        return this.itemParent;
    }

    public String getName( ) {
        return this.itemName;
    }

    public Class<? extends IDataItem> getType( ) {
        return this.itemType;
    }

    public int getPosition() {
        return itemPosition;
    }

    public List<IDataItemPrx> get() throws Error {

            IDataContainerPrx parent = this.getParent( );
            int nChildren = parent.GetNumberOfChildren( );

            List<IDataItemPrx> items = new ArrayList<>( );

            for ( int i = 0; i < nChildren; i++ ) {
                IDataItemPrx child = parent.GetChild( i );

                String aName = child.GetName( );
                Class aCls = getType( child );

                if ( this.getName( ) != null ) { // Name set
                    if ( aName.equals( this.getName( ) ) ) {  // Name matches
                        if ( this.getType( ) != null ) { // Name and Type set
                            if ( aCls.equals( this.getType( ) ) ) { // Name an Type match
                                items.add( child ); // We found the right child with the right name, right class
                            }
                        } else { // Name set but Type Unset: Keep
                            items.add( child ); // We found the right child with the right name, right class at the right position
                        }
                    }
                } else { // Name not is set, use only type and number
                    if ( this.getType( ) != null ) { // Type set
                        if ( aCls.equals( this.getType( ) ) ) { // Type matches
                            items.add( child ); // We found the right child at the right position
                        }
                    } else { // Name not set, Type not set, only Position
                        items.add( child ); // We found the right child at the right position
                    }
                }
            }

            // Cleanup all items for them to match their class
            items.replaceAll( item -> {
                try {
                    return EasyXT.castToType( item );
                } catch ( Error error ) {
                    error.printStackTrace( );
                }
                return item;
            } );
            return items;
        }

    @Override
    public String toString( ) {
        try {
            return "ItemQuery with the following elements: \n" +
                    "  parent:\t" + EasyXT.getName( itemParent ) +"\n"+
                    "  name:\t '" + itemName + "\n" +
                    "  type:\t" + itemType + "\n" +
                    "  position:\t" + itemPosition;
        } catch ( Error error ) {
            error.printStackTrace( );
        }
        return "ItemQuery with the following elements: \n" +
                "  name:\t '" + itemName + "\n" +
                "  type:\t" + itemType + "\n" +
                "  position:\t" + itemPosition;
    }

    public static class ItemQueryBuilder {
        private IDataContainerPrx itemParent = null;
        String itemName = null;
        Class<? extends IDataItem> itemType = null;
        int itemPosition = 1;


        public ItemQueryBuilder setName( String itemName ) {
            this.itemName = itemName;
            return this;
        }

        public ItemQueryBuilder setParent( IDataContainerPrx itemParent ) {
            this.itemParent = itemParent;
            return this;
        }

        public ItemQueryBuilder setType( String itemType ) {
            this.itemType = ItemType.valueOf( itemType ).getType( );
            return this;
        }

        public ItemQueryBuilder setPosition( int itemPosition ) {
            if ( this.itemPosition >= 0 ) this.itemPosition = itemPosition;
            return this;
        }

        public ItemQuery build( ) throws Error {
            if (this.itemParent == null) this.itemParent = EasyXT.getImaris().GetSurpassScene();

            return new ItemQuery( this.itemParent, this.itemName, this.itemType, this.itemPosition );
        }
    }
}