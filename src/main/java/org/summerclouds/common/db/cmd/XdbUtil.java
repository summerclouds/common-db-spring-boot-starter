package org.summerclouds.common.db.cmd;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MCollection;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.db.DbCollection;
import org.summerclouds.common.db.xdb.XdbManager;
import org.summerclouds.common.db.xdb.XdbService;
import org.summerclouds.common.db.xdb.XdbType;

public class XdbUtil {

    public static XdbType<?> getType(String apiName, String serviceName, String typeName)
            throws NotFoundException {
        XdbManager api = MSpring.lookup(XdbManager.class);
        XdbService service = api.getService(serviceName);
        return service.getType(typeName);
    }

    public static XdbService getService(String apiName, String serviceName)
            throws NotFoundException {
        XdbManager api = MSpring.lookup(XdbManager.class);
        return api.getService(serviceName);
    }

    public static <T> DbCollection<T> createObjectList(
            XdbType<T> type, String search, Map<String, Object> parameterValues) throws Exception {

        if (search.startsWith("(") && search.endsWith(")"))
            return type.getByQualification(
                    search.substring(1, search.length() - 1), parameterValues);

        return new IdArrayCollection<T>(type, search.split(","));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setValue(XdbType<?> type, Object object, String name, Object v)
            throws Exception {
        int p = name.indexOf('.');
        if (p > 0) {
            String p1 = name.substring(0, p);
            String p2 = name.substring(p + 1);
            Class<?> t = type.getAttributeType(p1);
            if (Map.class.isAssignableFrom(t)) {
                Map map = type.get(object, p1);
                if (map == null) {
                    if (t.isInterface()) map = new HashMap<>();
                    else map = (Map) t.getDeclaredConstructor().newInstance();
                    type.set(object, p1, map);
                }
                if (p2.equals("remove")) {
                    map.remove(v);
                } else if (p2.startsWith("set:")) {
                    p2 = p2.substring(4);
                    map.put(p2, v);
                } else map.put(p2, v);
            } else if (Collection.class.isAssignableFrom(t)) {
                Collection col = type.get(object, p1);
                if (col == null) {
                    if (t.isInterface()) col = new LinkedList<>();
                    else col = (Collection) t.getDeclaredConstructor().newInstance();
                    type.set(object, p1, col);
                }
                if (p2.equals("add") || p2.equals("last")) {
                    col.add(v);
                } else if (p2.equals("first") && col instanceof Deque) {
                    ((Deque) col).addFirst(v);
                } else if (p2.equals("clear")) {
                    col.clear();
                } else if (p2.equals("remove")) {
                    col.remove(v);
                } else {
                    int i = MCast.toint(p2, -1);
                    if (i > -1 && col instanceof AbstractList) {
                        ((AbstractList) col).set(i, v);
                    }
                }

            } else if (t.isArray()) {
                Object array = type.get(object, p1);
                LinkedList<Object> col = (LinkedList<Object>) MCollection.toList((Object[]) array);
                if (p2.equals("add") || p2.equals("last")) {
                    col.add(v);
                } else if (p2.equals("first")) {
                    col.addFirst(v);
                } else if (p2.equals("clear")) {
                    col.clear();
                } else if (p2.equals("remove")) {
                    col.remove(v);
                } else {
                    int i = MCast.toint(p2, -1);
                    if (i > -1) {
                        col.set(i, v);
                    }
                }
                array = Array.newInstance(t.getComponentType(), col.size());
                for (int i = 0; i < col.size(); i++) Array.set(array, i, col.get(i));
                type.set(object, p1, array);
            }
        } else type.set(object, name, v);
    }

    public static Object prepareValue(XdbType<?> type, String name, Object value) {
        int p = name.indexOf('.');
        if (p > 0) return value;
        Object v = type.prepareManualValue(name, value);
        return v;
    }

}
