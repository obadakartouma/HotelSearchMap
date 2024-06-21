package kart;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;

import pp.dorenda.client2.MapWidget;
import pp.dorenda.client2.overlay.MapOverlay;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Envelope;
import java.util.HashMap;
import java.util.Map;

public class ResultsOverlay extends MapOverlay {
    private MapWidget mapWidget;

    // private String text=null;
    // private Geometry geom=null;
    private HashMap<String, Geometry> res = new HashMap<String, Geometry>();

    public ResultsOverlay(MapWidget mapWidget) {
        this.mapWidget=mapWidget;
    }

    public void addResult(String text, Geometry geom) {
        res.put(text, geom);
    }

    public void resetResult() {
        res.clear();
    }

    public void paint(Graphics2D g) {
        if (res == null)
            return;

        for (Map.Entry<String, Geometry> entry : res.entrySet()) {
            Geometry geom = entry.getValue();
            String text = entry.getKey();

            mapWidget.paintJTSGeometry(g,new Color(100,200,100,200),geom,1,3);

            int[] textPos;
            if (geom instanceof com.vividsolutions.jts.geom.Point) {
                textPos=geo2pixelI(((com.vividsolutions.jts.geom.Point)geom).getY(),
                        ((com.vividsolutions.jts.geom.Point)geom).getX());
                textPos[1]-=15;
            }
            else {
                Envelope boundingBox=geom.getEnvelopeInternal();
                double centerX=(boundingBox.getMinX()+boundingBox.getMaxX())/2;
                double centerY=(boundingBox.getMinY()+boundingBox.getMaxY())/2;
                textPos=geo2pixelI(centerY,centerX);
            }
            mapWidget.paintText(g,new Point(textPos[0],textPos[1]),Color.black,Color.white,Color.white,text);
        }
    }
}
