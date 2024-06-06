package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import chire.func.Fun;
import mindustry.ctype.UnlockableContent;

import static chire.PythonJavaMod.pyMods;

public class ModData {
    public Fi directory;

    public String name;

    public String author = "frog";

    public String version = "1.0.0";
    /**ģ������ʱ���ļ�*/
    public String main = "main.py";

    public Fun<TextureRegion> iconTexture;

    public String description = "";


    private boolean enabled = true;

    private Seq<UnlockableContent> contents = new Seq<>();

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
        if (directory == null) throw new NullPointerException("ģ���ļ���·��Ϊ��");
        this.directory = directory;
        this.name = name;
        if (author != null) this.author = author;
        if (version != null) this.version = version;
        if (main != null) this.main = main;
        if (description != null) this.description = description;
        if (icon != null) this.iconTexture = icon;
    }

    public boolean enabled(){
//        if (pyMods.enabled().get(this.name) == null) {
//            pyMods.setEnabled(this, true);
//            return true;
//        }
//        return pyMods.EnabledSize() > 0 && pyMods.enabled().get(this.name) && pyMods.enabled() != null;

        return enabled;
    }

    public void setEnabled(boolean e){
        this.enabled = e;
    }

    public void setContents(Seq<UnlockableContent> contents) {
        this.contents = contents;
    }

    public Seq<UnlockableContent> getContent(){
        //return pyMods.contents().get(this);
        return contents;
    }

    public boolean any(){
        //return pyMods.contents().get(this) != null && pyMods.contents().get(this).any();
        return contents.any();
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
