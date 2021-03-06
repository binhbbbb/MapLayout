package org.vaadin.alump.maplayout;

import com.vaadin.util.ReflectTools;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * MapLayout click listener
 */
public interface MapLayoutClickListener extends Serializable {
    Method MAP_LAYOUT_CLICK_METHOD = ReflectTools.findMethod(
            MapLayoutClickListener.class, "mapLayoutClick",
            new Class[]{MapLayoutClickEvent.class});

    /**
     * Callback called when user clicks on MapLayout
     * @param event Event details
     */
    void mapLayoutClick(MapLayoutClickEvent event);
}
