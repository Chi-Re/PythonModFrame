package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import chire.mod.error.ErrorData;
import chire.serialization.CRJson;
import chire.ui.PyCore;
import mindustry.ctype.Content;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.MultiPacker;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static chire.PythonJavaMod.core;
import static mindustry.Vars.*;

public class PyMods {
    /**所有的解释器，为了统一调用*/
    private final Seq<General> interpreters = new Seq<>();

    public static final ArrayMap<String, ArrayMap<String, PyObject>> imports = new ArrayMap<>();

    public Seq<ModData> mods = new Seq<>();

    /**是否存在报错*/
    public static boolean hasError = false;
    /**所有的报错*/
    private final Seq<ErrorData> errors = new Seq<>();
    /**所有模组的启用的情况*/
    //private final ArrayMap<String, Boolean> enabled = new ArrayMap<>();
    /**所有模组的内容*/
    //private final ArrayMap<ModData, Seq<UnlockableContent>> modContents = new ArrayMap<>();

    private boolean requiresReload = false;
    /**缓存用的，用来计算modContents的数据*/
    private Seq<UnlockableContent> lastContent = null;

    private final String settingName = "py-enabled-mods";

    private final Fi pyModDirectory;

    public PyMods(){
        pyModDirectory = modDirectory.child("py_mods");

        addMod(new PluginInterpreter(new ModData(
                modDirectory.child("py_plugin"),
                "python-mod-plugin",
                "py-plugin",
                "炽热S",
                "1.1.4",
                "模组的插件~",
                null
        )));

        for (var mainJson : pyModDirectory.findAll(f -> {
            if (!Objects.equals(f.name(), "main.json")) return false;
            return new CRJson(f).getBoolean("python");
        })){
            //mods.put(config.getStr("name"), md);
            addMod(getConfig(mainJson), new PythonInterpreter());
        }
    }

    public ModData getConfig(Fi main){
        if (!main.exists() || main.isDirectory()) throw new RuntimeException("读取模组json配置文件出错");
        CRJson config = new CRJson(main);
        return new ModData(
                main.parent(),
                config.getStr("name"),
                config.getStr("author"),
                config.getStr("version"),
                config.getStr("description"),
                config.getStr("main")
        );
    }

    public void loadMod(){
        for (var inter : interpreters) {
            var config = inter.getConfig();
            if (getSetting(config.name)) try {
                inter.run();//can't load this type of class file
                inter.runLoad();
                upModContent(config);
            } catch (Throwable e){
                errors.add(new ErrorData(config, e,e instanceof IOException || e instanceof ArcRuntimeException));
            }
        }
    }

    public void initMod(){
        //Py-MOD加载
        for (var inter : interpreters) {
            var config = inter.getConfig();
            if (getSetting(config.name)) try {
                inter.runInit();
            } catch (Throwable e){
                addError(new ErrorData(config, e, e instanceof ArcRuntimeException));
            }
            var er = isError(config);
            setEna(config, getSetting(config.name) && !er);

            if (er) {
                //modContents.removeKey(config);
                //removeMod(config);
                inter.close();
            }
        }
    }

    public void addMod(General interpreter){
        interpreters.add(interpreter);
        mods.add(interpreter.getConfig());

        //addImport(interpreter.getConfig().directory);
    }

    public void addMod(ModData data, PythonInterpreter inter){
        addMod(new ModInterpreter(data, inter));
    }


    //可以测试了，明天再说
    protected void addImport(Fi modDirectory){
        var mainPy = modDirectory.child("main.py");
        if (!mainPy.exists()) return;

        analysisFile(mainPy.readString(), mainPy, null);
//        for (var c : mainPy.readString().split("\n")) {
//            String aic = analysisImport(c);
//            if (aic == null) continue;
//
//            var importFi = getImportFi(aic, modDirectory);
//            if (importFi == null || imports.get(importFi.path()) != null) continue;
//
//            analysisFile(importFi.readString(), importFi, aic.substring(aic.lastIndexOf("/")+1));
//        }
    }

    protected void analysisFile(String code, Fi fi, String logotype){
        for (var c : code.split("\n")) {
            String aic = analysisImport(c);
            if (aic == null) continue;

            //获取的要导入的文件路径
            var importFi = getImportFi(aic, fi.parent());
            if (importFi == null || imports.get(importFi.path()) != null) continue;

            analysisFile(importFi.readString(), importFi, aic.substring(aic.lastIndexOf("/")+1));
        }
        if (logotype != null) imports.put(fi.path(), getPyVars(fi.readString(), logotype));
    }

    protected ArrayMap<String, PyObject> getPyVars(String code, String logotype){
        PythonInterpreter interpreter = new PythonInterpreter();
        //interpreter.exec("from chire.py.lib import Import");
        interpreter.exec(code);
        ArrayMap<String, PyObject> returnList = new ArrayMap<>();
        if (Objects.equals(logotype, "*")) {
            for (var l : new CRJson(
                    interpreter.getLocals().toString().replaceAll("'", "")
            ).parse()){
                if (
                    Objects.equals(l.name(), "__doc__") ||
                    Objects.equals(l.name(), "__name__")
                ) continue;
                returnList.put(l.name(), interpreter.get(l.name()));
            }
        } else if (logotype.contains(",")) {

        } else {
            returnList.put(logotype, interpreter.get(logotype));
        }
        return returnList;
    }

