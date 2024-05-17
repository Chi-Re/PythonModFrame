package chire.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import chire.mod.ModData;
import chire.mod.PyMods;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.io.IOException;

import static chire.PythonJavaMod.*;
import static mindustry.Vars.*;
import static mindustry.Vars.mods;

public class PyModDialog extends BaseDialog {
    public PyModDialog(String title) {
        super(title);

        addCloseButton();

        shown(this::setup);

        hidden(() -> {
            if(requiresReload) reload();
        });
    }

    void setup(){
        float h = 110f;
        float w = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f), 520f);

        cont.clear();
        cont.defaults().width(Math.min(Core.graphics.getWidth() / Scl.scl(1.05f), 556f)).pad(4);
        //还没打算改这个
        cont.add("@mod.reloadrequired").visible(requiresReload).center().get().setAlignment(Align.center);
        cont.row();

        cont.table(buttons -> {
            buttons.left().defaults().growX().height(60f).uniformX();

            TextButton.TextButtonStyle style = Styles.flatBordert;
            float margin = 12f;

            buttons.button("@mod.import", Icon.add, style, () -> {
                BaseDialog dialog = new BaseDialog("@mod.import");

                TextButton.TextButtonStyle bstyle = Styles.flatt;

                dialog.cont.table(Tex.button, t -> {
                    t.defaults().size(300f, 70f);
                    t.margin(12f);

                    t.button("@mod.import.file", Icon.file, bstyle, () -> {
                        dialog.hide();

                        platform.showMultiFileChooser(file -> {
                            try{
                                //mods.importMod(file);
                                PyMods.importMod(file);
                                setup();
                            }catch(Exception e){
                                ui.showException(e);
                                Log.err(e);
                            }
                        }, "zip");
                    }).margin(12f);
                });
                dialog.addCloseButton();

                dialog.show();

            }).margin(margin);

        }).width(w);

        cont.row();

        cont.pane(p -> {
            p.clear();

            for (var mod : interpreters.list()){
                //if (!mod.getConfig().enabled()) continue;
                ModData item = mod.getConfig();

                p.button(t -> {
                    t.top().left();
                    t.margin(12f);

                    t.defaults().left().top();
                    t.table(title1 -> {
                        title1.left();

                        title1.add(new BorderImage(){{
                            if(item.iconTexture != null && item.iconTexture.get() != null){
                                setDrawable(item.iconTexture.get());
                            }else{
                                setDrawable(Tex.nomap);
                            }
                            border(Pal.accent);
                        }}).size(h - 8f).padTop(-8f).padLeft(-8f).padRight(8f);

                        title1.table(text -> {
                            Log.info(item);
                            text.add("[accent]" + Strings.stripColors(item.name) + "\n" +
                                    (mod.getConfig().description.length() > 0 ? "[lightgray]" + mod.getConfig().description + "\n" : "") +
                                    (item.enabled() ? "" : Core.bundle.get("mod.disabled") + "")
                            ).wrap().top().width(300f).growX().left();

                            text.row();
                        }).top().growX();

                        title1.add().growX();
                    }).growX().growY().left();

                    t.table(right -> {
                        right.right();
                        right.button(item.enabled() ? Icon.downOpen : Icon.upOpen, Styles.clearNonei, () -> {
                            PyMods.setEnabled(item, !item.enabled());
                            setup();
                        }).size(50f);

                        right.button(Icon.trash, Styles.clearNonei, () -> {
                        }).size(50f);
                    }).growX().right().padRight(-8f).padTop(-8f);
                }, Styles.flatBordert, () -> showMod(item)).size(w, h).growX().pad(4f);

                p.row();
            }
        });
    }

    private void showMod(ModData mod){
        BaseDialog dialog = new BaseDialog(mod.name);

        dialog.addCloseButton();

        dialog.cont.pane(desc -> {
            desc.center();
            desc.defaults().padTop(10).left();

            desc.add("@editor.name").padRight(10).color(Color.gray).padTop(0);
            desc.row();
            desc.add(mod.name).growX().wrap().padTop(2);
            desc.row();
            if(mod.author != null){
                desc.add("@editor.author").padRight(10).color(Color.gray);
                desc.row();
                desc.add(mod.author).growX().wrap().padTop(2);
                desc.row();
            }
            if(mod.description != null){
                desc.add("@editor.description").padRight(10).color(Color.gray).top();
                desc.row();
                desc.add(mod.description).growX().wrap().padTop(2);
                desc.row();
            }
        }).width(400f);

        if(mod.any()){
            dialog.cont.row();
            dialog.cont.button("@mods.viewcontent", Icon.book, () -> {
                BaseDialog d = new BaseDialog(mod.name);
                d.cont.pane(cs -> {
                    int i = 0;
                    for(UnlockableContent c : mod.getContent()){
                        cs.button(new TextureRegionDrawable(c.uiIcon), Styles.flati, iconMed, () -> {
                            ui.content.show(c);
                        }).size(50f).with(im -> {
                            var click = im.getClickListener();
                            im.update(() -> im.getImage().color.lerp(!click.isOver() ? Color.lightGray : Color.white, 0.4f * Time.delta));

                        }).tooltip(c.localizedName);

                        if(++i % (int)Math.min(Core.graphics.getWidth() / Scl.scl(110), 14) == 0) cs.row();
                    }
                }).grow();
                d.addCloseButton();
                d.show();
            }).size(300, 50).pad(4);
        }

        dialog.show();
    }

    private void reload(){
        ui.showInfoOnHidden("@mods.reloadexit", () -> {
            Log.info("退出以重新加载模组.");
            Core.app.exit();
        });
    }
}
