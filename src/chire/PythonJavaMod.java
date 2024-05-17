package chire;

import arc.Core;
import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ArrayMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import arc.util.Nullable;
import chire.mod.General;
import chire.mod.ModData;
import chire.mod.PluginInterpreter;
import chire.mod.PyMods;
import chire.mod.error.ErrorData;
import chire.ui.InfoDialog;
import chire.ui.PyCore;
import chire.ui.PyModDialog;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SettingsMenuDialog;
import org.jetbrains.annotations.NotNull;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.util.Properties;

import static chire.ui.StatLabel.addStat;
import static mindustry.Vars.*;

public class PythonJavaMod extends Mod{
    /**所有的解释器，为了统一调用*/
    public static Seq<General> interpreters = new Seq<>();

    public static ArrayMap<String, ModData> mods = new ArrayMap<>();

    public static Mods.LoadedMod core;

    public static PyModDialog modDialog;

    public static InfoDialog infoDialog;
    /**是否存在报错*/
    public static boolean hasError = false;
    /**所有的报错*/
    public static Seq<ErrorData> errors = new Seq<>();
    /**所有模组的启用的情况*/
    public static ArrayMap<String, Boolean> enabled = new ArrayMap<>();
    /**所有模组的内容*/
    public static ArrayMap<ModData, Seq<UnlockableContent>> modContents = new ArrayMap<>();

    public static boolean requiresReload = false;
    /**缓存用的，用来计算modContents的数据*/
    public Seq<UnlockableContent> lastContent = null;

    public static String settingName = "py-enabled-mods";

    public static Fi pyModDirectory;

    public PythonJavaMod(){
        Log.info("ppp");

        Properties props = new Properties();
        props.setProperty("python.console.encoding", "UTF-8");
        props.setProperty("python.import.site","false");
        props.setProperty("python.cachedir.skip", "true");
        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(preprops, props, new String[0]);

        interpreters.add(new PluginInterpreter(new ModData(
                modDirectory.child("py_plugin"),
                "python-mod-plugin",
                "py-plugin",
                "炽热S",
                "1.1.4",
                "模组的插件~",
                null
        )));

        pyModDirectory = modDirectory.child("py_mods");
        PyMods.load();
    }

    @Override
    public void init() {
        //Py-MOD加载
        for (var inter : interpreters) {
            var config = inter.getConfig();
            if (getSetting(config.name)) try {
                inter.runInit();
            } catch (Throwable e){
                errors.add(new ErrorData(config, e, e instanceof ArcRuntimeException));
            }

            var er = isError(config);
            enabled.put(config.name, getSetting(config.name) && !er);
            Log.info(getSetting(config.name));
            if (er) {
                modContents.removeKey(config);
                inter.close();
            }
        }
        Log.info(enabled);

        for (var error : errors) if (ui != null) {
            PyCore.showException(
                    "["+error.config.name+"]["+ error.config.version +"]模组错误",
                    error.throwable.getMessage()
            );
        }

        //UI的
        modDialog = new PyModDialog(getBundle("python.mod"));
        infoDialog = new InfoDialog("模组详情");

        if(ui != null && ui.settings != null) {
            ui.settings.addCategory(getBundle("python.mod"), Icon.book, settingsTable -> settingsTable.pref(new SettingsMenuDialog.SettingsTable.Setting(getBundle("python.mod")) {
                @Override
                public void add(SettingsMenuDialog.SettingsTable table) {
                    table.table(t -> {
                        t.add("模组详情页").center().pad(6);
                        t.row();
                        t.pane(p -> {
                            p.margin(13f);
                            p.left().defaults().left();
                            p.setBackground(Styles.black3);

                            p.table(stats -> {
                                addStat(stats, "已导入模组数", interpreters.size, 0.05f);
                                addStat(stats, "出现错误", errors.size, 0.1f);
                            }).top().grow().row();
                            p.button("@mods", Icon.book, modDialog::show).top().grow().row();
                            p.button("模组详情", Icon.book, infoDialog::show).top().grow().row();
                        }).grow().pad(12).top().row();
                    }).center().minWidth(370).maxSize(600, 550).grow().row();
                }
            }));
        }
    }

    @Override
    public void loadContent(){
        core = Vars.mods.getMod("python-mod");
        //TODO 为什么不把两个循环合并呢？
        //TODO 因为可能有天才在加载别的Py模组的Sprite和Bundle
        for (var inter : interpreters) {
            inter.addSprite();
            inter.addBundle();
        }
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

    public static boolean getSetting(String name){
        if (!Core.settings.has(settingName+"-"+name)) return true;
        return Core.settings.getBool(settingName+"-"+name);
    }

    public static void putSetting(String name, Boolean b){
        Core.settings.put(settingName+"-"+name, b);
    }

    public static boolean isError(ModData modData){
        return errors.contains(c -> c.config == modData);
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

    public static @Nullable PyFunction getFun(String name, PythonInterpreter interpreter){
        try {
            return (PyFunction) interpreter.get(name, PyFunction.class);
        } catch (NullPointerException n){
            Log.warn(n.toString());
            return null;
        }
    }

    public static @Nullable PyObject getVar(String name, PythonInterpreter interpreter){
        try {
            return (PyObject) interpreter.get(name, PyObject.class);
        } catch (NullPointerException n){
            Log.warn(n.toString());
            return null;
        }
    }

    public static String getBundle(String str){
        return Core.bundle.format(str);
    }

    public static @NotNull String getName(String add){
        return getBundle("mod.name") + "-" + add;
    }

    public static TextureRegion getTexture(String name){
        return Core.atlas.find(name);
    }
}