    protected Fi getImportFi(String importPath, Fi importDirectory){
        importPath = importPath.replaceAll("\\.", "/");
        String fp = importDirectory.path();
        String ip = importPath.substring(0, importPath.lastIndexOf("/"));
        while (true) {
            if (fp.contains(ip) && (fp.lastIndexOf(ip)==(fp.length()-ip.length()))) {
                return importDirectory.child(importPath.replace(ip+".", "")+".py");
            }
            if (ip.lastIndexOf(".") == -1){
                return null;
            }
            ip = ip.substring(0, ip.lastIndexOf("/"));
        }
    }

    protected String analysisImport(String importStr){
        Pattern fi = Pattern.compile("^(?i)\\s*from\s+(.*?)\s+import\s+(.*?)$");
        Pattern i = Pattern.compile("^(?i)\\s*import\s+(.*?)$");
        Matcher mat = fi.matcher(importStr);

        String filePath = null;
        if (mat.find()) {
            filePath = mat.group(1)+"."+mat.group(2);
        } else {
            Matcher mati = i.matcher(importStr);
            if (mati.find()) filePath = mati.group(1);
        }
        if (filePath == null) return null;

        try {
            Class.forName(filePath);
            return null;
        } catch (NullPointerException | ClassNotFoundException e) {
            return filePath;
        }
    }

    public Seq<ModData> getMods(){
        return mods;
    }

    public ModData getMod(String name){
        return mods.find(f -> Objects.equals(f.name, name));
    }

    public ModData getMod(int index){
        return mods.get(index);
    }

    public int getMod(ModData config){
        return mods.indexOf(config);
    }

//    public General getMod(ModData mod){
//        return getMod(f -> f.getConfig() == mod);
//    }
//
//    public General getMod(String name){
//        return getMod(f -> Objects.equals(f.getConfig().name, name));
//    }

    public void removeMod(ModData mod){
        interpreters.remove(f -> f.getConfig() == mod);
        mods.remove(mod);
        //if (modContents)modContents.removeKey(mod);
    }

    public void addSprite(){
        //TODO 为什么不把两个循环合并呢？
        //TODO 因为可能有天才在加载别的Py模组的Sprite和Bundle
        for (var inter : interpreters) {
            inter.addSprite();
            inter.addBundle();
        }
    }

    public void importMod(Fi file) {
        ZipFi zip = new ZipFi(file);
        if (zip.list().length != 1) throw new RuntimeException("导入模组出错，压缩包内含有非一文件夹");
        copyDirectory(zip, pyModDirectory);

        Log.info(pyModDirectory.child(zip.list()[0].name()));
        addMod(getConfig(pyModDirectory.child(zip.list()[0].name()).child("main.json")), new PythonInterpreter());

        requiresReload = true;
    }

    private void upModContent(ModData config){
        Seq<UnlockableContent> lc = Seq.with(content.getContentMap()).<Content>flatten().select(
                c -> c.minfo.mod == core && c instanceof UnlockableContent
        ).as();
        if (lastContent != null) {
            for (var l : lastContent) lc.remove(l);
        }
        lastContent = lc;
        //modContents.put(config, lc);
        mods.each(i -> i == config, f -> f.setContents(lc));
    }

    public boolean isError(ModData modData){
        return errors.contains(c -> c.config == modData);
    }

    public void showError(){
        for (var error : errors) if (ui != null) {
            PyCore.showException(
                    "["+error.config.name+"]["+ error.config.version +"]模组错误",
                    error.throwable.getMessage()
            );
        }
    }

    private void copyFile(Fi source, Fi dest){
        try{
            dest.write(source.read(), false);
        }catch(Exception ex){
            throw new RuntimeException("复制文件出现错误:"+ex);
        }
    }

    private void copyDirectory(Fi sourceDir, Fi destDir){
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

    public MultiPacker.PageType getPage(Fi file){
        String path = file.path();
        return
            path.contains("sprites/blocks/environment") || path.contains("sprites-override/blocks/environment") ? MultiPacker.PageType.environment :
            path.contains("sprites/editor") || path.contains("sprites-override/editor") ? MultiPacker.PageType.editor :
            path.contains("sprites/rubble") || path.contains("sprites-override/rubble") ? MultiPacker.PageType.rubble :
            path.contains("sprites/ui") || path.contains("sprites-override/ui") ? MultiPacker.PageType.ui :
            MultiPacker.PageType.main;
    }

    /**内部设置启用状态，不会出现requiresReload=true的情况*/
    private void setEna(ModData mod, boolean e){
        putSetting(mod.name, e);
        //mods.add(mod);
        //getMod(mod);
        if (e) {
            mods.each(f -> {
                if (f == mod) f.setEnabled(true);
            });
        } else {
            mods.remove(mod);
            mod.setEnabled(false);
            mods.add(mod);
        }
    }

    /**外部调用设置启用的方法*/
    public void setEnabled(ModData mod, boolean e){
        setEna(mod, e);
        requiresReload = true;
    }

    public boolean requiresReload(){
        return requiresReload;
    }

    public boolean getSetting(String name){
        if (!Core.settings.has(settingName+"-"+name)) return true;
        return Core.settings.getBool(settingName+"-"+name);
    }

    public void putSetting(String name, Boolean b){
        Core.settings.put(settingName+"-"+name, b);
    }

    public int modSize(){
        return interpreters.size;
    }

    public int ErrorSize(){
        return errors.size;
    }

    public void addError(ErrorData error){
        errors.add(error);
    }
}
