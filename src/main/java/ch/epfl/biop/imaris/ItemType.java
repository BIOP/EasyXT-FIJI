package ch.epfl.biop.imaris;

import Imaris.*;
import Imaris.Error;

import java.util.logging.Logger;

public enum ItemType {

    Volume(IVolume.class, t -> EasyXT.Utils.getFactory().IsVolume(t), t-> EasyXT.Utils.getFactory().ToVolume(t)),

    Spots(ISpots.class, t -> EasyXT.Utils.getFactory().IsSpots(t), t-> EasyXT.Utils.getFactory().ToSpots(t)),

    Surfaces(ISurfaces.class, t -> EasyXT.Utils.getFactory().IsSurfaces(t), t-> EasyXT.Utils.getFactory().ToSurfaces(t)),

    Cells(IICells.class, t -> EasyXT.Utils.getFactory().IsCells(t), t-> EasyXT.Utils.getFactory().ToCells(t)),

    Filaments(IFilaments.class, t -> EasyXT.Utils.getFactory().IsFilaments(t), t-> EasyXT.Utils.getFactory().ToFilaments(t)),

    ReferenceFrames(IReferenceFrames.class, t -> EasyXT.Utils.getFactory().IsReferenceFrames(t), t-> EasyXT.Utils.getFactory().ToReferenceFrames(t)),

    Points(IMeasurementPoints.class, t -> EasyXT.Utils.getFactory().IsMeasurementPoints(t), t-> EasyXT.Utils.getFactory().ToMeasurementPoints(t)),

    ClippingPlane(IClippingPlane.class, t -> EasyXT.Utils.getFactory().IsClippingPlane(t), t-> EasyXT.Utils.getFactory().ToClippingPlane(t)),

    Group(IDataContainer.class, t -> {
        try {
            return EasyXT.Utils.getImarisApp().GetFactory().IsDataContainer(t);
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }, t-> EasyXT.Utils.getImarisApp().GetFactory().ToDataContainer(t)),

    Light(ILightSource.class, t -> {
        try {
            return EasyXT.Utils.getImarisApp().GetFactory().IsLightSource(t);
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }, t-> EasyXT.Utils.getImarisApp().GetFactory().ToLightSource(t)),

    Frame(IFrame.class, t -> EasyXT.Utils.getImarisApp().GetFactory().IsFrame(t), t-> EasyXT.Utils.getImarisApp().GetFactory().ToFrame(t));

    private static final Logger log = Logger.getLogger(ItemQuery.class.getName());

    private final Comparer<IDataItemPrx> comparer;
    private final Converter<IDataItemPrx> converter;
    Class cls;

    ItemType(Class cls, Comparer<IDataItemPrx> comparer, Converter<IDataItemPrx> converter ) {
        this.cls = cls;
        this.comparer = comparer;
        this.converter = converter;
    }
    boolean matches(IDataItemPrx item) {
        try {
            return comparer.test(item);
        } catch (Error e) {
            log.severe("Error while testing if item "+item.getClass().getSimpleName()+" is of type "+this.name());
            log.severe(e.toString());
            return false;
        }
    }

    IDataItemPrx convert(IDataItemPrx item) {
        try {
            return converter.convert(item);
        } catch (Error e) {
            log.severe("Error while converting item "+item.getClass().getSimpleName()+" to "+this.name());
            log.severe(e.toString());
            return null;
        }
    }

    Class<? extends IDataItem> getType() {
        return this.cls;
    }

    private interface Converter<T> {
        IDataItemPrx convert(T item) throws Error;
    }

    private interface Comparer<T> {
        boolean test(T item) throws Error;
    }
}
