package chire.serialization;

import arc.files.Fi;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

public class CRJson {
    private String content = "";

    public CRJson(){
    }

    public CRJson(String content){
        setContent(content);
    }

    public CRJson(Fi file){
        setContent(file);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setContent(Fi file) {
        if (file.isDirectory()) throw new RuntimeException(file.path()+"为文件夹！无法读取");
        this.content = file.readString();
    }

    public JsonValue parse(){
        return new JsonReader().parse(content);
    }

    public static String getName(JsonValue value){
        return value.parent().get(value.name).toString();
    }

    public String getStr(String name){
        try {
            return parse().getString(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public Integer getInt(String name){
        try {
            return parse().getInt(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public Float getFloat(String name){
        try {
            return parse().getFloat(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public Double getDouble(String name){
        try {
            return parse().getDouble(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public Boolean getBoolean(String name){
        try {
            return parse().getBoolean(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    public Long getLong(String name){
        try {
            return parse().getLong(name);
        } catch (IllegalArgumentException e){
            return null;
        }
    }
}
