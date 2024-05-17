package chire.mod;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.func.Boolf;
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
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.util.Objects;

import static chire.PythonJavaMod.core;
import static chire.PythonJavaMod.pyMods;
import static mindustry.Vars.*;

public class PyMods {
    /**���еĽ�������Ϊ��ͳһ����*/
    private Seq<General> interpreters = new Seq<>();

    //public ArrayMap<String, ModData> mods = new ArrayMap<>();

    /**�Ƿ���ڱ���*/
    public static boolean hasError = false;
    /**���еı���*/
    private final Seq<ErrorData> errors = new Seq<>();
    /**����ģ������õ����*/
    private final ArrayMap<String, Boolean> enabled = new ArrayMap<>();
    /**����ģ�������*/
    private final ArrayMap<ModData, Seq<UnlockableContent>> modContents = new ArrayMap<>();

    private boolean requiresReload = false;
    /**�����õģ���������modContents������*/
    private Seq<UnlockableContent> lastContent = null;

    private final String settingName = "py-enabled-mods";

    private final Fi pyModDirectory;

    public PyMods(){
        pyModDirectory = modDirectory.child("py_mods");

        addMod(new PluginInterpreter(new ModData(
                modDirectory.child("py_plugin"),
                "python-mod-plugin",
                "py-plugin",
                "����S",
                "1.1.4",
                "ģ��Ĳ��~",
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
        if (!main.exists() || main.isDirectory()) throw new RuntimeException("��ȡģ��json�����ļ�����");
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
                inter.run();
                inter.runLoad();
                upModContent(config);
            } catch (Throwable e){
                errors.add(new ErrorData(config, e,e instanceof IOException || e instanceof ArcRuntimeException));
            }
        }
    }

    public void initMod(){
        //Py-MOD����
        for (var inter : interpreters) {
            var config = inter.getConfig();
            if (getSetting(config.name)) try {
                inter.runInit();
            } catch (Throwable e){
                pyMods.addError(new ErrorData(config, e, e instanceof ArcRuntimeException));
            }

            var er = pyMods.isError(config);
            pyMods.addEnabled(config.name, getSetting(config.name) && !er);

            if (er) {
                modContents.removeKey(config);
                inter.close();
            }
        }
    }

    public void addMod(General interpreter){
        interpreters.add(interpreter);
    }

    public void addMod(ModData data, PythonInterpreter inter){
        interpreters.add(new ModInterpreter(data, inter));
    }

    public Seq<General> getMods(){
        return interpreters;
    }

    public General getMod(Boolf<General> predicate){
        return interpreters.find(predicate);
    }

    public General getMod(ModData mod){
        return getMod(f -> f.getConfig() == mod);
    }

    public General getMod(String name){
        return getMod(f -> Objects.equals(f.getConfig().name, name));
    }

    public void removeMod(ModData mod){
        interpreters.remove(f -> f.getConfig() == mod);
        //if (modContents)modContents.removeKey(mod);
    }

    public void addSprite(){
        //TODO Ϊʲô��������ѭ���ϲ��أ�
        //TODO ��Ϊ����������ڼ��ر��Pyģ���Sprite��Bundle
        for (var inter : interpreters) {
            inter.addSprite();
            inter.addBundle();
        }
    }

    public void importMod(Fi file) {
        ZipFi zip = new ZipFi(file);
        if (zip.list().length != 1) throw new RuntimeException("����ģ�����ѹ�����ں��з�һ�ļ���");
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
        modContents.put(config, lc);
    }

    public boolean isError(ModData modData){
        return errors.contains(c -> c.config == modData);
    }

    public void showError(){
        for (var error : errors) if (ui != null) {
            PyCore.showException(
                    "["+error.config.name+"]["+ error.config.version +"]ģ�����",
                    error.throwable.getMessage()
            );
        }
    }

    private void copyFile(Fi source, Fi dest){
        try{
            dest.write(source.read(), false);
        }catch(Exception ex){
            throw new RuntimeException("�����ļ����ִ���:"+ex);
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

    public void setEnabled(ModData mod, boolean e){
        putSetting(mod.name, e);
        enabled.put(mod.name, e);
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

    public int EnabledSize(){
        return enabled.size;
    }

    public ArrayMap<String, Boolean> enabled(){
        return enabled;
    }

    public void addEnabled(String name, Boolean e){
        enabled.put(name, e);
    }

    public ArrayMap<ModData, Seq<UnlockableContent>> contents(){
        return modContents;
    }
}
