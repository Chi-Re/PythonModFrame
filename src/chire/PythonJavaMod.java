package chire;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ArrayMap;
import arc.util.Log;
import arc.util.Nullable;
import chire.mod.PyModImport;
import chire.mod.PyMods;
import chire.ui.InfoDialog;
import chire.ui.PyModDialog;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.SettingsMenuDialog;
import org.jetbrains.annotations.NotNull;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.util.Properties;

import static chire.ui.StatLabel.addStat;
import static mindustry.Vars.ui;

public class PythonJavaMod extends Mod{
    public static Mods.LoadedMod core;

    public static PyModDialog modDialog;

    public static InfoDialog infoDialog;

    public static PyMods pyMods = new PyMods();

    public PythonJavaMod(){
        Log.info("ppp");

        Properties props = new Properties();
        props.setProperty("python.console.encoding", "UTF-8");
        props.setProperty("python.import.site","false");
        props.setProperty("python.cachedir.skip", "true");
        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(preprops, props, new String[0]);
    }

    @Override
    public void init() {
        pyMods.initMod();

        pyMods.showError();

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
                                addStat(stats, "已导入模组数", pyMods.modSize(), 0.05f);
                                addStat(stats, "出现错误", pyMods.ErrorSize(), 0.1f);
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

        pyMods.addSprite();
        pyMods.loadMod();
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

//    public static void cleanup(){
//        importInter = new PythonInterpreter();
//    }
//
//    public static void delete(){
//        importInter = null;
//    }
}
