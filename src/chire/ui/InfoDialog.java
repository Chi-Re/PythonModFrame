package chire.ui;

import mindustry.ui.dialogs.BaseDialog;

public class InfoDialog extends BaseDialog {
    public InfoDialog(String title) {
        super(title);

        addCloseButton();

        shown(this::setup);
    }

    void setup(){
        cont.add("ģ������").row();
        cont.add("��ģ����Ϣ");
    }
}
