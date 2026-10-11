package net.sythix.client.config;

import java.io.IOException;

/**
 * Where the config text is kept. The desktop build uses a file; a browser build can provide
 * another implementation (for example browser storage) without touching the rest of the client.
 */
public interface ConfigStorage
{
    /** Returns the saved text, or null if nothing has been saved yet. */
    String read() throws IOException;

    void write(String text) throws IOException;
}
