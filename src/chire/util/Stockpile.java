package chire.util;

import arc.Core;
import arc.struct.ArrayMap;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Stockpile {
    public Stockpile(){
    }

    /**Object -> json*/
    public static String toObjectJson(Object... items){
        if(items.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"").append(items[0].toString()).append("\"");
        sb.append(":");
        if (items.length >= 2) sb.append(inStrJson(items[1]));
        else sb.append("\"\"");

        for(int i = 2; i < items.length; i += 2){
            sb.append(",");
            sb.append("\"").append(items[i]).append("\"");
            sb.append(":");
            sb.append(inStrJson(items[i+1]));
        }

        sb.append("}");
        return sb.toString();
    }

    /**Map -> json*/
    public static String toMapJson(Object map){
        if (map instanceof ArrayMap<?, ?> ms) {
            if (ms.size == 0) return "{}";
            List<Object> list = new ArrayList<>();
            for (var m : ms){
                list.add(m.key);
                list.add(m.value);
            }
            return toObjectJson(list.toArray());
        } else if (map instanceof Map<?,?> ms) {
            if (ms.size() == 0) return "{}";
            List<Object> list = new ArrayList<>();
            var k = ms.keySet().toArray();
            var v = ms.values().toArray();
            for (int i = 0; i < ms.size(); i++) {
                list.add(k[i]);
                list.add(v[i]);
            }
            return toObjectJson(list);
        }
        return "{}";
    }

    /**
     * json -> map
     *
     * @param type 默认key为str，但value是根据type。
     */
    public static <V> ArrayMap<String, V> toJsonMap(JsonValue json, Class<V> type){
        ArrayMap<String, V> map = new ArrayMap<>();
        if (json == null) return map;
        for (var j : json){
            if (type.isAssignableFrom(Boolean.class)) {
                map.put(j.name(), (V) getValue(j));
                continue;
            }
            map.put(j.name(), type.cast(getValue(j)));
        }
        return map;
    }

    public static <V> ArrayMap<String, V> toJsonMap(String json, Class<V> type){
        return toJsonMap(new JsonReader().parse(json), type);
    }

    /**
     * 例子:指定的json数据为 {"key": value}<br>
     * 获取的数据为 value
     *
     * @param <T> 转换的值。
     * @param json 指定的json数据。
     * @param key key的名称。
     * @param type 类型。
     *
     * @return json中的value。
     */
    public static <T> T getValue(JsonValue json, String key, Class<T> type){
        return type.cast(json.getString(key));
    }

    /**
     * 可以直接获取数据为 "key": value 的{@link JsonValue}<br>
     *
     * @param <T> 转换的值。
     * @param json 指定的JsonValue数据。
     * @param type 类型。
     */
    public static <T> T getValue(JsonValue json, Class<T> type){
        return getValue(json.parent(), json.name, type);
    }

    public static Object getValue(JsonValue json){
        return getValue(json, Object.class);
    }

    public static boolean inBoolean(String str){
        return str.equals("true");
    }

    /**将 str -> "str"*/
    public static String inStrJson(Object text){
        if (text instanceof String)
            return "\""+ text +"\"";
        else if (text instanceof Boolean)
            return ((boolean)text) ? "true" : "false";
        else if (text instanceof Integer || text instanceof Float || text instanceof Double)
            return text.toString();
        return text == null ? "" : ("\""+ text +"\"");
    }

    public static JsonValue settingParse(String name){
        return new JsonReader().parse(getSetting(name));
    }

    public static String getSetting(String name){
        return Core.settings.getString(name, "");
    }

    public static void putSetting(String name, String value){
        Core.settings.put(name, value);
    }

    public static void putMap(String name, ArrayMap<?, ?> map){
        putSetting(name, toMapJson(map));
    }

    public static <V> ArrayMap<String, V> getMap(String name, Class<V> type){
        return toJsonMap(getSetting(name), type);
    }

    public static <V> boolean revise(String name, String key, V value, Class<V> type){
        if (Core.settings.has(name)) return false;
        ArrayMap<String, V> last = Stockpile.getMap(name, type);
        last.put(key, value);
        Core.settings.put(name, toMapJson(last));
        return true;
    }
}
