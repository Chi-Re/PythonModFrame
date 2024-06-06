package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.PropertiesUtils;
import chire.PythonJavaMod;
import chire.serialization.CRJson;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 方便更改关于读取文件的数据<br>
 * TODO 但代码的耦合性会更高，或许不应该？
 *
 */
public abstract class FileOperation {
    public void spriteOperation(Fi directory, boolean prefix) throws Throwable {
        if (!directory.exists()) return;
        if (!directory.isDirectory()) throw new Throwable("文件 "+directory.path()+" 不为文件夹，错误");
        spriteOperation(
                directory.findAll(f -> f.extension().equals("png")),
                prefix
        );
    }

    public void spriteOperation(Fi directory) throws Throwable {
        spriteOperation(directory, true);
    }

    public void spriteOperation(Seq<Fi> fis) {
        spriteOperation(fis, true);
    }

    public void spriteOperation(Seq<Fi> sprites, boolean prefix){
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

    public void bundleOperation(Fi directory) throws Throwable {
        if (Core.bundle == null || !directory.exists()) return;
        if (!directory.isDirectory()) throw new Throwable("文件 "+directory.path()+" 不为文件夹，错误");
        var str = Core.bundle.getLocale().toString();
        var file = directory.list(f -> f.getName().equals(
                        "bundle" + (str.isEmpty() ? "" : "_") + str + ".properties"
                ) && f.canRead() && f.isFile()
        );

        if (file.length != 0) {
            var bundle = Core.bundle;
            PropertiesUtils.load(bundle.getProperties(), file[0].reader());
            Core.bundle = bundle;
        }
    }
}
