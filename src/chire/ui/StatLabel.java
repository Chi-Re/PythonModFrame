package chire.ui;

import arc.flabel.FLabel;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.actions.Actions;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Time;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;

public class StatLabel extends Table {
    private float progress = 0;

    public StatLabel(String stat, Object value, float delay){
        setTransform(true);
        setClip(true);
        setBackground(Tex.whiteui);
        setColor(Pal.accent);
        margin(2f);

        FLabel statLabel = new FLabel(stat);
        statLabel.setStyle(Styles.outlineLabel);
        statLabel.setWrap(true);
        statLabel.pause();

        Label valueLabel = new Label("", Styles.outlineLabel);
        valueLabel.setAlignment(Align.right);

        add(statLabel).left().growX().padLeft(5);
        add(valueLabel).right().growX().padRight(5);

        actions(
                Actions.scaleTo(0, 1),
                Actions.delay(delay),
                Actions.parallel(
                        Actions.scaleTo(1, 1, 0.3f, Interp.pow3Out),
                        Actions.color(Pal.darkestGray, 0.3f, Interp.pow3Out),
                        Actions.sequence(
                                Actions.delay(0.3f),
                                Actions.run(() -> {
                                    valueLabel.update(() -> {
                                        if (value instanceof Integer) {
                                            progress = Math.min(1, progress + (Time.delta / 60));
                                            valueLabel.setText("" + (int) Mathf.lerp(0, (Integer) value, ((Integer) value) < 10 ? progress : Interp.slowFast.apply(progress)));
                                        } else {
                                            if (progress < value.toString().length())progress += 1;
                                            valueLabel.setText(value.toString().substring(0, (int) progress));
                                        }
                                    });
                                    statLabel.resume();
                                })
                        )
                )
        );
    }

    public static void addStat(Table parent, String stat, Object value, float delay){
        parent.add(new StatLabel(stat, value, delay)).top().pad(5).growX().height(50).width(500).row();
    }
}
