package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import chire.func.Fun;
import mindustry.ctype.UnlockableContent;

import static chire.PythonJavaMod.enabled;
import static chire.PythonJavaMod.modContents;

public class ModData {
    public Fi directory;

    public String name;

    public String author = "frog";

    public String version = "1.0.0";
    /**模组运行时的文件*/
    public String main = "main.py";

    public Fun<TextureRegion> iconTexture;

    public String description = "";

    public ModData(Fi directory, Fun<TextureRegion> icon, String name, String author, String version, String description, String main){
        load(directory, icon, name, author, version, description, main);
    }

    public ModData(Fi directory, String name, String author, String version, String description, String main){
        this(directory, directory.child("icon.png"), name, author, version, description, main);
    }

    public ModData(Fi directory, Fi file, String name, String author, String version, String description, String main){
        load(directory, null, name, author, version, description, main);
        try {
            if (file.exists() && !file.isDirectory())
                this.iconTexture = () -> new TextureRegion(new Texture(file));
        } catch(Error e){
            throw new Error(e);
        }
    }

    public ModData(Fi directory, String iconName, String name, String author, String version, String description, String main){
        load(directory, () -> Core.atlas.find(iconName), name, author, version, description, main);
    }


    protected void load(Fi directory, Fun<TextureRegion> icon, String name, String author, String version, String description, String main){
        if (directory == null) throw new NullPointerException("模组文件夹路径为空");
        this.directory = directory;
        this.name = name;
        if (author != null) this.author = author;
        if (version != null) this.version = version;
        if (main != null) this.main = main;
        if (description != null) this.description = description;
        if (icon != null) this.iconTexture = icon;
    }

    public boolean enabled(){
        return enabled.size > 0 && enabled.get(this.name) && enabled != null;
    }

    public Seq<UnlockableContent> getContent(){
        return modContents.get(this);
    }

    public boolean any(){
        return modContents.get(this) != null && modContents.get(this).any();
    }

    @Override
    public String toString() {
        return "ModData{" +
                "directory=" + directory +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", version='" + version + '\'' +
                ", main='" + main + '\'' +
                '}';
    }
}
