package net.kolls.railworld.opening;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Created by siarhei on 1.10.15.
 */
public class MapListModel<T> extends AbstractListModel {
    private ArrayList<String> names;
    private ArrayList<T> maps;
    @Override
    public int getSize() {
        return names.size();
    }

    @Override
    public Object getElementAt(int index) {
        return names.get(index);
    }

    public T getMapAt(int index) {
        return maps.get(index);
    }

    public MapListModel() {
        names = new ArrayList<>();
        maps = new ArrayList<>();
    }

    public void clear() {
        int size = getSize();
        names = new ArrayList<>();
        maps = new ArrayList<>();
        this.fireContentsChanged(this, 0, size);
    }

    public void addElement(String name, T map) {
        int size = getSize();
        names.add(name);
        maps.add(map);
        this.fireContentsChanged(this, size, size + 1);
    }
}
