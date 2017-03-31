package org.vaadin.alump.maplayout.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Text;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;

import java.util.*;
import java.util.logging.Logger;

/**
 * GWT widget part of MapLayout
 */
public class MapLayoutWidget extends Widget {

    private final static Logger LOGGER = Logger.getLogger(MapLayoutWidget.class.getName());

    private Element currentMapElement;
    private boolean skipTitles = false;

    private Map<String,Set<String>> currentStyleNames = new HashMap<>();

    protected static int widgetCounter = 0;

    protected MapLayoutWidgetListener listener;

    public MapLayoutWidget() {

        setElement(Document.get().createDivElement());
        setStyleName("maplayout-addon");

    }

    public void setSkipTitles(boolean skipTitles) {
        this.skipTitles = skipTitles;
    }

    public void setListener(MapLayoutWidgetListener listener) {
        this.listener = listener;
    }

    public void setMap(String url) {
        RequestBuilder httpClient = new RequestBuilder(RequestBuilder.GET, url);

        try {
            httpClient.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    String responseContent = response.getText();
                    com.google.gwt.xml.client.Document svg = XMLParser.parse(responseContent);
                    injectMap(svg);
                    currentStyleNames = null;

                    Scheduler.get().scheduleDeferred(() -> {
                        if(listener != null) {
                            listener.onInitialRenderDone(MapLayoutWidget.this);
                        }
                    });
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    exception.printStackTrace();
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }

    private void injectMap(com.google.gwt.xml.client.Document source) {
        if(currentMapElement != null) {
            currentMapElement.removeFromParent();
            currentMapElement = null;
        }

        currentMapElement = injectInto(getElement(), source.getDocumentElement());

        currentMapElement.setId("maplayout-addon-" + (++widgetCounter));
    }

    private Element injectInto(Element to, com.google.gwt.xml.client.Node source) {
        if(source.getNodeType() == Node.TEXT_NODE) {
            Text text = Document.get().createTextNode(source.getNodeValue());
            to.appendChild(text);
            return null;
        } else if(source.getNodeType() == Node.ELEMENT_NODE) {
            String nodeName = source.getNodeName();
            if(skipTitles && nodeName.equals("title")) {
                return null;
            }

            Element injected = createElementNS(source.getNamespaceURI(), nodeName);

            for (int i = 0; i < source.getAttributes().getLength(); i++) {
                Node attribute = source.getAttributes().item(i);
                setAttributeNS(injected, attribute.getNamespaceURI(), attribute.getNodeName(), attribute.getNodeValue());
            }

            for (int i = 0; i < source.getChildNodes().getLength(); ++i) {
                com.google.gwt.xml.client.Node injectedChild = source.getChildNodes().item(i);
                injectInto(injected, injectedChild);
            }

            to.appendChild(injected);
            return injected;
        } else {
            return null;
        }
    }

    private static native Element createElementNS(final String ns, final String name)
	/*-{
        return document.createElementNS(ns, name);
    }-*/;

    private static native void setAttributeNS(JavaScriptObject element, final String ns, final String attribute, final String value)
    /*-{
        element.setAttributeNS(ns, attribute, value);
    }-*/;

    private static native Element findChildElement(JavaScriptObject element, final String id)
    /*-{
        if(!id) {
            console.error("Can not find with undefined id");
            return null;
        }
        if(!element) {
            console.error("Can not find from undefined element");
            return null;
        }
        var child = element.querySelector('#' + id);
        if(!child) {
            console.warn('Failed to find map child: ' + id);
        }
        return child;
    }-*/;

    private static native void addClassName(JavaScriptObject element, String className)
    /*-{
        element.classList.add(className);
    }-*/;

    private static native void removeClassName(JavaScriptObject element, String className)
    /*-{
        element.classList.remove(className);
    }-*/;

    public void setItemStyleNames(Map<String,Set<String>> styleNames) {
        if(styleNames == null || currentMapElement == null) {
            LOGGER.severe("update extra styles cancelled");
            return;
        }

        if(currentStyleNames != null) {
            currentStyleNames.entrySet().forEach(entry -> {
                final String removedStyleName = entry.getKey();
                Collection<String> ids = entry.getValue();
                ids.forEach(id -> {
                    Element child = findChildElement(currentMapElement, id);
                    removeClassName(child, removedStyleName);
                });
            });
        }

        currentStyleNames = new HashMap<>();
        styleNames.entrySet().forEach(entry -> {
            final String styleName = entry.getKey();
            Collection<String> ids = entry.getValue();
            if(ids.isEmpty()) {
                return;
            }

            ids.forEach(id -> {
                Element child = findChildElement(currentMapElement, id);
                addClassName(child, styleName);
            });
            currentStyleNames.put(styleName, new HashSet<>(ids));
        });

    }

}