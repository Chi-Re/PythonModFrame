package chire.mod;

import org.python.core.PyFunction;

import java.io.IOException;

public interface General {
    void addSprite();

    void addBundle();

    ModData getConfig();
//    PyFunction getInit();
//
//    PyFunction getLoad();

    void run() throws IOException;

    void runInit();

    void runLoad();

    void close();
}
