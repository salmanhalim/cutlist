package cutlist.packer;

import cutlist.CutList.Demand;

public class Node {
    public boolean    isroot   = false;
    public String     name;
    public double     x;
    public double     y;
    public double     w;
    public double     h;
    public double     cost;
    public boolean    used     = false;
    public Node       right    = null;
    public Node       down     = null;
    public Node       fit      = null;
    public Node       rootNode = null;
    public Demand     key      = null;
    public boolean    rotated  = false;

    public Node(String name, double w, double h, Demand key) {
        this.name = name;
        this.w    = w;
        this.h    = h;
        this.key  = key;
    }

    public Node(String name, double x, double y, double w, double h) {
        this.name = name;
        this.x    = x;
        this.y    = y;
        this.w    = w;
        this.h    = h;

        if (x == 0 && y == 0){
            this.isroot = true; // This is to print 'Pack Starts Here' in the example code
        }
    }

    public Node(String name, double x, double y, double w, double h, double cost) {
        this(name, x, y, w, h);

        this.cost = cost;
    }

    public String getRootName() {
        return fit != null && fit.rootNode != null ? fit.rootNode.name : fit.name;
    }

    public Node getRoot() {
        return fit != null && fit.rootNode != null ? fit.rootNode : fit;
    }

    public double getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Node [isroot=" + isroot + ", name=" + name + ", key=" + key + ", x=" + x + ", y=" + y + ", w=" + w + ", h=" + h
            + ", cost=" + cost + ", used=" + used + ", right=" + right + ", down=" + down + ", fit=" + fit
            + ", rootNode=" + (rootNode == null ? "null" : rootNode.name) + "]";
    }
}
