package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.Log;
import chire.PythonJavaMod;
import chire.serialization.CRJson;
import mindustry.graphics.MultiPacker;
import org.python.util.PythonInterpreter;

import java.util.Objects;

import static chire.PythonJavaMod.*;

public class PyMods {
    public PyMods(){
    }

    public static void load(){
        for (var mainJson : pyModDirectory.findAll(f -> {
            if (!Objects.equals(f.name(), "main.json")) return false;
            return new CRJson(f).getBoolean("python");
        })){
            CRJson config = new CRJson(mainJson);
            var md = new ModData(
                    mainJson.parent(),
                    config.getStr("name"),
                    config.getStr("author"),
                    config.getStr("version"),
                    config.getStr("description"),
                    config.getStr("main")
            );
            mods.put(config.getStr("name"), md);
            interpreters.add(new ModInterpreter(md, new PythonInterpreter()));
        }
    }

    public static void setSprite(Seq<Fi> sprites, boolean prefix){
        boolean bleed = Core.settings.getBool("linear", true) && !PythonJavaMod.core.meta.pregenerated;
        //解除限制(?)
        float textureScale = PythonJavaMod.core.meta.texturescale;

        for(Fi file : sprites) {
            String name = file.nameWithoutExtension();

            if(!prefix && !Core.atlas.has(name)){
                Log.warn("mod中的 '@' 贴图 '@' 试图覆盖游戏中不存在的贴图，这不脱裤子放屁吗？", name, PythonJavaMod.core.name);
                continue;
            }else if(prefix && !PythonJavaMod.core.meta.keepOutlines && name.endsWith("-outline") && file.path().contains("units") && !file.path().contains("blocks")){
                Log.warn("mod中的 '@' 精灵 '@' 很可能是一个不必要的单位轮廓。这些不应该是单独的贴图，忽略.", name, PythonJavaMod.core.name);
                continue;
            }

            try{
                Pixmap pix = new Pixmap(file.readBytes());
                if(bleed){
                    Pixmaps.bleed(pix, 2);
                }

                String fullName = (prefix ? PythonJavaMod.core.name + "-" : "") + name;

                Core.atlas.addRegion(fullName, new TextureRegion(new Texture(file)));

                pix.dispose();
            }catch(Error e){
                throw new Error(e);
            }
        }
    }

    public static void importMod(Fi file) {
        copyDirectory(new ZipFi(file), pyModDirectory);

        requiresReload = true;
    }

    private static void copyFile(Fi source, Fi dest){
        try{
            dest.write(source.read(), false);
        }catch(Exception ex){
            throw new RuntimeException("复制文件出现错误:"+ex);
        }
    }

    private static void copyDirectory(Fi sourceDir, Fi destDir){
        destDir.mkdirs();
        Fi[] files = sourceDir.list();
        for(Fi srcFile : files){
            Fi destFile = destDir.child(srcFile.name());
            if(srcFile.isDirectory())
                copyDirectory(srcFile, destFile);
            else
                copyFile(srcFile, destFile);
        }
    }


    public static MultiPacker.PageType getPage(Fi file){
        String path = file.path();
        return
            path.contains("sprites/blocks/environment") || path.contains("sprites-override/blocks/environment") ? MultiPacker.PageType.environment :
            path.contains("sprites/editor") || path.contains("sprites-override/editor") ? MultiPacker.PageType.editor :
            path.contains("sprites/rubble") || path.contains("sprites-override/rubble") ? MultiPacker.PageType.rubble :
            path.contains("sprites/ui") || path.contains("sprites-override/ui") ? MultiPacker.PageType.ui :
            MultiPacker.PageType.main;
    }

    public static void setEnabled(ModData mod, boolean e){
        putSetting(mod.name, e);
        enabled.put(mod.name, e);
        requiresReload = true;
    }
}
