package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.io.PropertiesUtils;

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

    public void spriteOperation(Seq<Fi> fis, boolean prefix) {
        PyMods.setSprite(fis, prefix);
    }

    public void spriteOperation(Seq<Fi> fis) {
        PyMods.setSprite(fis, true);
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
