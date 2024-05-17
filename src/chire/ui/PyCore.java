package chire.ui;

import arc.graphics.Color;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Collapser;
import arc.util.Align;
import mindustry.ui.Styles;

public class PyCore {
    public static void showException(String text, String exc){
        new Dialog(""){{
            setFillParent(true);
            cont.margin(15);
            cont.add("@error.title").colspan(2);
            cont.row();
            cont.image().width(300f).pad(2).colspan(2).height(4f).color(Color.scarlet);
            cont.row();
            cont.add((text == null ? "" : "\n[lightgray]" + text)).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
            cont.row();

            Collapser col = new Collapser(base -> base.pane(t -> t.margin(14f).add(exc).color(Color.lightGray).left()), true);

            cont.button("@details", Styles.togglet, col::toggle).size(180f, 50f).checked(b -> !col.isCollapsed()).fillX().right();
            cont.button("@ok", this::hide).size(110, 50).fillX().left();
            cont.row();
            cont.add(col).colspan(2).pad(2);
            closeOnBack();
        }}.show();
    }
}
